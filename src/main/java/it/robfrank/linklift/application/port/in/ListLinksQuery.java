package it.robfrank.linklift.application.port.in;

public record ListLinksQuery(int page, int size, String sortBy, String sortDirection, String userId) {
    public ListLinksQuery {
        // Apply defaults if not provided (but don't fix invalid values)
        if (sortBy == null || sortBy.isBlank()) sortBy = "extractedAt";
        if (sortDirection == null || sortDirection.isBlank()) sortDirection = "DESC";
    }

    // Factory method for creation with defaults
    public static ListLinksQuery of(Integer page, Integer size, String sortBy, String sortDirection) {
        int safePage = page != null ? page : 0;
        int safeSize = size != null ? size : 20;
        return new ListLinksQuery(safePage, safeSize, sortBy, sortDirection, null);
    }

    // Factory method for creation with user context
    public static ListLinksQuery forUser(Integer page, Integer size, String sortBy, String sortDirection, String userId) {
        int safePage = page != null ? page : 0;
        int safeSize = size != null ? size : 20;
        return new ListLinksQuery(safePage, safeSize, sortBy, sortDirection, userId);
    }
}
