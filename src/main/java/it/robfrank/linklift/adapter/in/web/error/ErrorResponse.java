package it.robfrank.linklift.adapter.in.web.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.robfrank.linklift.application.domain.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response format for the API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, int code, String message, Map<String, String> fieldErrors, String path, LocalDateTime timestamp) {
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private int status;
    private int code;
    private String message;
    private Map<String, String> fieldErrors;
    private String path;
    private LocalDateTime timestamp = LocalDateTime.now();

    public Builder status(int status) {
      this.status = status;
      return this;
    }

    public Builder errorCode(ErrorCode errorCode) {
      this.code = errorCode.getCode();
      this.message = errorCode.getDefaultMessage();
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder fieldErrors(Map<String, String> fieldErrors) {
      this.fieldErrors = fieldErrors;
      return this;
    }

    public Builder path(String path) {
      this.path = path;
      return this;
    }

    public ErrorResponse build() {
      return new ErrorResponse(status, code, message, fieldErrors, path, timestamp);
    }
  }
}
