package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.User;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Port interface for JWT token operations.
 * Provides abstraction for JWT token generation and validation.
 */
public interface JwtTokenPort {

    /**
     * Generates a JWT access token for a user.
     *
     * @param user the user to generate token for
     * @param expirationTime when the token should expire
     * @return the generated JWT token
     */
    @NonNull String generateAccessToken(@NonNull User user, @NonNull LocalDateTime expirationTime);

    /**
     * Generates a JWT refresh token for a user.
     *
     * @param user the user to generate token for
     * @param expirationTime when the token should expire
     * @return the generated JWT refresh token
     */
    @NonNull String generateRefreshToken(@NonNull User user, @NonNull LocalDateTime expirationTime);

    /**
     * Validates and parses a JWT token.
     *
     * @param token the JWT token to validate
     * @return TokenClaims if valid, empty if invalid
     */
    @NonNull Optional<TokenClaims> validateToken(@NonNull String token);

    /**
     * Extracts user ID from a JWT token without full validation.
     * Used for blacklist checking.
     *
     * @param token the JWT token
     * @return user ID if extractable, empty otherwise
     */
    @NonNull Optional<String> extractUserIdFromToken(@NonNull String token);

    /**
     * Gets the expiration time from a JWT token.
     *
     * @param token the JWT token
     * @return expiration time if extractable, empty otherwise
     */
    @NonNull Optional<LocalDateTime> getTokenExpiration(@NonNull String token);

    /**
     * Container for JWT token claims.
     */
    record TokenClaims(
        @NonNull String userId,
        @NonNull String username,
        @NonNull String email,
        @NonNull LocalDateTime issuedAt,
        @NonNull LocalDateTime expiresAt,
        @NonNull String tokenType,
        @NonNull Map<String, Object> customClaims
    ) {}
}
