package it.robfrank.linklift.application.domain.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown for validation errors.
 * Contains a map of field names to error messages.
 */
public class ValidationException extends LinkLiftException {

  private final Map<String, String> fieldErrors;

  public ValidationException(String message) {
    super(message, ErrorCode.VALIDATION_ERROR);
    this.fieldErrors = new HashMap<>();
  }

  public ValidationException(String message, Map<String, String> fieldErrors) {
    super(message, ErrorCode.VALIDATION_ERROR);
    this.fieldErrors = new HashMap<>(fieldErrors);
  }

  public Map<String, String> getFieldErrors() {
    return fieldErrors;
  }

  public ValidationException addFieldError(String field, String message) {
    this.fieldErrors.put(field, message);
    return this;
  }
}
