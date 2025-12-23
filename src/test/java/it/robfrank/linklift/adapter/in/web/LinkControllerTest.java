package it.robfrank.linklift.adapter.in.web;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.javalin.testtools.JavalinTest;
import it.robfrank.linklift.adapter.in.web.error.GlobalExceptionHandler;
import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.SecurityContext;
import it.robfrank.linklift.application.port.in.DeleteLinkUseCase;
import it.robfrank.linklift.application.port.in.UpdateLinkCommand;
import it.robfrank.linklift.application.port.in.UpdateLinkUseCase;
import java.time.LocalDateTime;
import java.util.List;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LinkControllerTest {

  private UpdateLinkUseCase updateLinkUseCase;
  private DeleteLinkUseCase deleteLinkUseCase;
  private LinkController linkController;

  @BeforeEach
  void setUp() {
    updateLinkUseCase = Mockito.mock(UpdateLinkUseCase.class);
    deleteLinkUseCase = Mockito.mock(DeleteLinkUseCase.class);
    linkController = new LinkController(updateLinkUseCase, deleteLinkUseCase);
  }

  @Test
  void updateLink_shouldReturn200_whenLinkIsUpdated() {
    // Given
    Link updatedLink = new Link("link-123", "https://example.com", "Updated Title", "Updated Description", LocalDateTime.now(), "text/html", List.of());
    when(updateLinkUseCase.updateLink(any(UpdateLinkCommand.class))).thenReturn(updatedLink);

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);

      // Simulate authentication by setting SecurityContext
      app.before(ctx -> {
        var securityContext = new SecurityContext("user-123", "testuser", "test@example.com", List.of(), true, LocalDateTime.now(), "127.0.0.1", "test-agent");
        it.robfrank.linklift.adapter.in.web.security.SecurityContext.setSecurityContext(ctx, securityContext);
      });
      app.patch("/links/{id}", linkController::updateLink);

      Response response = client.patch(
        "/links/link-123",
        """
        {
          "title": "Updated Title",
          "description": "Updated Description"
        }
        """
      );

      assertThat(response.code()).isEqualTo(200);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(
        json -> json.node("data").isObject(),
        json -> json.node("data.id").isEqualTo("link-123"),
        json -> json.node("data.title").isEqualTo("Updated Title"),
        json -> json.node("data.description").isEqualTo("Updated Description")
      );
    });
  }

  @Test
  void updateLink_shouldReturn404_whenLinkNotFound() {
    // Given
    when(updateLinkUseCase.updateLink(any(UpdateLinkCommand.class))).thenThrow(new LinkNotFoundException("Link not found"));

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      // Simulate authentication by setting SecurityContext
      app.before(ctx -> {
        var securityContext = new SecurityContext("user-123", "testuser", "test@example.com", List.of(), true, LocalDateTime.now(), "127.0.0.1", "test-agent");
        it.robfrank.linklift.adapter.in.web.security.SecurityContext.setSecurityContext(ctx, securityContext);
      });
      app.patch("/links/{id}", linkController::updateLink);

      Response response = client.patch(
        "/links/non-existent-link",
        """
        {
          "title": "Updated Title",
          "description": "Updated Description"
        }
        """
      );

      assertThat(response.code()).isEqualTo(404);
      String responseBody = response.body().string();
      assertThatJson(responseBody).node("message").isString().contains("Link not found");
    });
  }

  @Test
  void updateLink_shouldUpdateOnlyTitle_whenDescriptionIsNull() {
    // Given
    Link updatedLink = new Link("link-123", "https://example.com", "New Title", "Old Description", LocalDateTime.now(), "text/html", List.of());
    when(updateLinkUseCase.updateLink(any(UpdateLinkCommand.class))).thenReturn(updatedLink);

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      // Simulate authentication by setting SecurityContext
      app.before(ctx -> {
        var securityContext = new SecurityContext("user-123", "testuser", "test@example.com", List.of(), true, LocalDateTime.now(), "127.0.0.1", "test-agent");
        it.robfrank.linklift.adapter.in.web.security.SecurityContext.setSecurityContext(ctx, securityContext);
      });
      app.patch("/links/{id}", linkController::updateLink);

      Response response = client.patch(
        "/links/link-123",
        """
        {
          "title": "New Title"
        }
        """
      );

      assertThat(response.code()).isEqualTo(200);
    });
  }

  @Test
  void deleteLink_shouldReturn204_whenLinkIsDeleted() {
    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      // Simulate authentication by setting SecurityContext
      app.before(ctx -> {
        var securityContext = new SecurityContext("user-123", "testuser", "test@example.com", List.of(), true, LocalDateTime.now(), "127.0.0.1", "test-agent");
        it.robfrank.linklift.adapter.in.web.security.SecurityContext.setSecurityContext(ctx, securityContext);
      });
      app.delete("/links/{id}", linkController::deleteLink);

      Response response = client.delete("/links/link-123");

      assertThat(response.code()).isEqualTo(204);
      verify(deleteLinkUseCase).deleteLink(eq("link-123"), eq("user-123"));
    });
  }

  @Test
  void deleteLink_shouldReturn404_whenLinkNotFound() {
    // Given
    doThrow(new LinkNotFoundException("Link not found or not owned by user")).when(deleteLinkUseCase).deleteLink(any(), any());

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      // Simulate authentication by setting SecurityContext
      app.before(ctx -> {
        var securityContext = new SecurityContext("user-123", "testuser", "test@example.com", List.of(), true, LocalDateTime.now(), "127.0.0.1", "test-agent");
        it.robfrank.linklift.adapter.in.web.security.SecurityContext.setSecurityContext(ctx, securityContext);
      });
      app.delete("/links/{id}", linkController::deleteLink);

      Response response = client.delete("/links/non-existent-link");

      assertThat(response.code()).isEqualTo(404);
      String responseBody = response.body().string();
      assertThatJson(responseBody).node("message").isString().contains("Link not found");
    });
  }

  @Test
  void deleteLink_shouldReturn401_whenUserNotAuthenticated() {
    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      // Don't set userId attribute to simulate unauthenticated request
      app.delete("/links/{id}", linkController::deleteLink);

      Response response = client.delete("/links/link-123");

      assertThat(response.code()).isEqualTo(401);
    });
  }
}
