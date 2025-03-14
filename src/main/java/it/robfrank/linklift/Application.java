package it.robfrank.linklift;

import com.arcadedb.remote.RemoteDatabase;
import io.javalin.Javalin;
import it.robfrank.linklift.adapter.in.web.NewLinkController;
import it.robfrank.linklift.adapter.out.persitence.ArcadeLinkRepository;
import it.robfrank.linklift.adapter.out.persitence.LinkMapper;
import it.robfrank.linklift.adapter.out.persitence.LinkPersistenceAdapter;
import it.robfrank.linklift.application.domain.service.NewLinkService;
import it.robfrank.linklift.application.port.in.NewLinkUseCase;
import it.robfrank.linklift.config.WebBuilder;

public class Application {

  public static void main(String[] args) {
    String arcadedbServer = System.getProperty("linklift.arcadedb.url", "localhost");
    RemoteDatabase database = new RemoteDatabase(arcadedbServer, 2480, "linklift", "root", "playwithdata");
    ArcadeLinkRepository repository = new ArcadeLinkRepository(database, new LinkMapper());
    LinkPersistenceAdapter persistenceAdapter = new LinkPersistenceAdapter(repository);
    NewLinkUseCase useCase = new NewLinkService(persistenceAdapter);
    NewLinkController newLinkController = new NewLinkController(useCase);

    Javalin app = new WebBuilder().withLinkController(newLinkController).build();

    app.start(7070);
  }
  //  @NotNull
  //  private static Javalin getApp(NewLinkController newLinkController) {
  //    Javalin app = Javalin.create(config -> {
  //      config.bundledPlugins.enableCors(cors -> {
  //        cors.addRule(it -> {
  //          it.anyHost();
  //        });
  //      });
  //      config.router.apiBuilder(() -> {
  //        get("/", ctx -> ctx.result("Hello World"));
  //        get("/up", ctx -> ctx.status(200));
  //        path("/api/v1", () -> {
  //          post(newLinkController::processLink);
  //        });
  //      });
  //    });
  //    return app;
  //  }
}
