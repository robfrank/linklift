package it.robfrank.linklift.config;

import io.javalin.Javalin;
import it.robfrank.linklift.adapter.in.web.NewLinkController;

import static io.javalin.apibuilder.ApiBuilder.get;

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
    });
  }

  public WebBuilder withLinkController(NewLinkController newLinkController) {

    app.post("/api/v1/link", newLinkController::processLink);

    return this;
  }

  public Javalin build() {
    return app;
  }
}