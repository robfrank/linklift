package it.robfrank.linklift;
import io.javalin.Javalin;
import it.robfrank.linklift.controller.LinkController;

public class Application {
  public static void main(String[] args) {
    Javalin app = Javalin.create(config -> {
      config.bundledPlugins.enableCors(cors -> {
        cors.addRule(it -> {
          it.anyHost();
        });
      });
    }).start(7070);

    // Registra i controller
    new LinkController(app);
  }
}