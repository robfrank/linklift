package it.robfrank.linklift.config;

import static io.javalin.apibuilder.ApiBuilder.get;

import io.javalin.Javalin;
import io.javalin.http.HandlerType;
import io.javalin.micrometer.MicrometerPlugin;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import it.robfrank.linklift.adapter.in.web.*;
import it.robfrank.linklift.adapter.in.web.error.GlobalExceptionHandler;
import it.robfrank.linklift.adapter.in.web.security.JwtAuthenticationHandler;
import it.robfrank.linklift.adapter.in.web.security.RequireAuthentication;
import it.robfrank.linklift.adapter.in.web.security.RequirePermission;
import it.robfrank.linklift.application.domain.model.Role;
import it.robfrank.linklift.application.domain.service.AuthorizationService;

public class WebBuilder {

  private Javalin app;
  private AuthorizationService authorizationService;
  private JwtAuthenticationHandler jwtAuthenticationHandler;
  private RequireAuthentication requireAuthentication;
  private PrometheusMeterRegistry registry;
  private JvmGcMetrics jvmGcMetrics;

  public WebBuilder() {
    registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    new ClassLoaderMetrics().bindTo(registry);
    new JvmMemoryMetrics().bindTo(registry);
    jvmGcMetrics = new JvmGcMetrics();
    jvmGcMetrics.bindTo(registry);
    new ProcessorMetrics().bindTo(registry);
    new JvmThreadMetrics().bindTo(registry);

    app = Javalin.create(config -> {
      // Register cleanup on application shutdown
      config.events(event ->
        event.serverStopping(() -> {
          if (jvmGcMetrics != null) {
            jvmGcMetrics.close();
          }
        })
      );

      config.bundledPlugins.enableCors(cors -> {
        cors.addRule(it -> it.anyHost());
      });
      config.router.apiBuilder(() -> {
        get("/", ctx -> ctx.result("LinkLift"));
        get("/up", ctx -> ctx.status(200));
        get("/metrics", ctx -> ctx.result(registry.scrape()));
      });

      // Enable detailed logging
      config.bundledPlugins.enableDevLogging();

      // Enable Micrometer metrics
      config.registerPlugin(
        new MicrometerPlugin(micrometerConfig -> {
          micrometerConfig.registry = this.registry;
          micrometerConfig.tags = Tags.of("app", "linklift");
        })
      );
    });

    // Configure global exception handlers
    GlobalExceptionHandler.configure(app);
  }

  public WebBuilder withAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
    this.jwtAuthenticationHandler = new JwtAuthenticationHandler(authorizationService);
    this.requireAuthentication = new RequireAuthentication(authorizationService);

    // Apply JWT authentication to all requests
    app.before(jwtAuthenticationHandler);

    return this;
  }

  public WebBuilder withAuthenticationController(AuthenticationController authenticationController) {
    app.post("/api/v1/auth/register", authenticationController::register);
    app.post("/api/v1/auth/login", authenticationController::login);
    app.post("/api/v1/auth/refresh", authenticationController::refreshToken);
    app.post("/api/v1/auth/logout", authenticationController::logout);
    return this;
  }

  public WebBuilder withLinkController(NewLinkController newLinkController) {
    app.before("/api/v1/link", requireAuthentication);
    app.before("/api/v1/link", RequirePermission.any(authorizationService, Role.Permissions.CREATE_LINK));
    app.put("/api/v1/link", newLinkController::processLink);
    return this;
  }

  public WebBuilder withListLinksController(ListLinksController listLinksController) {
    app.before("/api/v1/links", requireAuthentication);
    app.before("/api/v1/links", RequirePermission.any(authorizationService, Role.Permissions.READ_OWN_LINKS));
    app.get("/api/v1/links", listLinksController::listLinks);
    return this;
  }

  public WebBuilder withSearchController(SearchContentController searchContentController) {
    app.before("/api/v1/search", requireAuthentication);
    app.before("/api/v1/search", RequirePermission.any(authorizationService, Role.Permissions.READ_OWN_LINKS));
    app.get("/api/v1/search", searchContentController::search);
    return this;
  }

  public WebBuilder withGetContentController(GetContentController getContentController) {
    app.before("/api/v1/links/{linkId}/content", requireAuthentication);
    app.before("/api/v1/links/{linkId}/content", ctx -> {
      if (ctx.method().equals(HandlerType.GET)) {
        RequirePermission.any(authorizationService, Role.Permissions.READ_OWN_LINKS).handle(ctx);
      }
    });
    app.get("/api/v1/links/{linkId}/content", getContentController::getContent);

    app.before("/api/v1/links/{linkId}/content/refresh", ctx -> {
      if (ctx.method().equals(HandlerType.POST)) {
        RequirePermission.any(authorizationService, Role.Permissions.UPDATE_OWN_LINKS).handle(ctx);
      }
    });
    app.post("/api/v1/links/{linkId}/content/refresh", getContentController::refreshContent);
    return this;
  }

  public WebBuilder withDeleteContentController(DeleteContentController deleteContentController) {
    app.before("/api/v1/links/{linkId}/content", ctx -> {
      if (ctx.method().equals(HandlerType.DELETE)) {
        RequirePermission.any(authorizationService, Role.Permissions.DELETE_OWN_LINKS).handle(ctx);
      }
    });
    app.delete("/api/v1/links/{linkId}/content", deleteContentController::deleteContent);
    return this;
  }

  public WebBuilder withCollectionController(CollectionController collectionController) {
    // Shared authentication for all /api/v1/collections endpoints
    app.before("/api/v1/collections", requireAuthentication);
    app.before("/api/v1/collections/{id}", requireAuthentication);
    app.before("/api/v1/collections/{id}/links", requireAuthentication);
    app.before("/api/v1/collections/{id}/links/{linkId}", requireAuthentication);

    // List collections (read-only, just needs authentication)
    app.get("/api/v1/collections", collectionController::listCollections);

    // Get collection with links (read-only, just needs authentication)
    app.get("/api/v1/collections/{id}", collectionController::getCollection);

    // Create collection
    app.before("/api/v1/collections", RequirePermission.any(authorizationService, Role.Permissions.CREATE_COLLECTION));
    app.post("/api/v1/collections", collectionController::createCollection);

    // Add link to collection
    app.before("/api/v1/collections/{id}/links", RequirePermission.any(authorizationService, Role.Permissions.UPDATE_OWN_COLLECTION));
    app.post("/api/v1/collections/{id}/links", collectionController::addLinkToCollection);

    // Remove link from collection
    app.before("/api/v1/collections/{id}/links/{linkId}", RequirePermission.any(authorizationService, Role.Permissions.UPDATE_OWN_COLLECTION));
    app.delete("/api/v1/collections/{id}/links/{linkId}", collectionController::removeLinkFromCollection);

    // Delete collection
    app.before("/api/v1/collections/{id}", RequirePermission.any(authorizationService, Role.Permissions.DELETE_OWN_COLLECTION));
    app.delete("/api/v1/collections/{id}", collectionController::deleteCollection);

    return this;
  }

  public WebBuilder withAdminController(AdminController adminController) {
    app.before("/api/v1/admin/*", requireAuthentication);
    app.before("/api/v1/admin/*", RequirePermission.any(authorizationService, Role.Permissions.ADMIN_ACCESS));
    app.post("/api/v1/admin/backfill-embeddings", adminController::backfillEmbeddings);
    return this;
  }

  public WebBuilder withGetRelatedLinksController(GetRelatedLinksController getRelatedLinksController) {
    app.before("/api/v1/links/{linkId}/related", requireAuthentication);
    app.before("/api/v1/links/{linkId}/related", RequirePermission.any(authorizationService, Role.Permissions.READ_OWN_LINKS));
    app.get("/api/v1/links/{linkId}/related", getRelatedLinksController::getRelatedLinks);
    return this;
  }

  public WebBuilder withLinkManagementController(LinkController linkController) {
    app.before("/api/v1/links/{id}", requireAuthentication);

    // Method-specific permission checks
    app.before("/api/v1/links/{id}", ctx -> {
      switch (ctx.method()) {
        case PATCH -> RequirePermission.any(authorizationService, Role.Permissions.UPDATE_OWN_LINKS).handle(ctx);
        case DELETE -> RequirePermission.any(authorizationService, Role.Permissions.DELETE_OWN_LINKS).handle(ctx);
        default -> {}
      }
    });

    app.patch("/api/v1/links/{id}", linkController::updateLink);
    app.delete("/api/v1/links/{id}", linkController::deleteLink);

    return this;
  }

  public Javalin build() {
    return app;
  }
}
