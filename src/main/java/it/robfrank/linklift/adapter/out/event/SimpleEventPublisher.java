package it.robfrank.linklift.adapter.out.event;

import it.robfrank.linklift.application.domain.event.DomainEvent;
import it.robfrank.linklift.application.port.out.DomainEventPublisher;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SimpleEventPublisher implements DomainEventPublisher {

  private final List<DomainEventSubscriber<?>> subscribers = new CopyOnWriteArrayList<>();

  @Override
  public void publish(DomainEvent event) {
    subscribers.forEach(subscriber -> subscriber.handleEvent(event));
  }

  public <T extends DomainEvent> void subscribe(Class<T> eventType, Consumer<T> eventHandler) {
    subscribers.add(new DomainEventSubscriber<>(eventType, eventHandler));
  }

  public void clear() {
    subscribers.clear();
  }

  private static class DomainEventSubscriber<T extends DomainEvent> {

    private final Class<T> eventType;
    private final Consumer<T> eventHandler;

    public DomainEventSubscriber(Class<T> eventType, Consumer<T> eventHandler) {
      this.eventType = eventType;
      this.eventHandler = eventHandler;
    }

    @SuppressWarnings("unchecked")
    public void handleEvent(DomainEvent event) {
      if (eventType.isInstance(event)) {
        eventHandler.accept((T) event);
      }
    }
  }
}
