package it.robfrank.linklift.application.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base interface for domain events.
 * All domain events should implement this interface.
 */
public interface DomainEvent {
  /**
   * Gets the unique identifier for this event.
   *
   * @return the event ID
   */
  default String getEventId() {
    return UUID.randomUUID().toString();
  }

  /**
   * Gets the timestamp when the event occurred.
   *
   * @return the event timestamp
   */
  default LocalDateTime getTimestamp() {
    return LocalDateTime.now();
  }
}
