package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.ReadStatus;

public record ListLinksQuery(
  int page,
  int size,
  String sortBy,
  String sortDirection,
  String userId,
  ReadStatus readStatus,
  Boolean archived,
  Boolean favorited
) {
  public ListLinksQuery {
    // Apply defaults if not provided (but don't fix invalid values)
    if (sortBy == null || sortBy.isBlank()) sortBy = "extractedAt";
    if (sortDirection == null || sortDirection.isBlank()) sortDirection = "DESC";
  }

  // Factory method for creation with defaults
  public static ListLinksQuery of(Integer page, Integer size, String sortBy, String sortDirection) {
    int safePage = page != null ? page : 0;
    int safeSize = size != null ? size : 20;
    return new ListLinksQuery(safePage, safeSize, sortBy, sortDirection, null, null, null, null);
  }

  // Factory method for creation with user context
  public static ListLinksQuery forUser(Integer page, Integer size, String sortBy, String sortDirection, String userId) {
    int safePage = page != null ? page : 0;
    int safeSize = size != null ? size : 20;
    return new ListLinksQuery(safePage, safeSize, sortBy, sortDirection, userId, null, null, null);
  }

  // Factory method for creation with user context and status filters
  public static ListLinksQuery forUserWithFilters(
    Integer page,
    Integer size,
    String sortBy,
    String sortDirection,
    String userId,
    ReadStatus readStatus,
    Boolean archived,
    Boolean favorited
  ) {
    int safePage = page != null ? page : 0;
    int safeSize = size != null ? size : 20;
    return new ListLinksQuery(safePage, safeSize, sortBy, sortDirection, userId, readStatus, archived, favorited);
  }
}
