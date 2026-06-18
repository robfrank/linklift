package it.robfrank.linklift.application.port.out;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface QuestionAnswerPort {
  @NonNull
  String generateAnswer(@NonNull String question, @NonNull String context);
}
