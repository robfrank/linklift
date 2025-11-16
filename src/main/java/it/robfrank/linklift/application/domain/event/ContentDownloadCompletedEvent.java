package it.robfrank.linklift.application.domain.event;

import it.robfrank.linklift.application.domain.model.Content;
import java.time.LocalDateTime;
import org.jspecify.annotations.NonNull;

public class ContentDownloadCompletedEvent implements DomainEvent {

  private final Content content;
  private final String eventId;
  private final LocalDateTime timestamp;

  public ContentDownloadCompletedEvent(@NonNull Content content) {
    this.content = content;
    this.eventId = DomainEvent.super.getEventId();
    this.timestamp = LocalDateTime.now();
  }

  public Content getContent() {
    return content;
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
    return (
      "ContentDownloadCompletedEvent{" + "contentId='" + content.id() + '\'' + ", linkId='" + content.linkId() + '\'' + ", timestamp=" + getTimestamp() + '}'
    );
  }
}
