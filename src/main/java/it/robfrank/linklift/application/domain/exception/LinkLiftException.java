package it.robfrank.linklift.application.domain.exception;

/**
 * Base exception class for all domain exceptions in the LinkLift application.
 * This provides a consistent exception hierarchy for the application.
 */
public class LinkLiftException extends RuntimeException {

  private final ErrorCode errorCode;

  public LinkLiftException(String message, ErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public LinkLiftException(String message, Throwable cause, ErrorCode errorCode) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
