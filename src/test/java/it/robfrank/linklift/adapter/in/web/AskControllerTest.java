package it.robfrank.linklift.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.model.AnswerSource;
import it.robfrank.linklift.application.domain.model.QuestionAnswer;
import it.robfrank.linklift.application.port.in.AskQuestionCommand;
import it.robfrank.linklift.application.port.in.AskQuestionUseCase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AskControllerTest {

  @Mock
  private AskQuestionUseCase askQuestionUseCase;

  @Mock
  private Context context;

  private AskController askController;

  @BeforeEach
  void setUp() {
    askController = new AskController(askQuestionUseCase);
  }

  @Test
  void ask_shouldReturnAnswer_whenAuthenticated() {
    // Arrange
    var securityContext = new it.robfrank.linklift.application.domain.model.SecurityContext(
      "user-123",
      "testuser",
      "test@example.com",
      List.of(),
      true,
      null,
      null,
      null
    );
    when(context.attribute("security.context")).thenReturn(securityContext);
    when(context.bodyAsClass(AskController.AskRequest.class)).thenReturn(new AskController.AskRequest("What is Java?"));

    var expectedAnswer = new QuestionAnswer(
      "What is Java?",
      "Java is a programming language.",
      List.of(new AnswerSource("link-1", "Java Tutorial", "http://example.com/java", "Java is a widely-used language..."))
    );
    when(askQuestionUseCase.ask(any(AskQuestionCommand.class))).thenReturn(expectedAnswer);

    // Act
    askController.ask(context);

    // Assert
    ArgumentCaptor<AskQuestionCommand> commandCaptor = ArgumentCaptor.forClass(AskQuestionCommand.class);
    verify(askQuestionUseCase).ask(commandCaptor.capture());
    assertThat(commandCaptor.getValue().question()).isEqualTo("What is Java?");
    assertThat(commandCaptor.getValue().userId()).isEqualTo("user-123");
    verify(context).json(expectedAnswer);
  }

  @Test
  void ask_shouldReturn401_whenNotAuthenticated() {
    // Arrange - no security context set, so SecurityContext.getCurrentUserId returns null
    when(context.bodyAsClass(AskController.AskRequest.class)).thenReturn(new AskController.AskRequest("What is Java?"));

    // Act + Assert - unauthenticated requests throw, which GlobalExceptionHandler maps to 401
    assertThatThrownBy(() -> askController.ask(context)).isInstanceOf(AuthenticationException.class);
    verify(askQuestionUseCase, never()).ask(any());
  }

  @Test
  void ask_shouldReturn400_whenQuestionIsEmpty() {
    // Arrange
    when(context.bodyAsClass(AskController.AskRequest.class)).thenReturn(new AskController.AskRequest(""));
    when(context.status(HttpStatus.BAD_REQUEST)).thenReturn(context);

    // Act
    askController.ask(context);

    // Assert
    verify(context).status(HttpStatus.BAD_REQUEST);
    verify(context).result("Question cannot be empty");
    verify(askQuestionUseCase, never()).ask(any());
  }

  @Test
  void ask_shouldReturn400_whenQuestionIsNull() {
    // Arrange
    when(context.bodyAsClass(AskController.AskRequest.class)).thenReturn(new AskController.AskRequest(null));
    when(context.status(HttpStatus.BAD_REQUEST)).thenReturn(context);

    // Act
    askController.ask(context);

    // Assert
    verify(context).status(HttpStatus.BAD_REQUEST);
    verify(askQuestionUseCase, never()).ask(any());
  }
}
