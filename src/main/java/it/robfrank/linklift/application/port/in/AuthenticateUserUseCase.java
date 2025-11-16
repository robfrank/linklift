package it.robfrank.linklift.application.port.in;

/**
 * Use case interface for user authentication.
 * Handles login and token generation.
 */
public interface AuthenticateUserUseCase {
  /**
   * Authenticates a user and returns authentication tokens.
   *
   * @param command the authentication command
   * @return authentication result with tokens
   * @throws it.robfrank.linklift.application.domain.exception.AuthenticationException if authentication fails
   */
  AuthenticationResult authenticate(AuthenticateUserCommand command);

  /**
   * Result of successful authentication.
   */
  record AuthenticationResult(
    String userId,
    String username,
    String email,
    String firstName,
    String lastName,
    String accessToken,
    String refreshToken,
    long accessTokenExpiresIn, // seconds
    long refreshTokenExpiresIn // seconds
  ) {}
}
