package it.robfrank.linklift.adapter.out.event;

import static org.assertj.core.api.Assertions.assertThat;

import it.robfrank.linklift.application.domain.event.DomainEvent;
import it.robfrank.linklift.application.domain.event.LinkCreatedEvent;
import it.robfrank.linklift.application.domain.model.Link;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimpleEventPublisherTest {

  private SimpleEventPublisher eventPublisher;

  @BeforeEach
  void setUp() {
    eventPublisher = new SimpleEventPublisher();
  }

  @Test
  void subscribe_shouldReceiveMatchingEvents() {
    // Arrange
    AtomicBoolean linkCreatedHandlerCalled = new AtomicBoolean(false);
    AtomicBoolean genericEventHandlerCalled = new AtomicBoolean(false);

    Link link = new Link("id", "url", "title", "description", LocalDateTime.now(), "contentType", java.util.List.of());
    LinkCreatedEvent event = new LinkCreatedEvent(link, "user1");

    // Subscribe to specific event type
    eventPublisher.subscribe(LinkCreatedEvent.class, e -> {
      linkCreatedHandlerCalled.set(true);
      assertThat(e.getLink()).isEqualTo(link);
    });

    // Subscribe to base event type
    eventPublisher.subscribe(DomainEvent.class, e -> {
      genericEventHandlerCalled.set(true);
    });

    // Act
    eventPublisher.publish(event);

    // Assert
    assertThat(linkCreatedHandlerCalled.get()).isTrue();
    assertThat(genericEventHandlerCalled.get()).isTrue();
  }

  @Test
  void subscribe_shouldNotReceiveNonMatchingEvents() {
    // Arrange
    AtomicInteger handlerCallCount = new AtomicInteger(0);

    // Create a test-specific event type
    class TestEvent implements DomainEvent {

      private final String message;

      public TestEvent(String message) {
        this.message = message;
      }

      public String getMessage() {
        return message;
      }
    }

    TestEvent testEvent = new TestEvent("test");
    Link link = new Link("id", "url", "title", "description", LocalDateTime.now(), "contentType", java.util.List.of());
    LinkCreatedEvent linkEvent = new LinkCreatedEvent(link, "user1");

    // Subscribe only to LinkCreatedEvent
    eventPublisher.subscribe(LinkCreatedEvent.class, e -> {
      handlerCallCount.incrementAndGet();
    });

    // Act - publish TestEvent
    eventPublisher.publish(testEvent);

    // Assert - handler shouldn't be called
    assertThat(handlerCallCount.get()).isEqualTo(0);

    // Act - publish LinkCreatedEvent
    eventPublisher.publish(linkEvent);

    // Assert - handler should be called now
    assertThat(handlerCallCount.get()).isEqualTo(1);
  }

  @Test
  void clear_shouldRemoveAllSubscribers() {
    // Arrange
    AtomicInteger handlerCallCount = new AtomicInteger(0);

    Link link = new Link("id", "url", "title", "description", LocalDateTime.now(), "contentType", java.util.List.of());
    LinkCreatedEvent event = new LinkCreatedEvent(link, "user1");

    eventPublisher.subscribe(LinkCreatedEvent.class, e -> {
      handlerCallCount.incrementAndGet();
    });

    // Verify initial subscription works
    eventPublisher.publish(event);
    assertThat(handlerCallCount.get()).isEqualTo(1);

    // Act
    eventPublisher.clear();

    // Publish again
    eventPublisher.publish(event);

    // Assert
    assertThat(handlerCallCount.get()).isEqualTo(1); // Still 1, not incremented
  }
}
