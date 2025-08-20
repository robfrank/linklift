package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * User domain model representing a user in the LinkLift system.
 * This is an immutable record following the established pattern in the codebase.
 */
public record User(
  @JsonProperty("id") String id,
  @JsonProperty("username") String username,
  @JsonProperty("email") String email,
  @JsonProperty("passwordHash") String passwordHash,
  @JsonProperty("salt") String salt,
  @JsonProperty("createdAt") LocalDateTime createdAt,
  @JsonProperty("updatedAt") LocalDateTime updatedAt,
  @JsonProperty("isActive") boolean isActive,
  @JsonProperty("firstName") String firstName,
  @JsonProperty("lastName") String lastName,
  @JsonProperty("lastLoginAt") LocalDateTime lastLoginAt
) {
  public User {
    // Ensure timestamps are truncated to seconds for ArcadeDB compatibility
    createdAt = createdAt == null ? LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) : createdAt.truncatedTo(ChronoUnit.SECONDS);
    updatedAt = updatedAt == null ? null : updatedAt.truncatedTo(ChronoUnit.SECONDS);
    lastLoginAt = lastLoginAt == null ? null : lastLoginAt.truncatedTo(ChronoUnit.SECONDS);
  }

  /**
   * Creates a new User instance with updated last login timestamp.
   * Following immutable pattern for domain model updates.
   */
  public User withLastLogin(LocalDateTime lastLoginAt) {
    return new User(
      id,
      username,
      email,
      passwordHash,
      salt,
      createdAt,
      LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
      isActive,
      firstName,
      lastName,
      lastLoginAt == null ? null : lastLoginAt.truncatedTo(ChronoUnit.SECONDS)
    );
  }

  /**
   * Creates a new User instance with updated active status.
   */
  public User withActiveStatus(boolean isActive) {
    return new User(
      id,
      username,
      email,
      passwordHash,
      salt,
      createdAt,
      LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
      isActive,
      firstName,
      lastName,
      lastLoginAt
    );
  }

  /**
   * Creates a new User instance with updated password.
   */
  public User withPassword(String passwordHash, String salt) {
    return new User(
      id,
      username,
      email,
      passwordHash,
      salt,
      createdAt,
      LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
      isActive,
      firstName,
      lastName,
      lastLoginAt
    );
  }

  /**
   * Returns a user instance without sensitive information for API responses.
   */
  public User toPublic() {
    return new User(
      id,
      username,
      email,
      null, // Remove password hash
      null, // Remove salt
      createdAt,
      updatedAt,
      isActive,
      firstName,
      lastName,
      lastLoginAt
    );
  }

  /**
   * Gets the full name of the user, combining first and last names.
   */
  public String getFullName() {
    if (firstName == null && lastName == null) {
      return username;
    }
    if (firstName == null) {
      return lastName;
    }
    if (lastName == null) {
      return firstName;
    }
    return firstName + " " + lastName;
  }
}
