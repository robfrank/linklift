package it.robfrank.linklift.adapter.in.web;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.javalin.testtools.JavalinTest;
import it.robfrank.linklift.adapter.in.web.error.GlobalExceptionHandler;
import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.SecurityContext;
import it.robfrank.linklift.application.port.in.GetRelatedLinksUseCase;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GetRelatedLinksControllerTest {

  private GetRelatedLinksUseCase getRelatedLinksUseCase;
  private GetRelatedLinksController getRelatedLinksController;

  @BeforeEach
  void setUp() {
    getRelatedLinksUseCase = Mockito.mock(GetRelatedLinksUseCase.class);
    getRelatedLinksController = new GetRelatedLinksController(getRelatedLinksUseCase);
  }

  @Test
  void getRelatedLinks_shouldReturn200_withRelatedLinks() {
    // Given
    List<Link> relatedLinks = Arrays.asList(
      new Link("link-2", "https://example.com/2", "Related 1", "Desc 1", LocalDateTime.now(), "text/html", List.of()),
      new Link("link-3", "https://example.com/3", "Related 2", "Desc 2", LocalDateTime.now(), "text/html", List.of())
    );

    when(getRelatedLinksUseCase.getRelatedLinks(eq("link-1"), eq("user-123"))).thenReturn(relatedLinks);

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      // Simulate authentication by setting SecurityContext
      app.before(ctx -> {
        var securityContext = new SecurityContext("user-123", "testuser", "test@example.com", List.of(), true, LocalDateTime.now(), "127.0.0.1", "test-agent");
        it.robfrank.linklift.adapter.in.web.security.SecurityContext.setSecurityContext(ctx, securityContext);
      });
      app.get("/links/{linkId}/related", getRelatedLinksController::getRelatedLinks);

      Response response = client.get("/links/link-1/related");

      assertThat(response.code()).isEqualTo(200);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(
        json -> json.node("data").isArray().hasSize(2),
        json -> json.node("data[0].id").isEqualTo("link-2"),
        json -> json.node("data[1].id").isEqualTo("link-3")
      );
    });
  }

  @Test
  void getRelatedLinks_shouldReturn200_withEmptyList_whenNoRelatedLinks() {
    // Given
    when(getRelatedLinksUseCase.getRelatedLinks(eq("link-1"), eq("user-123"))).thenReturn(List.of());

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      // Simulate authentication by setting SecurityContext
      app.before(ctx -> {
        var securityContext = new SecurityContext("user-123", "testuser", "test@example.com", List.of(), true, LocalDateTime.now(), "127.0.0.1", "test-agent");
        it.robfrank.linklift.adapter.in.web.security.SecurityContext.setSecurityContext(ctx, securityContext);
      });
      app.get("/links/{linkId}/related", getRelatedLinksController::getRelatedLinks);

      Response response = client.get("/links/link-1/related");

      assertThat(response.code()).isEqualTo(200);

      String responseBody = response.body().string();
      assertThatJson(responseBody).node("data").isArray().isEmpty();
    });
  }

  @Test
  void getRelatedLinks_shouldReturn401_whenUserNotAuthenticated() {
    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      // Don't set userId attribute
      app.get("/links/{linkId}/related", getRelatedLinksController::getRelatedLinks);

      Response response = client.get("/links/link-1/related");

      assertThat(response.code()).isEqualTo(401);
      String responseBody = response.body().string();
      assertThatJson(responseBody).node("message").isEqualTo("Unauthorized");
    });
  }

  @Test
  void getRelatedLinks_shouldReturn404_whenLinkNotFound() {
    // Given
    when(getRelatedLinksUseCase.getRelatedLinks(eq("non-existent"), eq("user-123"))).thenThrow(new LinkNotFoundException("Link not found"));

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      // Simulate authentication by setting SecurityContext
      app.before(ctx -> {
        var securityContext = new SecurityContext("user-123", "testuser", "test@example.com", List.of(), true, LocalDateTime.now(), "127.0.0.1", "test-agent");
        it.robfrank.linklift.adapter.in.web.security.SecurityContext.setSecurityContext(ctx, securityContext);
      });
      app.get("/links/{linkId}/related", getRelatedLinksController::getRelatedLinks);

      Response response = client.get("/links/non-existent/related");

      assertThat(response.code()).isEqualTo(404);
    });
  }
}
