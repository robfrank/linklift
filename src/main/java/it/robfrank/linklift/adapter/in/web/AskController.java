package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import it.robfrank.linklift.adapter.in.web.security.SecurityContext;
import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.model.QuestionAnswer;
import it.robfrank.linklift.application.port.in.AskQuestionCommand;
import it.robfrank.linklift.application.port.in.AskQuestionUseCase;
import org.jspecify.annotations.NonNull;

public class AskController {

  private final AskQuestionUseCase askQuestionUseCase;

  public AskController(@NonNull AskQuestionUseCase askQuestionUseCase) {
    this.askQuestionUseCase = askQuestionUseCase;
  }

  public void ask(@NonNull Context ctx) {
    var body = ctx.bodyAsClass(AskRequest.class);
    if (body == null || body.question() == null || body.question().isBlank()) {
      ctx.status(HttpStatus.BAD_REQUEST);
      ctx.result("Question cannot be empty");
      return;
    }

    String userId = SecurityContext.getCurrentUserId(ctx);
    if (userId == null) {
      // Consistent with the other controllers: let GlobalExceptionHandler map this to 401.
      throw AuthenticationException.unauthorizedAccess();
    }

    QuestionAnswer result = askQuestionUseCase.ask(new AskQuestionCommand(body.question(), userId));
    ctx.json(result);
  }

  record AskRequest(String question) {}
}
