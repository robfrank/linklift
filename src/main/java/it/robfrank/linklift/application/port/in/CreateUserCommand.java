package it.robfrank.linklift.application.port.in;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Command for creating a new user account.
 * Follows the established command pattern in the codebase.
 */
public record CreateUserCommand(
  @JsonProperty("username") String username,
  @JsonProperty("email") String email,
  @JsonProperty("password") String password,
  @JsonProperty("firstName") String firstName,
  @JsonProperty("lastName") String lastName
) {
  public CreateUserCommand {
    // Validation - ensure required fields are present
    if (username == null || username.trim().isEmpty()) {
      throw new IllegalArgumentException("Username is required");
    }
    if (email == null || email.trim().isEmpty()) {
      throw new IllegalArgumentException("Email is required");
    }
    if (password == null || password.trim().isEmpty()) {
      throw new IllegalArgumentException("Password is required");
    }

    // Normalize data
    username = username.trim().toLowerCase();
    email = email.trim().toLowerCase();
    firstName = firstName != null ? firstName.trim() : null;
    lastName = lastName != null ? lastName.trim() : null;
  }
}
