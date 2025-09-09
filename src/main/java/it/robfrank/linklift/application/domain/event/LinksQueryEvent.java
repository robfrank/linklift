package it.robfrank.linklift.application.domain.event;

import it.robfrank.linklift.application.port.in.ListLinksQuery;
import java.time.LocalDateTime;

public class LinksQueryEvent implements DomainEvent {

    private final ListLinksQuery query;
    private final long resultCount;
    private final String eventId;
    private final LocalDateTime timestamp;

    public LinksQueryEvent(ListLinksQuery query, long resultCount) {
        this.query = query;
        this.resultCount = resultCount;
        this.eventId = DomainEvent.super.getEventId();
        this.timestamp = LocalDateTime.now();
    }

    public ListLinksQuery getQuery() {
        return query;
    }

    public long getResultCount() {
        return resultCount;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
