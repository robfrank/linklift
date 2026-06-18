package it.robfrank.linklift.application.domain.validation;

import it.robfrank.linklift.application.domain.exception.ValidationException;

public class ValidationUtils {

  private ValidationUtils() {
    // Private constructor to prevent instantiation
  }

  public static void requireNotEmpty(String value, String fieldName) {
    if (value == null || value.trim().isEmpty()) {
      throw new ValidationException(fieldName + " cannot be empty").addFieldError(fieldName, "cannot be empty");
    }
  }

  public static void requireNotNull(Object value, String fieldName) {
    if (value == null) {
      throw new ValidationException(fieldName + " cannot be null").addFieldError(fieldName, "cannot be null");
    }
  }

  public static void requireMaxLength(String value, int maxLength, String fieldName) {
    if (value != null && value.length() > maxLength) {
      throw new ValidationException(fieldName + " must not exceed " + maxLength + " characters").addFieldError(
        fieldName,
        "must not exceed " + maxLength + " characters"
      );
    }
  }
}
