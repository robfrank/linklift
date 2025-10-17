package it.robfrank.linklift.application.domain.event;

import java.time.LocalDateTime;
import org.jspecify.annotations.NonNull;

public class ContentDownloadStartedEvent implements DomainEvent {

    private final String linkId;
    private final String url;
    private final String eventId;
    private final LocalDateTime timestamp;

    public ContentDownloadStartedEvent(@NonNull String linkId, @NonNull String url) {
        this.linkId = linkId;
        this.url = url;
        this.eventId = DomainEvent.super.getEventId();
        this.timestamp = LocalDateTime.now();
    }

    public String getLinkId() {
        return linkId;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "ContentDownloadStartedEvent{" + "linkId='" + linkId + '\'' + ", url='" + url + '\'' + ", timestamp=" + getTimestamp() + '}';
    }
}
