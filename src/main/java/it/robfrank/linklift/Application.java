package it.robfrank.linklift;

import com.arcadedb.remote.RemoteDatabase;
import io.javalin.Javalin;
import it.robfrank.linklift.adapter.in.web.*;
import it.robfrank.linklift.adapter.out.content.SimpleTextSummarizer;
import it.robfrank.linklift.adapter.out.event.SimpleEventPublisher;
import it.robfrank.linklift.adapter.out.http.HttpContentDownloader;
import it.robfrank.linklift.adapter.out.http.JsoupContentExtractor;
import it.robfrank.linklift.adapter.out.persistence.*;
import it.robfrank.linklift.adapter.out.persistence.ArcadeCollectionRepository;
import it.robfrank.linklift.adapter.out.persistence.CollectionPersistenceAdapter;
import it.robfrank.linklift.adapter.out.security.BCryptPasswordSecurityAdapter;
import it.robfrank.linklift.adapter.out.security.JwtTokenAdapter;
import it.robfrank.linklift.application.domain.event.*;
import it.robfrank.linklift.application.domain.service.*;
import it.robfrank.linklift.application.domain.service.CreateCollectionService;
import it.robfrank.linklift.application.domain.service.GetRelatedLinksService;
import it.robfrank.linklift.application.port.in.*;
import it.robfrank.linklift.application.port.in.CreateCollectionUseCase;
import it.robfrank.linklift.application.port.in.GetRelatedLinksUseCase;
import it.robfrank.linklift.application.port.out.ContentDownloaderPort;
import it.robfrank.linklift.config.DatabaseInitializer;
import it.robfrank.linklift.config.SecureConfiguration;
import it.robfrank.linklift.config.WebBuilder;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    ExecutorService contentExtractionExecutor = Executors.newFixedThreadPool(1); // Single thread for content extraction

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

    // Initialize content extractors
    JsoupContentExtractor contentExtractor = new JsoupContentExtractor();
    SimpleTextSummarizer contentSummarizer = new SimpleTextSummarizer();

    // Create and configure event publisher
    SimpleEventPublisher eventPublisher = new SimpleEventPublisher();
    ContentDownloaderPort linkContentExtractorService = new HttpContentDownloader(httpClient);

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

    DownloadContentUseCase downloadContentUseCase = new DownloadContentService(
      contentDownloader,
      contentPersistenceAdapter,
      eventPublisher,
      contentExtractor,
      contentSummarizer
    );
    configureEventSubscribers(eventPublisher, downloadContentUseCase);
    GetContentUseCase getContentUseCase = new GetContentService(contentPersistenceAdapter);

    NewLinkUseCase newLinkUseCase = new NewLinkService(linkPersistenceAdapter, eventPublisher);
    ListLinksUseCase listLinksUseCase = new ListLinksService(linkPersistenceAdapter, eventPublisher);

    // Initialize Collection and Related Links components
    ArcadeCollectionRepository collectionRepository = new ArcadeCollectionRepository(database);
    CollectionPersistenceAdapter collectionPersistenceAdapter = new CollectionPersistenceAdapter(collectionRepository);
    CreateCollectionUseCase createCollectionUseCase = new CreateCollectionService(collectionPersistenceAdapter);
    ListCollectionsUseCase listCollectionsUseCase = new ListCollectionsService(collectionPersistenceAdapter);
    GetCollectionUseCase getCollectionUseCase = new GetCollectionService(collectionPersistenceAdapter);
    AddLinkToCollectionUseCase addLinkToCollectionUseCase = new AddLinkToCollectionService(collectionPersistenceAdapter);
    RemoveLinkFromCollectionUseCase removeLinkFromCollectionUseCase = new RemoveLinkFromCollectionService(collectionPersistenceAdapter);
    DeleteCollectionUseCase deleteCollectionUseCase = new DeleteCollectionService(collectionPersistenceAdapter);
    GetRelatedLinksUseCase getRelatedLinksUseCase = new GetRelatedLinksService(linkPersistenceAdapter);

    CollectionController collectionController = new CollectionController(
      createCollectionUseCase,
      listCollectionsUseCase,
      getCollectionUseCase,
      addLinkToCollectionUseCase,
      removeLinkFromCollectionUseCase,
      deleteCollectionUseCase
    );
    GetRelatedLinksController getRelatedLinksController = new GetRelatedLinksController(getRelatedLinksUseCase);

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
      .withCollectionController(collectionController)
      .withGetRelatedLinksController(getRelatedLinksController)
      .build();

    app.start(7070);
  }

  private static void configureEventSubscribers(SimpleEventPublisher eventPublisher, DownloadContentUseCase linkContentExtractorService) {
    // Configure event subscribers - this is where different components can
    // subscribe to events
    eventPublisher.subscribe(LinkCreatedEvent.class, event -> {
      logger
        .atInfo()
        .addArgument(() -> event.getLink().url())
        .addArgument(event.getUserId())
        .addArgument(event.getTimestamp())
        .log("Link created: {} for user: {} at {}");
      linkContentExtractorService.downloadContentAsync(new DownloadContentCommand(event.getLink().id(), event.getLink().url()));
    });

    eventPublisher.subscribe(LinksQueryEvent.class, event -> {
      logger
        .atInfo()
        .addArgument(() -> event.getQuery().page())
        .addArgument(() -> event.getQuery().size())
        .addArgument(event.getResultCount())
        .addArgument(() -> event.getQuery().userId())
        .addArgument(event.getTimestamp())
        .log("Links queried: page={}, size={}, results={} for user: {} at {}");
    });

    // User management events
    eventPublisher.subscribe(CreateUserService.UserCreatedEvent.class, event -> {
      logger
        .atInfo()
        .addArgument(() -> event.username())
        .addArgument(() -> event.email())
        .addArgument(() -> LocalDateTime.now())
        .log("User created: {} ({}) at {}");
    });

    eventPublisher.subscribe(AuthenticationService.UserAuthenticatedEvent.class, event -> {
      logger
        .atInfo()
        .addArgument(() -> event.username())
        .addArgument(() -> event.ipAddress())
        .addArgument(() -> event.timestamp())
        .log("User authenticated: {} from {} at {}");
    });

    eventPublisher.subscribe(AuthenticationService.TokenRefreshedEvent.class, event -> {
      logger
        .atInfo()
        .addArgument(() -> event.username())
        .addArgument(() -> event.ipAddress())
        .addArgument(() -> event.timestamp())
        .log("Token refreshed for user: {} from {} at {}");
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

    ExecutorService contentExtractionExecutor = Executors.newFixedThreadPool(1); // Single thread for content extraction

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

    // Initialize content extractors
    JsoupContentExtractor contentExtractor = new JsoupContentExtractor();
    SimpleTextSummarizer contentSummarizer = new SimpleTextSummarizer();

    SimpleEventPublisher eventPublisher = new SimpleEventPublisher();

    ContentDownloaderPort linkContentExtractorService = new HttpContentDownloader(httpClient);

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

    DownloadContentUseCase downloadContentUseCase = new DownloadContentService(
      contentDownloader,
      contentPersistenceAdapter,
      eventPublisher,
      contentExtractor,
      contentSummarizer
    );

    configureEventSubscribers(eventPublisher, downloadContentUseCase);

    GetContentUseCase getContentUseCase = new GetContentService(contentPersistenceAdapter);

    NewLinkUseCase newLinkUseCase = new NewLinkService(linkPersistenceAdapter, eventPublisher);
    ListLinksUseCase listLinksUseCase = new ListLinksService(linkPersistenceAdapter, eventPublisher);

    // Initialize Collection and Related Links components
    ArcadeCollectionRepository collectionRepository = new ArcadeCollectionRepository(database);
    CollectionPersistenceAdapter collectionPersistenceAdapter = new CollectionPersistenceAdapter(collectionRepository);
    CreateCollectionUseCase createCollectionUseCase = new CreateCollectionService(collectionPersistenceAdapter);
    ListCollectionsUseCase listCollectionsUseCase = new ListCollectionsService(collectionPersistenceAdapter);
    GetCollectionUseCase getCollectionUseCase = new GetCollectionService(collectionPersistenceAdapter);
    AddLinkToCollectionUseCase addLinkToCollectionUseCase = new AddLinkToCollectionService(collectionPersistenceAdapter);
    RemoveLinkFromCollectionUseCase removeLinkFromCollectionUseCase = new RemoveLinkFromCollectionService(collectionPersistenceAdapter);
    DeleteCollectionUseCase deleteCollectionUseCase = new DeleteCollectionService(collectionPersistenceAdapter);
    GetRelatedLinksUseCase getRelatedLinksUseCase = new GetRelatedLinksService(linkPersistenceAdapter);

    CollectionController collectionController = new CollectionController(
      createCollectionUseCase,
      listCollectionsUseCase,
      getCollectionUseCase,
      addLinkToCollectionUseCase,
      removeLinkFromCollectionUseCase,
      deleteCollectionUseCase
    );
    GetRelatedLinksController getRelatedLinksController = new GetRelatedLinksController(getRelatedLinksUseCase);

    // Initialize controllers
    NewLinkController newLinkController = new NewLinkController(newLinkUseCase);
    ListLinksController listLinksController = new ListLinksController(listLinksUseCase);
    GetContentController getContentController = new GetContentController(getContentUseCase);
    AuthenticationController authenticationController = new AuthenticationController(userService, authenticationService, authenticationService);

    // Initialize Link Management
    UpdateLinkUseCase updateLinkUseCase = new UpdateLinkService(linkPersistenceAdapter, linkPersistenceAdapter);
    DeleteLinkUseCase deleteLinkUseCase = new DeleteLinkService(linkPersistenceAdapter, linkPersistenceAdapter);
    LinkController linkController = new LinkController(updateLinkUseCase, deleteLinkUseCase);

    // Build and start web application
    Javalin app = new WebBuilder()
      .withAuthorizationService(authorizationService)
      .withAuthenticationController(authenticationController)
      .withLinkController(newLinkController)
      .withListLinksController(listLinksController)
      .withGetContentController(getContentController)
      .withCollectionController(collectionController)
      .withGetRelatedLinksController(getRelatedLinksController)
      .withLinkManagementController(linkController)
      .build();

    app.start(port);
    return app;
  }
}
