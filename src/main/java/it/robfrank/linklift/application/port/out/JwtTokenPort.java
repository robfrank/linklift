package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.User;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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
    String generateAccessToken(User user, LocalDateTime expirationTime);

    /**
     * Generates a JWT refresh token for a user.
     *
     * @param user the user to generate token for
     * @param expirationTime when the token should expire
     * @return the generated JWT refresh token
     */
    String generateRefreshToken(User user, LocalDateTime expirationTime);

    /**
     * Validates and parses a JWT token.
     *
     * @param token the JWT token to validate
     * @return TokenClaims if valid, empty if invalid
     */
    Optional<TokenClaims> validateToken(String token);

    /**
     * Extracts user ID from a JWT token without full validation.
     * Used for blacklist checking.
     *
     * @param token the JWT token
     * @return user ID if extractable, empty otherwise
     */
    Optional<String> extractUserIdFromToken(String token);

    /**
     * Gets the expiration time from a JWT token.
     *
     * @param token the JWT token
     * @return expiration time if extractable, empty otherwise
     */
    Optional<LocalDateTime> getTokenExpiration(String token);

    /**
     * Container for JWT token claims.
     */
    record TokenClaims(
        String userId,
        String username,
        String email,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        String tokenType,
        Map<String, Object> customClaims
    ) {}
}
