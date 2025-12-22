package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NonNull;

public record GraphData(@JsonProperty("nodes") @NonNull List<LinkNode> nodes, @JsonProperty("edges") @NonNull List<LinkEdge> edges) {
  public record LinkNode(@JsonProperty("id") @NonNull String id, @JsonProperty("label") @NonNull String label, @JsonProperty("url") @NonNull String url) {}

  public record LinkEdge(@JsonProperty("source") @NonNull String source, @JsonProperty("target") @NonNull String target) {}
}
