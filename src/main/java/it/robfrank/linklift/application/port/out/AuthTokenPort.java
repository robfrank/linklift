package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.AuthToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Port interface for authentication token management.
 */
public interface AuthTokenPort {
  /**
   * Saves a new authentication token.
   */
  @NonNull
  AuthToken saveToken(@NonNull AuthToken token);

  /**
   * Finds a token by its value.
   */
  @NonNull
  Optional<AuthToken> findByToken(@NonNull String token);

  /**
   * Finds all valid tokens for a user and token type.
   */
  @NonNull
  List<AuthToken> findValidTokensByUserAndType(@NonNull String userId, AuthToken.@NonNull TokenType tokenType);

  /**
   * Marks a token as used.
   */
  @NonNull
  AuthToken markTokenAsUsed(@NonNull String tokenId);

  /**
   * Revokes a token.
   */
  @NonNull
  AuthToken revokeToken(@NonNull String tokenId);

  /**
   * Revokes all tokens for a user.
   */
  void revokeAllUserTokens(@NonNull String userId);

  /**
   * Revokes all tokens of a specific type for a user.
   */
  void revokeUserTokensByType(@NonNull String userId, AuthToken.@NonNull TokenType tokenType);

  /**
   * Cleans up expired tokens.
   */
  int cleanupExpiredTokens();

  /**
   * Gets all tokens for a user (for admin purposes).
   */
  @NonNull
  List<AuthToken> findAllTokensForUser(@NonNull String userId);

  /**
   * Deletes used tokens older than the specified cutoff date.
   */
  int deleteUsedTokensOlderThan(@NonNull LocalDateTime cutoffDate);
}
