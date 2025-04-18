package it.robfrank.linklift.application.domain.exception;

/**
 * Exception thrown for database-related errors.
 */
public class DatabaseException extends LinkLiftException {

  public DatabaseException(String message) {
    super(message, ErrorCode.DATABASE_ERROR);
  }

  public DatabaseException(String message, Throwable cause) {
    super(message, cause, ErrorCode.DATABASE_ERROR);
  }
}
