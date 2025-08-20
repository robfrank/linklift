package it.robfrank.linklift.application.domain.event;

import it.robfrank.linklift.application.domain.model.Link;
import java.time.LocalDateTime;

public class LinkCreatedEvent implements DomainEvent {

  private final Link link;
  private final String eventId;
  private final LocalDateTime timestamp;

  public LinkCreatedEvent(Link link) {
    this.link = link;
    this.eventId = DomainEvent.super.getEventId();
    this.timestamp = LocalDateTime.now();
  }

  public Link getLink() {
    return link;
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
