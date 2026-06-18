package it.robfrank.linklift.config;

import io.javalin.Javalin;
import io.javalin.http.HandlerType;
import io.javalin.micrometer.MicrometerPlugin;
import io.javalin.router.JavalinDefaultRoutingApi;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builds the Javalin application.
 *
 * <p>Javalin 7 requires all routing (HTTP handlers, {@code before} filters and
 * exception handlers) to be registered via {@code config.routes} inside the
 * {@code Javalin.create(...)} configuration lambda; routes can no longer be added
 * to a running {@link Javalin} instance. To preserve the fluent
 * {@code withXxxController(...)} builder API, each registration is captured as a
 * deferred {@link Consumer} and applied against {@code config.routes} in {@link #build()}.
 */
public class WebBuilder {

  private AuthorizationService authorizationService;
  private JwtAuthenticationHandler jwtAuthenticationHandler;
  private RequireAuthentication requireAuthentication;
  private final PrometheusMeterRegistry registry;
  private final JvmGcMetrics jvmGcMetrics;

  private final List<Consumer<JavalinDefaultRoutingApi>> routeRegistrations = new ArrayList<>();

  public WebBuilder() {
    registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    new ClassLoaderMetrics().bindTo(registry);
    new JvmMemoryMetrics().bindTo(registry);
    jvmGcMetrics = new JvmGcMetrics();
    jvmGcMetrics.bindTo(registry);
    new ProcessorMetrics().bindTo(registry);
    new JvmThreadMetrics().bindTo(registry);
  }

  public WebBuilder withAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
    this.jwtAuthenticationHandler = new JwtAuthenticationHandler(authorizationService);
    this.requireAuthentication = new RequireAuthentication(authorizationService);

    // Apply JWT authentication to all requests
    routeRegistrations.add(routes -> routes.before(jwtAuthenticationHandler));

    return this;
  }

  public WebBuilder withAuthenticationController(AuthenticationController authenticationController) {
    routeRegistrations.add(routes -> {
      routes.post("/api/v1/auth/register", authenticationController::register);
      routes.post("/api/v1/auth/login", authenticationController::login);
      routes.post("/api/v1/auth/refresh", authenticationController::refreshToken);
      routes.post("/api/v1/auth/logout", authenticationController::logout);
    });
    return this;
  }

  public WebBuilder withLinkController(NewLinkController newLinkController) {
    routeRegistrations.add(routes -> {
      routes.before("/api/v1/link", requireAuthentication);
      routes.before("/api/v1/link", RequirePermission.any(authorizationService, Role.Permissions.CREATE_LINK));
      routes.put("/api/v1/link", newLinkController::processLink);
    });
    return this;
  }

  public WebBuilder withListLinksController(ListLinksController listLinksController) {
    routeRegistrations.add(routes -> {
      routes.before("/api/v1/links", requireAuthentication);
      routes.before("/api/v1/links", RequirePermission.any(authorizationService, Role.Permissions.READ_OWN_LINKS));
      routes.get("/api/v1/links", listLinksController::listLinks);

      routes.before("/api/v1/graph", requireAuthentication);
      routes.before("/api/v1/graph", RequirePermission.any(authorizationService, Role.Permissions.READ_OWN_LINKS));
      routes.get("/api/v1/graph", listLinksController::getGraph);
    });
    return this;
  }

  public WebBuilder withSearchController(SearchContentController searchContentController) {
    routeRegistrations.add(routes -> {
      routes.before("/api/v1/search", requireAuthentication);
      routes.before("/api/v1/search", RequirePermission.any(authorizationService, Role.Permissions.READ_OWN_LINKS));
      routes.get("/api/v1/search", searchContentController::search);
    });
    return this;
  }

  public WebBuilder withGetContentController(GetContentController getContentController) {
    routeRegistrations.add(routes -> {
      routes.before("/api/v1/links/{linkId}/content", requireAuthentication);
      routes.before("/api/v1/links/{linkId}/content", ctx -> {
        if (ctx.method().equals(HandlerType.GET)) {
          RequirePermission.any(authorizationService, Role.Permissions.READ_OWN_LINKS).handle(ctx);
        }
      });
      routes.get("/api/v1/links/{linkId}/content", getContentController::getContent);

      routes.before("/api/v1/links/{linkId}/content/refresh", ctx -> {
        if (ctx.method().equals(HandlerType.POST)) {
          RequirePermission.any(authorizationService, Role.Permissions.UPDATE_OWN_LINKS).handle(ctx);
        }
      });
      routes.post("/api/v1/links/{linkId}/content/refresh", getContentController::refreshContent);
    });
    return this;
  }

  public WebBuilder withDeleteContentController(DeleteContentController deleteContentController) {
    routeRegistrations.add(routes -> {
      routes.before("/api/v1/links/{linkId}/content", ctx -> {
        if (ctx.method().equals(HandlerType.DELETE)) {
          RequirePermission.any(authorizationService, Role.Permissions.DELETE_OWN_LINKS).handle(ctx);
        }
      });
      routes.delete("/api/v1/links/{linkId}/content", deleteContentController::deleteContent);
    });
    return this;
  }

  public WebBuilder withCollectionController(CollectionController collectionController) {
    routeRegistrations.add(routes -> {
      // Shared authentication for all /api/v1/collections endpoints
      routes.before("/api/v1/collections", requireAuthentication);
      routes.before("/api/v1/collections/{id}", requireAuthentication);
      routes.before("/api/v1/collections/{id}/links", requireAuthentication);
      routes.before("/api/v1/collections/{id}/links/{linkId}", requireAuthentication);

      // List collections (read-only, just needs authentication)
      routes.get("/api/v1/collections", collectionController::listCollections);

      // Get collection with links (read-only, just needs authentication)
      routes.get("/api/v1/collections/{id}", collectionController::getCollection);

      // Create collection
      routes.before("/api/v1/collections", RequirePermission.any(authorizationService, Role.Permissions.CREATE_COLLECTION));
      routes.post("/api/v1/collections", collectionController::createCollection);

      // Add link to collection
      routes.before("/api/v1/collections/{id}/links", RequirePermission.any(authorizationService, Role.Permissions.UPDATE_OWN_COLLECTION));
      routes.post("/api/v1/collections/{id}/links", collectionController::addLinkToCollection);

      // Remove link from collection
      routes.before("/api/v1/collections/{id}/links/{linkId}", RequirePermission.any(authorizationService, Role.Permissions.UPDATE_OWN_COLLECTION));
      routes.delete("/api/v1/collections/{id}/links/{linkId}", collectionController::removeLinkFromCollection);

      // Delete collection
      routes.before("/api/v1/collections/{id}", RequirePermission.any(authorizationService, Role.Permissions.DELETE_OWN_COLLECTION));
      routes.delete("/api/v1/collections/{id}", collectionController::deleteCollection);
    });
    return this;
  }

  public WebBuilder withAdminController(AdminController adminController) {
    routeRegistrations.add(routes -> {
      routes.before("/api/v1/admin/*", requireAuthentication);
      routes.before("/api/v1/admin/*", RequirePermission.any(authorizationService, Role.Permissions.ADMIN_ACCESS));
      routes.post("/api/v1/admin/backfill-embeddings", adminController::backfillEmbeddings);
    });
    return this;
  }

  public WebBuilder withGetRelatedLinksController(GetRelatedLinksController getRelatedLinksController) {
    routeRegistrations.add(routes -> {
      routes.before("/api/v1/links/{linkId}/related", requireAuthentication);
      routes.before("/api/v1/links/{linkId}/related", RequirePermission.any(authorizationService, Role.Permissions.READ_OWN_LINKS));
      routes.get("/api/v1/links/{linkId}/related", getRelatedLinksController::getRelatedLinks);
    });
    return this;
  }

  public WebBuilder withNoteController(NoteController noteController) {
    routeRegistrations.add(routes -> {
      routes.before("/api/v1/links/{linkId}/notes", requireAuthentication);
      routes.before("/api/v1/links/{linkId}/notes/{noteId}", requireAuthentication);

      routes.get("/api/v1/links/{linkId}/notes", noteController::getNotes);
      routes.post("/api/v1/links/{linkId}/notes", noteController::createNote);
      routes.patch("/api/v1/links/{linkId}/notes/{noteId}", noteController::updateNote);
      routes.delete("/api/v1/links/{linkId}/notes/{noteId}", noteController::deleteNote);
    });
    return this;
  }

  public WebBuilder withAskController(AskController askController) {
    routeRegistrations.add(routes -> {
      routes.before("/api/v1/ask", requireAuthentication);
      routes.before("/api/v1/ask", RequirePermission.any(authorizationService, Role.Permissions.READ_OWN_LINKS));
      routes.post("/api/v1/ask", askController::ask);
    });
    return this;
  }

  public WebBuilder withTagController(TagController tagController) {
    routeRegistrations.add(routes -> {
      routes.before("/api/v1/tags", requireAuthentication);
      routes.before("/api/v1/tags/{tagId}", requireAuthentication);
      routes.before("/api/v1/links/{linkId}/tags", requireAuthentication);
      routes.before("/api/v1/links/{linkId}/tags/{tagId}", requireAuthentication);
      routes.before("/api/v1/links/{linkId}/tags/suggest", requireAuthentication);

      routes.get("/api/v1/tags", tagController::listTags);
      routes.post("/api/v1/tags", tagController::createTag);
      routes.delete("/api/v1/tags/{tagId}", tagController::deleteTag);

      routes.get("/api/v1/links/{linkId}/tags", tagController::getTagsForLink);
      routes.post("/api/v1/links/{linkId}/tags", tagController::addTagToLink);
      routes.delete("/api/v1/links/{linkId}/tags/{tagId}", tagController::removeTagFromLink);
      routes.get("/api/v1/links/{linkId}/tags/suggest", tagController::suggestTags);
    });
    return this;
  }

  public WebBuilder withLinkManagementController(LinkController linkController) {
    routeRegistrations.add(routes -> {
      routes.before("/api/v1/links/{id}", requireAuthentication);
      routes.before("/api/v1/links/{id}/status", requireAuthentication);

      // Method-specific permission checks
      routes.before("/api/v1/links/{id}", ctx -> {
        HandlerType method = ctx.method();
        if (method == HandlerType.PATCH) {
          RequirePermission.any(authorizationService, Role.Permissions.UPDATE_OWN_LINKS).handle(ctx);
        } else if (method == HandlerType.DELETE) {
          RequirePermission.any(authorizationService, Role.Permissions.DELETE_OWN_LINKS).handle(ctx);
        }
      });
      routes.before("/api/v1/links/{id}/status", ctx -> {
        if (ctx.method() == HandlerType.PATCH) {
          RequirePermission.any(authorizationService, Role.Permissions.UPDATE_OWN_LINKS).handle(ctx);
        }
      });

      routes.patch("/api/v1/links/{id}", linkController::updateLink);
      routes.patch("/api/v1/links/{id}/status", linkController::updateLinkStatus);
      routes.delete("/api/v1/links/{id}", linkController::deleteLink);
    });
    return this;
  }

  public Javalin build() {
    return Javalin.create(config -> {
      // Register cleanup on application shutdown
      config.events.serverStopping(() -> {
        if (jvmGcMetrics != null) {
          jvmGcMetrics.close();
        }
      });

      config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));

      // Enable detailed logging
      config.bundledPlugins.enableDevLogging();

      // Enable Micrometer metrics
      config.registerPlugin(
        new MicrometerPlugin(micrometerConfig -> {
          micrometerConfig.registry = this.registry;
          micrometerConfig.tags = Tags.of("app", "linklift");
        })
      );

      // Base routes
      config.routes.get("/", ctx -> ctx.result("LinkLift"));
      config.routes.get("/up", ctx -> ctx.status(200));
      config.routes.get("/metrics", ctx -> ctx.result(registry.scrape()));

      // Global exception handlers
      GlobalExceptionHandler.configure(config.routes);

      // Controller routes registered via the fluent builder API
      routeRegistrations.forEach(registration -> registration.accept(config.routes));
    });
  }
}
