package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * AuthToken domain model representing authentication tokens in the system.
 * Supports various token types for session management, password reset, etc.
 */
public record AuthToken(
    @JsonProperty("id") @NonNull String id,
    @JsonProperty("token") @NonNull String token,
    @JsonProperty("tokenType") @NonNull TokenType tokenType,
    @JsonProperty("userId") @NonNull String userId,
    @JsonProperty("createdAt") @NonNull LocalDateTime createdAt,
    @JsonProperty("expiresAt") @Nullable LocalDateTime expiresAt,
    @JsonProperty("usedAt") @Nullable LocalDateTime usedAt,
    @JsonProperty("isRevoked") boolean isRevoked,
    @JsonProperty("ipAddress") @Nullable String ipAddress,
    @JsonProperty("userAgent") @Nullable String userAgent
) {
    public AuthToken {
        // Ensure timestamps are truncated to seconds for ArcadeDB compatibility
        createdAt = createdAt == null ? LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) : createdAt.truncatedTo(ChronoUnit.SECONDS);
        expiresAt = expiresAt == null ? null : expiresAt.truncatedTo(ChronoUnit.SECONDS);
        usedAt = usedAt == null ? null : usedAt.truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * Token types supported by the authentication system.
     */
    public enum TokenType {
        SESSION("SESSION"),
        REFRESH("REFRESH"),
        PASSWORD_RESET("PASSWORD_RESET"),
        EMAIL_VERIFICATION("EMAIL_VERIFICATION");

        private final String value;

        TokenType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static @NonNull TokenType fromString(@NonNull String value) {
            for (TokenType type : TokenType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown token type: " + value);
        }
    }

    /**
     * Checks if the token is currently valid (not expired and not revoked).
     */
    public boolean isValid() {
        if (isRevoked || usedAt != null) {
            return false;
        }
        return expiresAt == null || LocalDateTime.now().isBefore(expiresAt);
    }

    /**
     * Checks if the token has expired.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Creates a new AuthToken instance marked as used.
     */
    public @NonNull AuthToken markAsUsed() {
        return new AuthToken(
            id,
            token,
            tokenType,
            userId,
            createdAt,
            expiresAt,
            LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
            isRevoked,
            ipAddress,
            userAgent
        );
    }

    /**
     * Creates a new AuthToken instance marked as revoked.
     */
    public @NonNull AuthToken markAsRevoked() {
        return new AuthToken(id, token, tokenType, userId, createdAt, expiresAt, usedAt, true, ipAddress, userAgent);
    }

    /**
     * Returns the remaining time until expiration in seconds.
     * Returns null if the token doesn't have an expiration time.
     */
    public @Nullable Long getSecondsUntilExpiration() {
        if (expiresAt == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(expiresAt)) {
            return 0L;
        }
        return ChronoUnit.SECONDS.between(now, expiresAt);
    }
}
