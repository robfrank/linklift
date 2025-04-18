package it.robfrank.linklift.application.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class DomainEvent {

  private final String eventId;
  private final LocalDateTime timestamp;

  protected DomainEvent() {
    this.eventId = UUID.randomUUID().toString();
    this.timestamp = LocalDateTime.now();
  }

  public String getEventId() {
    return eventId;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }
}
