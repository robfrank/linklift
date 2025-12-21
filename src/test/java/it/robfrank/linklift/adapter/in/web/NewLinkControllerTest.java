package it.robfrank.linklift.adapter.in.web;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.javalin.testtools.JavalinTest;
import it.robfrank.linklift.adapter.in.web.error.GlobalExceptionHandler;
import it.robfrank.linklift.application.domain.exception.LinkAlreadyExistsException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.in.NewLinkCommand;
import it.robfrank.linklift.application.port.in.NewLinkUseCase;
import java.time.LocalDateTime;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NewLinkControllerTest {

  private NewLinkUseCase newLinkUseCase;
  private NewLinkController newLinkController;

  @BeforeEach
  void setUp() {
    newLinkUseCase = Mockito.mock(NewLinkUseCase.class);
    newLinkController = new NewLinkController(newLinkUseCase);
  }

  @Test
  void processLink_shouldReturn201_whenLinkIsValid() {
    when(newLinkUseCase.newLink(any(NewLinkCommand.class))).thenReturn(
      new Link("123456", "http://www.google.com", "Google", "Search engine", LocalDateTime.now(), "text/html", java.util.List.of())
    );

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.put("/link", newLinkController::processLink);

      Response response = client.put(
        "/link",
        """
        {"url":"http://www.google.com",
        "title":"Google",
        "description":"Search engine"}
        """
      );
      assertThat(response.code()).isEqualTo(201);

      assertThatJson(response.body().string()).and(
        json -> json.node("link").isObject().containsKey("id").containsKey("extractedAt").containsKey("contentType").containsKey("url"),
        json -> json.node("status").isString().isEqualTo("Link received")
      );
    });
  }

  @Test
  void processLink_shouldReturn400_whenLinkIsInvalid() {
    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.put("/link", newLinkController::processLink);

      Response response = client.put(
        "/link",
        """
        {"url":"http://www.google.com"
        }
        """
      );
      assertThat(response.code()).isEqualTo(400);
      String body = response.body().string();
      assertThatJson(body).node("REQUEST_BODY").isArray().element(0).node("message").isEqualTo("Title cannot be empty");
    });
  }

  @Test
  void processLink_shouldReturn409_whenLinkAlreadyExists() {
    when(newLinkUseCase.newLink(any(NewLinkCommand.class))).thenThrow(new LinkAlreadyExistsException("http://www.google.com"));

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.put("/link", newLinkController::processLink);

      Response response = client.put(
        "/link",
        """
        {"url":"http://www.google.com",
        "title":"Google",
        "description":"Search engine"}
        """
      );
      assertThat(response.code()).isEqualTo(409);
      String body = response.body().string();

      assertThatJson(body).and(
        json -> json.node("status").isEqualTo(409),
        json -> json.node("code").isEqualTo(2001),
        json -> json.node("message").isString().contains("http://www.google.com")
      );
    });
  }
}
