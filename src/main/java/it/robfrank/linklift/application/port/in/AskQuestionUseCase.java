package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.QuestionAnswer;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface AskQuestionUseCase {
  @NonNull
  QuestionAnswer ask(@NonNull AskQuestionCommand command);
}
