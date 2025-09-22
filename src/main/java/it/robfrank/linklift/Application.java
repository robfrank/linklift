package it.robfrank.linklift;

import com.arcadedb.remote.RemoteDatabase;
import io.javalin.Javalin;
import it.robfrank.linklift.adapter.in.web.AuthenticationController;
import it.robfrank.linklift.adapter.in.web.ListLinksController;
import it.robfrank.linklift.adapter.in.web.NewLinkController;
import it.robfrank.linklift.adapter.out.event.SimpleEventPublisher;
import it.robfrank.linklift.adapter.out.persitence.ArcadeAuthTokenRepository;
import it.robfrank.linklift.adapter.out.persitence.ArcadeLinkRepository;
import it.robfrank.linklift.adapter.out.persitence.ArcadeUserRepository;
import it.robfrank.linklift.adapter.out.persitence.AuthTokenMapper;
import it.robfrank.linklift.adapter.out.persitence.AuthTokenPersistenceAdapter;
import it.robfrank.linklift.adapter.out.persitence.LinkMapper;
import it.robfrank.linklift.adapter.out.persitence.LinkPersistenceAdapter;
import it.robfrank.linklift.adapter.out.persitence.UserMapper;
import it.robfrank.linklift.adapter.out.persitence.UserPersistenceAdapter;
import it.robfrank.linklift.adapter.out.persitence.UserRolePersistenceAdapter;
import it.robfrank.linklift.adapter.out.security.BCryptPasswordSecurityAdapter;
import it.robfrank.linklift.adapter.out.security.JwtTokenAdapter;
import it.robfrank.linklift.application.domain.event.LinkCreatedEvent;
import it.robfrank.linklift.application.domain.event.LinksQueryEvent;
import it.robfrank.linklift.application.domain.service.AuthenticationService;
import it.robfrank.linklift.application.domain.service.AuthorizationService;
import it.robfrank.linklift.application.domain.service.CreateUserService;
import it.robfrank.linklift.application.domain.service.ListLinksService;
import it.robfrank.linklift.application.domain.service.NewLinkService;
import it.robfrank.linklift.application.port.in.AuthenticateUserUseCase;
import it.robfrank.linklift.application.port.in.CreateUserUseCase;
import it.robfrank.linklift.application.port.in.ListLinksUseCase;
import it.robfrank.linklift.application.port.in.NewLinkUseCase;
import it.robfrank.linklift.application.port.in.RefreshTokenUseCase;
import it.robfrank.linklift.config.DatabaseInitializer;
import it.robfrank.linklift.config.SecureConfiguration;
import it.robfrank.linklift.config.WebBuilder;
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
        logger.info("Security configuration: {}", SecureConfiguration.getConfigurationHints());

        new DatabaseInitializer(arcadedbServer, 2480, "root", "playwithdata").initializeDatabase();

        RemoteDatabase database = new RemoteDatabase(arcadedbServer, 2480, "linklift", "root", "playwithdata");

        // Initialize repositories and mappers
        LinkMapper linkMapper = new LinkMapper();
        ArcadeLinkRepository linkRepository = new ArcadeLinkRepository(database, linkMapper);
        LinkPersistenceAdapter linkPersistenceAdapter = new LinkPersistenceAdapter(linkRepository);

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

        NewLinkUseCase newLinkUseCase = new NewLinkService(linkPersistenceAdapter, eventPublisher);
        ListLinksUseCase listLinksUseCase = new ListLinksService(linkPersistenceAdapter, eventPublisher);

        // Initialize controllers
        NewLinkController newLinkController = new NewLinkController(newLinkUseCase);
        ListLinksController listLinksController = new ListLinksController(listLinksUseCase);
        AuthenticationController authenticationController = new AuthenticationController(userService, authenticationService, authenticationService);

        // Build and start web application
        Javalin app = new WebBuilder()
            .withAuthorizationService(authorizationService)
            .withAuthenticationController(authenticationController)
            .withLinkController(newLinkController)
            .withListLinksController(listLinksController)
            .build();

        app.start(7070);
    }

    private static void configureEventSubscribers(SimpleEventPublisher eventPublisher) {
        // Configure event subscribers - this is where different components can subscribe to events
        eventPublisher.subscribe(LinkCreatedEvent.class, event -> {
            logger.info("Link created: {} for user: {} at {}", event.getLink().url(), event.getUserId(), event.getTimestamp());
        });

        eventPublisher.subscribe(LinksQueryEvent.class, event -> {
            logger.info(
                "Links queried: page={}, size={}, results={} for user: {} at {}",
                event.getQuery().page(),
                event.getQuery().size(),
                event.getResultCount(),
                event.getQuery().userId(),
                event.getTimestamp()
            );
        });

        // User management events
        eventPublisher.subscribe(CreateUserService.UserCreatedEvent.class, event -> {
            logger.info("User created: {} ({}) at {}", event.username(), event.email(), LocalDateTime.now());
        });

        eventPublisher.subscribe(AuthenticationService.UserAuthenticatedEvent.class, event -> {
            logger.info("User authenticated: {} from {} at {}", event.username(), event.ipAddress(), event.timestamp());
        });

        eventPublisher.subscribe(AuthenticationService.TokenRefreshedEvent.class, event -> {
            logger.info("Token refreshed for user: {} from {} at {}", event.username(), event.ipAddress(), event.timestamp());
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

        NewLinkUseCase newLinkUseCase = new NewLinkService(linkPersistenceAdapter, eventPublisher);
        ListLinksUseCase listLinksUseCase = new ListLinksService(linkPersistenceAdapter, eventPublisher);

        // Initialize controllers
        NewLinkController newLinkController = new NewLinkController(newLinkUseCase);
        ListLinksController listLinksController = new ListLinksController(listLinksUseCase);
        AuthenticationController authenticationController = new AuthenticationController(userService, authenticationService, authenticationService);

        // Build and start web application
        Javalin app = new WebBuilder()
            .withAuthorizationService(authorizationService)
            .withAuthenticationController(authenticationController)
            .withLinkController(newLinkController)
            .withListLinksController(listLinksController)
            .build();

        app.start(port);
        return app;
    }
}
