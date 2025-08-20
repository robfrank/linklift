package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.AuthToken;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for authentication token management.
 */
public interface AuthTokenPort {

    /**
     * Saves a new authentication token.
     */
    AuthToken saveToken(AuthToken token);

    /**
     * Finds a token by its value.
     */
    Optional<AuthToken> findByToken(String token);

    /**
     * Finds all valid tokens for a user and token type.
     */
    List<AuthToken> findValidTokensByUserAndType(String userId, AuthToken.TokenType tokenType);

    /**
     * Marks a token as used.
     */
    AuthToken markTokenAsUsed(String tokenId);

    /**
     * Revokes a token.
     */
    AuthToken revokeToken(String tokenId);

    /**
     * Revokes all tokens for a user.
     */
    void revokeAllUserTokens(String userId);

    /**
     * Revokes all tokens of a specific type for a user.
     */
    void revokeUserTokensByType(String userId, AuthToken.TokenType tokenType);

    /**
     * Cleans up expired tokens.
     */
    int cleanupExpiredTokens();

    /**
     * Gets all tokens for a user (for admin purposes).
     */
    List<AuthToken> findAllTokensForUser(String userId);

    /**
     * Deletes used tokens older than the specified cutoff date.
     */
    int deleteUsedTokensOlderThan(java.time.LocalDateTime cutoffDate);
}
