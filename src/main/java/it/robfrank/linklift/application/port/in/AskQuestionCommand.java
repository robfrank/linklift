package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

public record AskQuestionCommand(@NonNull String question, @NonNull String userId) {}
