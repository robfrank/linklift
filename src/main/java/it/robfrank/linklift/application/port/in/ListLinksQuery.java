package it.robfrank.linklift.application.port.in;

public record ListLinksQuery(int page, int size, String sortBy, String sortDirection) {
  public ListLinksQuery {
    // Apply defaults if not provided (but don't fix invalid values)
    if (sortBy == null || sortBy.isBlank()) sortBy = "extractedAt";
    if (sortDirection == null || sortDirection.isBlank()) sortDirection = "DESC";
  }

  // Factory method for safe creation with defaults
  public static ListLinksQuery of(Integer page, Integer size, String sortBy, String sortDirection) {
    int safePage = (page != null && page >= 0) ? page : 0;
    int safeSize = (size != null && size >= 1 && size <= 100) ? size : 20;
    return new ListLinksQuery(safePage, safeSize, sortBy, sortDirection);
  }
}
