package it.robfrank.linklift.config;

import static io.javalin.apibuilder.ApiBuilder.get;

import io.javalin.Javalin;
import it.robfrank.linklift.adapter.in.web.NewLinkController;
import it.robfrank.linklift.adapter.in.web.error.GlobalExceptionHandler;

public class WebBuilder {

  private Javalin app;

  public WebBuilder() {
    app = Javalin.create(config -> {
      config.bundledPlugins.enableCors(cors -> {
        cors.addRule(it -> it.anyHost());
      });
      config.router.apiBuilder(() -> {
        get("/", ctx -> ctx.result("LinkLift"));
        get("/up", ctx -> ctx.status(200));
      });

      // Enable detailed logging
      config.bundledPlugins.enableDevLogging();
    });

    // Configure global exception handlers
    GlobalExceptionHandler.configure(app);
  }

  public WebBuilder withLinkController(NewLinkController newLinkController) {
    app.put("/api/v1/link", newLinkController::processLink);

    return this;
  }

  public Javalin build() {
    return app;
  }
}
