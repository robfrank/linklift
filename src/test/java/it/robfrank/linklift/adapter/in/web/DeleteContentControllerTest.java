package it.robfrank.linklift.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.javalin.testtools.JavalinTest;
import it.robfrank.linklift.adapter.in.web.error.GlobalExceptionHandler;
import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.port.in.DeleteContentCommand;
import it.robfrank.linklift.application.port.in.DeleteContentUseCase;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeleteContentControllerTest {

  private DeleteContentUseCase deleteContentUseCase;
  private DeleteContentController deleteContentController;

  @BeforeEach
  void setUp() {
    deleteContentUseCase = Mockito.mock(DeleteContentUseCase.class);
    deleteContentController = new DeleteContentController(deleteContentUseCase);
  }

  @Test
  void deleteContent_shouldReturn204_whenContentIsDeleted() {
    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      app.delete("/content/{linkId}", deleteContentController::deleteContent);

      Response response = client.delete("/content/link-123");

      assertThat(response.code()).isEqualTo(204);
      verify(deleteContentUseCase).deleteContent(any(DeleteContentCommand.class));
    });
  }

  @Test
  void deleteContent_shouldReturn404_whenLinkNotFound() {
    // Given
    doThrow(new LinkNotFoundException("Link not found")).when(deleteContentUseCase).deleteContent(any(DeleteContentCommand.class));

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      app.delete("/content/{linkId}", deleteContentController::deleteContent);

      Response response = client.delete("/content/non-existent-link");

      assertThat(response.code()).isEqualTo(404);
    });
  }

  @Test
  void deleteContent_shouldCallUseCaseWithCorrectLinkId() {
    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      app.delete("/content/{linkId}", deleteContentController::deleteContent);

      client.delete("/content/specific-link-id");

      verify(deleteContentUseCase).deleteContent(any(DeleteContentCommand.class));
    });
  }
}
