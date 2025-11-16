package it.robfrank.linklift.application.domain.event;

import java.time.LocalDateTime;
import org.jspecify.annotations.NonNull;

public class ContentDownloadFailedEvent implements DomainEvent {

  private final String linkId;
  private final String url;
  private final String errorMessage;
  private final String eventId;
  private final LocalDateTime timestamp;

  public ContentDownloadFailedEvent(@NonNull String linkId, @NonNull String url, @NonNull String errorMessage) {
    this.linkId = linkId;
    this.url = url;
    this.errorMessage = errorMessage;
    this.eventId = DomainEvent.super.getEventId();
    this.timestamp = LocalDateTime.now();
  }

  public String getLinkId() {
    return linkId;
  }

  public String getUrl() {
    return url;
  }

  public String getErrorMessage() {
    return errorMessage;
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
      "ContentDownloadFailedEvent{" +
      "linkId='" +
      linkId +
      '\'' +
      ", url='" +
      url +
      '\'' +
      ", errorMessage='" +
      errorMessage +
      '\'' +
      ", timestamp=" +
      getTimestamp() +
      '}'
    );
  }
}
