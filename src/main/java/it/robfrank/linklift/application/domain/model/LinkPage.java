package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record LinkPage(
  @JsonProperty("content") List<Link> content,
  @JsonProperty("page") int page,
  @JsonProperty("size") int size,
  @JsonProperty("totalElements") long totalElements,
  @JsonProperty("totalPages") int totalPages,
  @JsonProperty("hasNext") boolean hasNext,
  @JsonProperty("hasPrevious") boolean hasPrevious
) {
  public LinkPage {
    // Calculate derived fields
    int calculatedTotalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
    totalPages = calculatedTotalPages;
    hasNext = page < totalPages - 1;
    hasPrevious = page > 0;
  }
}
