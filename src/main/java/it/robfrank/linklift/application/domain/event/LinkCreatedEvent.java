package it.robfrank.linklift.application.domain.event;

import it.robfrank.linklift.application.domain.model.Link;

public class LinkCreatedEvent extends DomainEvent {

  private final Link link;

  public LinkCreatedEvent(Link link) {
    super();
    this.link = link;
  }

  public Link getLink() {
    return link;
  }
}
