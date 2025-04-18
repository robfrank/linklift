package it.robfrank.linklift.application.domain.exception;

/**
 * Error codes for the LinkLift application.
 * These codes provide a consistent way to identify different types of errors
 * and can be mapped to HTTP status codes and error messages.
 */
public enum ErrorCode {
  // Generic errors
  INTERNAL_ERROR(1000, "Internal server error"),
  VALIDATION_ERROR(1001, "Validation error"),

  // Domain-specific errors
  LINK_NOT_FOUND(2000, "Link not found"),
  LINK_ALREADY_EXISTS(2001, "Link already exists"),
  INVALID_LINK_URL(2002, "Invalid link URL"),

  // Infrastructure errors
  DATABASE_ERROR(3000, "Database error"),
  NETWORK_ERROR(3001, "Network error");

  private final int code;
  private final String defaultMessage;

  ErrorCode(int code, String defaultMessage) {
    this.code = code;
    this.defaultMessage = defaultMessage;
  }

  public int getCode() {
    return code;
  }

  public String getDefaultMessage() {
    return defaultMessage;
  }
}
