package it.robfrank.linklift.adapter.in.web;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.javalin.testtools.JavalinTest;
import it.robfrank.linklift.adapter.in.web.error.GlobalExceptionHandler;
import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.LinkPage;
import it.robfrank.linklift.application.port.in.ListLinksQuery;
import it.robfrank.linklift.application.port.in.ListLinksUseCase;
import java.time.LocalDateTime;
import java.util.List;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListLinksControllerTest {

  private ListLinksUseCase listLinksUseCase;
  private ListLinksController listLinksController;

  @BeforeEach
  void setUp() {
    listLinksUseCase = Mockito.mock(ListLinksUseCase.class);
    listLinksController = new ListLinksController(listLinksUseCase);
  }

  @Test
  void listLinks_shouldReturn200_withDefaultParameters() {
    // Given
    List<Link> links = List.of(new Link("1", "https://example.com", "Example", "Description", LocalDateTime.now(), "text/html"));
    LinkPage linkPage = new LinkPage(links, 0, 20, 1, 1, false, false);

    when(listLinksUseCase.listLinks(any(ListLinksQuery.class))).thenReturn(linkPage);

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.get("/links", listLinksController::listLinks);

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

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.get("/links", listLinksController::listLinks);

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

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.get("/links", listLinksController::listLinks);

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

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.get("/links", listLinksController::listLinks);

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
    ListLinksController controller = new ListLinksController.Builder().withListLinksUseCase(listLinksUseCase).build();

    // Then
    assertThat(controller).isNotNull();
  }
}
