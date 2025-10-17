package it.robfrank.linklift;

import com.arcadedb.remote.RemoteDatabase;
import io.javalin.Javalin;
import it.robfrank.linklift.adapter.in.web.AuthenticationController;
import it.robfrank.linklift.adapter.in.web.GetContentController;
import it.robfrank.linklift.adapter.in.web.ListLinksController;
import it.robfrank.linklift.adapter.in.web.NewLinkController;
import it.robfrank.linklift.adapter.out.event.SimpleEventPublisher;
import it.robfrank.linklift.adapter.out.http.HttpContentDownloader;
import it.robfrank.linklift.adapter.out.persitence.*;
import it.robfrank.linklift.adapter.out.security.BCryptPasswordSecurityAdapter;
import it.robfrank.linklift.adapter.out.security.JwtTokenAdapter;
import it.robfrank.linklift.application.domain.event.*;
import it.robfrank.linklift.application.domain.service.*;
import it.robfrank.linklift.application.port.in.DownloadContentUseCase;
import it.robfrank.linklift.application.port.in.GetContentUseCase;
import it.robfrank.linklift.application.port.in.ListLinksUseCase;
import it.robfrank.linklift.application.port.in.NewLinkUseCase;
import it.robfrank.linklift.config.DatabaseInitializer;
import it.robfrank.linklift.config.SecureConfiguration;
import it.robfrank.linklift.config.WebBuilder;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    // JWT secret loaded from secure configuration (environment variables)
    private static final String JWT_SECRET = SecureConfiguration.getJwtSecret();

    public static void main(String[] args) {
        String arcadedbServer = System.getProperty("linklift.arcadedb.host", "localhost");

        logger.info("Starting LinkLift application with ArcadeDB server: {}", arcadedbServer);
        logger.atInfo().addArgument(() -> SecureConfiguration.getConfigurationHints()).log("Security configuration: {}");

        new DatabaseInitializer(arcadedbServer, 2480, "root", "playwithdata").initializeDatabase();

        RemoteDatabase database = new RemoteDatabase(arcadedbServer, 2480, "linklift", "root", "playwithdata");

        // Initialize repositories and mappers
        LinkMapper linkMapper = new LinkMapper();
        ArcadeLinkRepository linkRepository = new ArcadeLinkRepository(database, linkMapper);
        LinkPersistenceAdapter linkPersistenceAdapter = new LinkPersistenceAdapter(linkRepository);

        ArcadeContentRepository contentRepository = new ArcadeContentRepository(database);
        ContentPersistenceAdapter contentPersistenceAdapter = new ContentPersistenceAdapter(contentRepository);

        UserMapper userMapper = new UserMapper();
        ArcadeUserRepository userRepository = new ArcadeUserRepository(database, userMapper);
        UserPersistenceAdapter userPersistenceAdapter = new UserPersistenceAdapter(userRepository);

        AuthTokenMapper authTokenMapper = new AuthTokenMapper();
        ArcadeAuthTokenRepository authTokenRepository = new ArcadeAuthTokenRepository(database, authTokenMapper);
        AuthTokenPersistenceAdapter authTokenPersistenceAdapter = new AuthTokenPersistenceAdapter(authTokenRepository);

        // Initialize user roles adapter
        UserRolePersistenceAdapter userRolePersistenceAdapter = new UserRolePersistenceAdapter();

        // Initialize security adapters
        BCryptPasswordSecurityAdapter passwordSecurityAdapter = new BCryptPasswordSecurityAdapter();
        JwtTokenAdapter jwtTokenAdapter = new JwtTokenAdapter(JWT_SECRET);

        // Initialize HTTP client for content download
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        HttpContentDownloader contentDownloader = new HttpContentDownloader(httpClient);

        // Create and configure event publisher
        SimpleEventPublisher eventPublisher = new SimpleEventPublisher();
        configureEventSubscribers(eventPublisher);

        // Initialize services
        CreateUserService userService = new CreateUserService(userPersistenceAdapter, userPersistenceAdapter, passwordSecurityAdapter, eventPublisher);

        AuthenticationService authenticationService = new AuthenticationService(
            userPersistenceAdapter,
            userPersistenceAdapter,
            passwordSecurityAdapter,
            jwtTokenAdapter,
            authTokenPersistenceAdapter,
            eventPublisher
        );

        AuthorizationService authorizationService = new AuthorizationService(jwtTokenAdapter, userPersistenceAdapter, userRolePersistenceAdapter);

        DownloadContentUseCase downloadContentUseCase = new DownloadContentService(contentDownloader, contentPersistenceAdapter, eventPublisher);
        GetContentUseCase getContentUseCase = new GetContentService(contentPersistenceAdapter);

        NewLinkUseCase newLinkUseCase = new NewLinkService(linkPersistenceAdapter, eventPublisher, downloadContentUseCase);
        ListLinksUseCase listLinksUseCase = new ListLinksService(linkPersistenceAdapter, eventPublisher);

        // Initialize controllers
        NewLinkController newLinkController = new NewLinkController(newLinkUseCase);
        ListLinksController listLinksController = new ListLinksController(listLinksUseCase);
        GetContentController getContentController = new GetContentController(getContentUseCase);
        AuthenticationController authenticationController = new AuthenticationController(userService, authenticationService, authenticationService);

        // Build and start web application
        Javalin app = new WebBuilder()
            .withAuthorizationService(authorizationService)
            .withAuthenticationController(authenticationController)
            .withLinkController(newLinkController)
            .withListLinksController(listLinksController)
            .withGetContentController(getContentController)
            .build();

        app.start(7070);
    }

    private static void configureEventSubscribers(SimpleEventPublisher eventPublisher) {
        // Configure event subscribers - this is where different components can subscribe to events
        eventPublisher.subscribe(LinkCreatedEvent.class, event -> {
            logger.atInfo().addArgument(() -> event.getLink().url()).addArgument(event.getUserId()).addArgument(event.getTimestamp()).log("Link created: {} for user: {} at {}");
        });

        eventPublisher.subscribe(LinksQueryEvent.class, event -> {
            logger.atInfo().addArgument(() -> event.getQuery().page()).addArgument(() -> event.getQuery().size()).addArgument(event.getResultCount()).addArgument(() -> event.getQuery().userId()).addArgument(event.getTimestamp()).log("Links queried: page={}, size={}, results={} for user: {} at {}");
        });

        // User management events
        eventPublisher.subscribe(CreateUserService.UserCreatedEvent.class, event -> {
            logger.atInfo().addArgument(() -> event.username()).addArgument(() -> event.email()).addArgument(() -> LocalDateTime.now()).log("User created: {} ({}) at {}");
        });

        eventPublisher.subscribe(AuthenticationService.UserAuthenticatedEvent.class, event -> {
            logger.atInfo().addArgument(() -> event.username()).addArgument(() -> event.ipAddress()).addArgument(() -> event.timestamp()).log("User authenticated: {} from {} at {}");
        });

        eventPublisher.subscribe(AuthenticationService.TokenRefreshedEvent.class, event -> {
            logger.atInfo().addArgument(() -> event.username()).addArgument(() -> event.ipAddress()).addArgument(() -> event.timestamp()).log("Token refreshed for user: {} from {} at {}");
        });

        // Content download events
        eventPublisher.subscribe(ContentDownloadStartedEvent.class, event -> {
            logger.info("Content download started for link: {} at {}", event.getLinkId(), event.getTimestamp());
        });

        eventPublisher.subscribe(ContentDownloadCompletedEvent.class, event -> {
            logger.atInfo().addArgument(() -> event.getContent().linkId()).addArgument(event.getTimestamp()).log("Content download completed for link: {} at {}");
        });

        eventPublisher.subscribe(ContentDownloadFailedEvent.class, event -> {
            logger.error("Content download failed for link: {} - {} at {}", event.getLinkId(), event.getErrorMessage(), event.getTimestamp());
        });
    }

    // Method to start the application - useful for testing
    public Javalin start(int port) {
        String arcadedbServer = System.getProperty("linklift.arcadedb.host", "localhost");
        new DatabaseInitializer(arcadedbServer, 2480, "root", "playwithdata").initializeDatabase();

        RemoteDatabase database = new RemoteDatabase(arcadedbServer, 2480, "linklift", "root", "playwithdata");

        // Initialize repositories and mappers
        LinkMapper linkMapper = new LinkMapper();
        ArcadeLinkRepository linkRepository = new ArcadeLinkRepository(database, linkMapper);
        LinkPersistenceAdapter linkPersistenceAdapter = new LinkPersistenceAdapter(linkRepository);

        ArcadeContentRepository contentRepository = new ArcadeContentRepository(database);
        ContentPersistenceAdapter contentPersistenceAdapter = new ContentPersistenceAdapter(contentRepository);

        UserMapper userMapper = new UserMapper();
        ArcadeUserRepository userRepository = new ArcadeUserRepository(database, userMapper);
        UserPersistenceAdapter userPersistenceAdapter = new UserPersistenceAdapter(userRepository);

        AuthTokenMapper authTokenMapper = new AuthTokenMapper();
        ArcadeAuthTokenRepository authTokenRepository = new ArcadeAuthTokenRepository(database, authTokenMapper);
        AuthTokenPersistenceAdapter authTokenPersistenceAdapter = new AuthTokenPersistenceAdapter(authTokenRepository);

        // Initialize user roles adapter
        UserRolePersistenceAdapter userRolePersistenceAdapter = new UserRolePersistenceAdapter();

        // Initialize security adapters
        BCryptPasswordSecurityAdapter passwordSecurityAdapter = new BCryptPasswordSecurityAdapter();
        JwtTokenAdapter jwtTokenAdapter = new JwtTokenAdapter(JWT_SECRET);

        // Initialize HTTP client for content download
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        HttpContentDownloader contentDownloader = new HttpContentDownloader(httpClient);

        SimpleEventPublisher eventPublisher = new SimpleEventPublisher();
        configureEventSubscribers(eventPublisher);

        // Initialize services
        CreateUserService userService = new CreateUserService(userPersistenceAdapter, userPersistenceAdapter, passwordSecurityAdapter, eventPublisher);

        AuthenticationService authenticationService = new AuthenticationService(
            userPersistenceAdapter,
            userPersistenceAdapter,
            passwordSecurityAdapter,
            jwtTokenAdapter,
            authTokenPersistenceAdapter,
            eventPublisher
        );

        AuthorizationService authorizationService = new AuthorizationService(jwtTokenAdapter, userPersistenceAdapter, userRolePersistenceAdapter);

        DownloadContentUseCase downloadContentUseCase = new DownloadContentService(contentDownloader, contentPersistenceAdapter, eventPublisher);
        GetContentUseCase getContentUseCase = new GetContentService(contentPersistenceAdapter);

        NewLinkUseCase newLinkUseCase = new NewLinkService(linkPersistenceAdapter, eventPublisher, downloadContentUseCase);
        ListLinksUseCase listLinksUseCase = new ListLinksService(linkPersistenceAdapter, eventPublisher);

        // Initialize controllers
        NewLinkController newLinkController = new NewLinkController(newLinkUseCase);
        ListLinksController listLinksController = new ListLinksController(listLinksUseCase);
        GetContentController getContentController = new GetContentController(getContentUseCase);
        AuthenticationController authenticationController = new AuthenticationController(userService, authenticationService, authenticationService);

        // Build and start web application
        Javalin app = new WebBuilder()
            .withAuthorizationService(authorizationService)
            .withAuthenticationController(authenticationController)
            .withLinkController(newLinkController)
            .withListLinksController(listLinksController)
            .withGetContentController(getContentController)
            .build();

        app.start(port);
        return app;
    }
}
