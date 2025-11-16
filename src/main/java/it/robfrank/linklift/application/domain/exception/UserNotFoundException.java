package it.robfrank.linklift.application.domain.exception;

/**
 * Exception thrown when a user is not found in the system.
 */
public class UserNotFoundException extends LinkLiftException {

  public UserNotFoundException(String identifier) {
    super("User not found: " + identifier, ErrorCode.USER_NOT_FOUND);
  }

  public UserNotFoundException(String message, Throwable cause) {
    super(message, cause, ErrorCode.USER_NOT_FOUND);
  }
}
