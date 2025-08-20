package it.robfrank.linklift.application.domain.exception;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends LinkLiftException {

  public AuthenticationException(String message) {
    super(message, ErrorCode.INVALID_CREDENTIALS);
  }

  public AuthenticationException(String message, ErrorCode errorCode) {
    super(message, errorCode);
  }

  public AuthenticationException(String message, Throwable cause, ErrorCode errorCode) {
    super(message, cause, errorCode);
  }

  public static AuthenticationException invalidCredentials() {
    return new AuthenticationException("Invalid username or password");
  }

  public static AuthenticationException tokenExpired() {
    return new AuthenticationException("Authentication token has expired", ErrorCode.TOKEN_EXPIRED);
  }

  public static AuthenticationException tokenInvalid() {
    return new AuthenticationException("Invalid authentication token", ErrorCode.TOKEN_INVALID);
  }

  public static AuthenticationException tokenRevoked() {
    return new AuthenticationException("Authentication token has been revoked", ErrorCode.TOKEN_REVOKED);
  }

  public static AuthenticationException unauthorizedAccess() {
    return new AuthenticationException("Unauthorized access", ErrorCode.UNAUTHORIZED_ACCESS);
  }

  public static AuthenticationException insufficientPermissions() {
    return new AuthenticationException("Insufficient permissions", ErrorCode.INSUFFICIENT_PERMISSIONS);
  }
}
