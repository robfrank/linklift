package it.robfrank.linklift.adapter.in.web;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import io.javalin.testtools.Response;
import it.robfrank.linklift.adapter.in.web.error.GlobalExceptionHandler;
import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.GraphData;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.LinkPage;
import it.robfrank.linklift.application.domain.model.ReadStatus;
import it.robfrank.linklift.application.domain.model.SecurityContext;
import it.robfrank.linklift.application.port.in.GetGraphUseCase;
import it.robfrank.linklift.application.port.in.ListLinksQuery;
import it.robfrank.linklift.application.port.in.ListLinksUseCase;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListLinksControllerTest {

  private ListLinksUseCase listLinksUseCase;
  private GetGraphUseCase getGraphUseCase;
  private ListLinksController listLinksController;

  @BeforeEach
  void setUp() {
    listLinksUseCase = Mockito.mock(ListLinksUseCase.class);
    getGraphUseCase = Mockito.mock(GetGraphUseCase.class);
    listLinksController = new ListLinksController.Builder().withListLinksUseCase(listLinksUseCase).withGetGraphUseCase(getGraphUseCase).build();
  }

  @Test
  void listLinks_shouldReturn200_withDefaultParameters() {
    // Given
    List<Link> links = List.of(
      new Link("1", "https://example.com", "Example", "Description", LocalDateTime.now(), "text/html", List.of(), ReadStatus.UNREAD, false, false)
    );
    LinkPage linkPage = new LinkPage(links, 0, 20, 1, 1, false, false);

    when(listLinksUseCase.listLinks(any(ListLinksQuery.class))).thenReturn(linkPage);

    Javalin app = Javalin.create(cfg -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(cfg.routes);
      cfg.routes.get("/links", listLinksController::listLinks);
    });

    JavalinTest.test(app, (server, client) -> {
      Response response = client.get("/links");

      assertThat(response.code()).isEqualTo(200);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(
        json -> json.node("message").isString().isEqualTo("Links retrieved successfully"),
        json -> json.node("data").isObject(),
        json -> json.node("data.content").isArray().hasSize(1),
        json -> json.node("data.page").isEqualTo(0),
        json -> json.node("data.size").isEqualTo(20),
        json -> json.node("data.totalElements").isEqualTo(1),
        json -> json.node("data.totalPages").isEqualTo(1),
        json -> json.node("data.hasNext").isEqualTo(false),
        json -> json.node("data.hasPrevious").isEqualTo(false)
      );
    });
  }

  @Test
  void listLinks_shouldReturn200_withCustomParameters() {
    // Given
    LinkPage linkPage = new LinkPage(List.of(), 1, 10, 25, 3, true, true);

    when(listLinksUseCase.listLinks(any(ListLinksQuery.class))).thenReturn(linkPage);

    Javalin app = Javalin.create(cfg -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(cfg.routes);
      cfg.routes.get("/links", listLinksController::listLinks);
    });

    JavalinTest.test(app, (server, client) -> {
      Response response = client.get("/links?page=1&size=10&sortBy=title&sortDirection=ASC");

      assertThat(response.code()).isEqualTo(200);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(
        json -> json.node("message").isString().isEqualTo("Links retrieved successfully"),
        json -> json.node("data.page").isEqualTo(1),
        json -> json.node("data.size").isEqualTo(10),
        json -> json.node("data.totalElements").isEqualTo(25),
        json -> json.node("data.totalPages").isEqualTo(3),
        json -> json.node("data.hasNext").isEqualTo(true),
        json -> json.node("data.hasPrevious").isEqualTo(true)
      );
    });
  }

  @Test
  void listLinks_shouldReturn400_whenValidationFails() {
    // Given
    ValidationException validationEx = new ValidationException("Invalid parameters");
    validationEx.addFieldError("page", "Page must be >= 0");

    when(listLinksUseCase.listLinks(any(ListLinksQuery.class))).thenThrow(validationEx);

    Javalin app = Javalin.create(cfg -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(cfg.routes);
      cfg.routes.get("/links", listLinksController::listLinks);
    });

    JavalinTest.test(app, (server, client) -> {
      Response response = client.get("/links?page=-1");

      assertThat(response.code()).isEqualTo(400);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(
        json -> json.node("status").isEqualTo(400),
        json -> json.node("code").isEqualTo(1001),
        json -> json.node("message").isString().contains("Invalid parameters"),
        json -> json.node("fieldErrors").isObject(),
        json -> json.node("fieldErrors.page").isString().contains("Page must be >= 0")
      );
    });
  }

  @Test
  void listLinks_shouldReturnEmptyList_whenNoLinks() {
    // Given
    LinkPage emptyPage = new LinkPage(List.of(), 0, 20, 0, 0, false, false);

    when(listLinksUseCase.listLinks(any(ListLinksQuery.class))).thenReturn(emptyPage);

    Javalin app = Javalin.create(cfg -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(cfg.routes);
      cfg.routes.get("/links", listLinksController::listLinks);
    });

    JavalinTest.test(app, (server, client) -> {
      Response response = client.get("/links");

      assertThat(response.code()).isEqualTo(200);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(
        json -> json.node("message").isString().isEqualTo("Links retrieved successfully"),
        json -> json.node("data.content").isArray().isEmpty(),
        json -> json.node("data.totalElements").isEqualTo(0),
        json -> json.node("data.totalPages").isEqualTo(0)
      );
    });
  }

  @Test
  void builder_shouldCreateController() {
    // When
    ListLinksController controller = new ListLinksController.Builder().withListLinksUseCase(listLinksUseCase).withGetGraphUseCase(getGraphUseCase).build();

    // Then
    assertThat(controller).isNotNull();
  }

  @Test
  void getGraph_shouldReturn200_withNodesAndEdges() {
    // Given
    GraphData graphData = new GraphData(
      List.of(new GraphData.LinkNode("node-1", "Example", "https://example.com"), new GraphData.LinkNode("node-2", "Related", "https://related.com")),
      List.of(new GraphData.LinkEdge("node-1", "node-2"))
    );
    when(getGraphUseCase.getGraphData("user-123")).thenReturn(graphData);

    Javalin app = Javalin.create(cfg -> {
      GlobalExceptionHandler.configure(cfg.routes);
      cfg.routes.before(ctx -> {
        var securityContext = new SecurityContext("user-123", "testuser", "test@example.com", List.of(), true, LocalDateTime.now(), "127.0.0.1", "test-agent");
        it.robfrank.linklift.adapter.in.web.security.SecurityContext.setSecurityContext(ctx, securityContext);
      });
      cfg.routes.get("/graph", listLinksController::getGraph);
    });

    JavalinTest.test(app, (server, client) -> {
      Response response = client.get("/graph");

      assertThat(response.code()).isEqualTo(200);
      String responseBody = response.body().string();
      assertThatJson(responseBody).and(
        json -> json.node("message").isEqualTo("Graph data retrieved successfully"),
        json -> json.node("data.nodes").isArray().hasSize(2),
        json -> json.node("data.edges").isArray().hasSize(1),
        json -> json.node("data.nodes[0].id").isEqualTo("node-1"),
        json -> json.node("data.edges[0].source").isEqualTo("node-1"),
        json -> json.node("data.edges[0].target").isEqualTo("node-2")
      );
    });
  }

  @Test
  void getGraph_shouldReturn401_whenUserNotAuthenticated() {
    Javalin app = Javalin.create(cfg -> {
      GlobalExceptionHandler.configure(cfg.routes);
      cfg.routes.get("/graph", listLinksController::getGraph);
    });

    JavalinTest.test(app, (server, client) -> {
      Response response = client.get("/graph");

      assertThat(response.code()).isEqualTo(401);
      String responseBody = response.body().string();
      assertThatJson(responseBody).node("message").isEqualTo("Unauthorized access");
    });
  }

  @Test
  void getGraph_shouldReturn200_withEmptyGraph_whenNoLinks() {
    // Given
    GraphData emptyGraph = new GraphData(List.of(), List.of());
    when(getGraphUseCase.getGraphData("user-123")).thenReturn(emptyGraph);

    Javalin app = Javalin.create(cfg -> {
      GlobalExceptionHandler.configure(cfg.routes);
      cfg.routes.before(ctx -> {
        var securityContext = new SecurityContext("user-123", "testuser", "test@example.com", List.of(), true, LocalDateTime.now(), "127.0.0.1", "test-agent");
        it.robfrank.linklift.adapter.in.web.security.SecurityContext.setSecurityContext(ctx, securityContext);
      });
      cfg.routes.get("/graph", listLinksController::getGraph);
    });

    JavalinTest.test(app, (server, client) -> {
      Response response = client.get("/graph");

      assertThat(response.code()).isEqualTo(200);
      String responseBody = response.body().string();
      assertThatJson(responseBody).and(json -> json.node("data.nodes").isArray().isEmpty(), json -> json.node("data.edges").isArray().isEmpty());
    });
  }
}
