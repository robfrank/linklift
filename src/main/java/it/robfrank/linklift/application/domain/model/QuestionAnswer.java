package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NonNull;

public record QuestionAnswer(
  @JsonProperty("question") @NonNull String question,
  @JsonProperty("answer") @NonNull String answer,
  @JsonProperty("sources") @NonNull List<AnswerSource> sources
) {}
