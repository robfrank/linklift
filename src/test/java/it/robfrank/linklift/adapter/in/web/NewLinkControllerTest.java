package it.robfrank.linklift.adapter.in.web;

import io.javalin.testtools.JavalinTest;
import it.robfrank.linklift.application.port.in.NewLinkCommand;
import it.robfrank.linklift.application.port.in.NewLinkUseCase;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class NewLinkControllerTest {

  private NewLinkUseCase    newLinkUseCase;
  private NewLinkController newLinkController;

  @BeforeEach
  void setUp() {
    newLinkUseCase = Mockito.mock(NewLinkUseCase.class);
    newLinkController = new NewLinkController(newLinkUseCase);
  }

  @Test
  void processLink_shouldReturn201_whenLinkIsValid() {
    when(newLinkUseCase.newLink(any(NewLinkCommand.class)))
        .thenReturn(true);

    JavalinTest.test((app, client) -> {
      app.post("/link", newLinkController::processLink);

      Response response = client.post("/link",
          """
              {"url":"http://www.google.com",
              "title":"Google",
              "description":"Search engine"}
              """);
//      System.out.println("response = " + response.body().string());
      assertThat(response.code()).isEqualTo(201);
      assertThatJson(response.body().string())
          .and(
              json -> json.node("linkCommand")
                  .isObject()
                  .isEqualTo("""
                      {
                        "url": "http://www.google.com",
                        "title": "Google",
                        "description": "Search engine"
                      }
                      """),
              json -> json.node("status").isString().isEqualTo("Link received")
          );
    });
  }

  @Test
  void processLink_shouldReturn400_whenLinkIsInvalid() {
    when(newLinkUseCase.newLink(any(NewLinkCommand.class))).thenReturn(false);

    JavalinTest.test((app, client) -> {
      app.post("/link", newLinkController::processLink);

      Response response = client.post("/link",
          """
              {"url":"http://www.google.com"
              }
              """);
      assertThat(response.code()).isEqualTo(400);
      ResponseBody body = response.body();

      assertThatJson(body.string())
          .node("REQUEST_BODY")
          .isArray()
          .element(0)
          .node("message").isEqualTo("Title cannot be empty");
    });
  }
}