package it.robfrank.linklift;

import com.arcadedb.remote.RemoteDatabase;
import com.arcadedb.remote.RemoteServer;
import io.javalin.Javalin;
import it.robfrank.linklift.adapter.in.web.NewLinkController;
import it.robfrank.linklift.adapter.out.persitence.ArcadeLinkRepository;
import it.robfrank.linklift.adapter.out.persitence.LinkMapper;
import it.robfrank.linklift.adapter.out.persitence.LinkPersistenceAdapter;
import it.robfrank.linklift.application.domain.service.NewLinkService;
import it.robfrank.linklift.application.port.in.NewLinkUseCase;
import it.robfrank.linklift.config.DatabaseConfig;
import it.robfrank.linklift.config.WebBuilder;

public class Application {

  public static void main(String[] args) {
    String arcadedbServer = System.getProperty("linklift.arcadedb.host", "localhost");

    System.out.println("arcadedbServer = " + arcadedbServer);

    initializeDatabase(arcadedbServer);

    RemoteDatabase database = new RemoteDatabase(arcadedbServer, 2480, "linklift", "root", "playwithdata");
    ArcadeLinkRepository repository = new ArcadeLinkRepository(database, new LinkMapper());
    LinkPersistenceAdapter persistenceAdapter = new LinkPersistenceAdapter(repository);
    NewLinkUseCase useCase = new NewLinkService(persistenceAdapter);
    NewLinkController newLinkController = new NewLinkController(useCase);

    Javalin app = new WebBuilder().withLinkController(newLinkController).build();

    app.start(7070);
  }

  private static void initializeDatabase(String arcadedbServer) {
    RemoteServer server = new RemoteServer(arcadedbServer, 2480, "root", "playwithdata");

    if (!server.exists("linklift")) {
      server.create("linklift");
    }
    RemoteDatabase database = new RemoteDatabase(arcadedbServer, 2480, "linklift", "root", "playwithdata");
    DatabaseConfig.initializeSchema(database);
  }
}
