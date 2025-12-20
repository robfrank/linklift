package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.port.in.AuthenticateUserUseCase.AuthenticationResult;
import org.jspecify.annotations.NonNull;

/**
 * Use case interface for refreshing authentication tokens.
 */
public interface RefreshTokenUseCase {
  /**
   * Refreshes the access token using a valid refresh token.
   *
   * @param command the refresh token command
   * @return new authentication tokens
   * @throws it.robfrank.linklift.application.domain.exception.AuthenticationException if
   *                                                                                   refresh
   *                                                                                   token
   *                                                                                   is
   *                                                                                   invalid
   */
  @NonNull
  AuthenticationResult refreshToken(@NonNull RefreshTokenCommand command);
}
