package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.event.LinksQueryEvent;
import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.LinkPage;
import it.robfrank.linklift.application.port.in.ListLinksQuery;
import it.robfrank.linklift.application.port.in.ListLinksUseCase;
import it.robfrank.linklift.application.port.out.DomainEventPublisher;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import java.util.Set;

public class ListLinksService implements ListLinksUseCase {

    private static final Set<String> VALID_SORT_FIELDS = Set.of("id", "url", "title", "description", "extractedAt", "contentType");

    private static final Set<String> VALID_SORT_DIRECTIONS = Set.of("ASC", "DESC");

    private final LoadLinksPort loadLinksPort;
    private final DomainEventPublisher eventPublisher;

    public ListLinksService(LoadLinksPort loadLinksPort, DomainEventPublisher eventPublisher) {
        this.loadLinksPort = loadLinksPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public LinkPage listLinks(ListLinksQuery query) {
        validateQuery(query);

        LinkPage result = loadLinksPort.loadLinks(query);

        // Publish domain event for analytics
        eventPublisher.publish(new LinksQueryEvent(query, result.totalElements()));

        return result;
    }

    private void validateQuery(ListLinksQuery query) {
        ValidationException validationException = new ValidationException("Invalid query parameters");

        if (query.page() < 0) {
            validationException.addFieldError("page", "Page must be >= 0");
        }

        if (query.size() < 1 || query.size() > 100) {
            validationException.addFieldError("size", "Size must be between 1 and 100");
        }

        if (!VALID_SORT_FIELDS.contains(query.sortBy())) {
            validationException.addFieldError("sortBy", "Invalid sort field. Valid fields: " + String.join(", ", VALID_SORT_FIELDS));
        }

        if (!VALID_SORT_DIRECTIONS.contains(query.sortDirection().toUpperCase())) {
            validationException.addFieldError("sortDirection", "Sort direction must be ASC or DESC");
        }

        // Validate userId is provided (required for user-owned links)
        if (query.userId() == null || query.userId().isBlank()) {
            validationException.addFieldError("userId", "User ID is required");
        }

        if (!validationException.getFieldErrors().isEmpty()) {
            throw validationException;
        }
    }
}
