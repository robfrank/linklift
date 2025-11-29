package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.event.DomainEvent;

public interface DomainEventPublisher {
  void publish(DomainEvent event);
}
