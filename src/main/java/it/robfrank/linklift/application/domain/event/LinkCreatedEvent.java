package it.robfrank.linklift.application.domain.event;

import it.robfrank.linklift.application.domain.model.Link;
import java.time.LocalDateTime;

public class LinkCreatedEvent implements DomainEvent {

  private final Link link;
  private final String userId;
  private final String eventId;
  private final LocalDateTime timestamp;

  public LinkCreatedEvent(Link link, String userId) {
    this.link = link;
    this.userId = userId;
    this.eventId = getEventId();
    this.timestamp = LocalDateTime.now();
  }

  public Link getLink() {
    return link;
  }

  public String getUserId() {
    return userId;
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
