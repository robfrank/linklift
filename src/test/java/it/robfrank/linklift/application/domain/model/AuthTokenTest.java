package it.robfrank.linklift.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class AuthTokenTest {

    @Test
    void constructor_shouldSetAllPropertiesCorrectly() {
        // Arrange
        String id = "token-123";
        String token = "jwt-token-value";
        AuthToken.TokenType tokenType = AuthToken.TokenType.SESSION;
        String userId = "user-123";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        LocalDateTime usedAt = null;
        boolean isRevoked = false;
        String ipAddress = "192.168.1.1";
        String userAgent = "Test-Agent";

        // Act
        AuthToken authToken = new AuthToken(id, token, tokenType, userId, createdAt, expiresAt, usedAt, isRevoked, ipAddress, userAgent);

        // Assert
        assertThat(authToken.id()).isEqualTo(id);
        assertThat(authToken.token()).isEqualTo(token);
        assertThat(authToken.tokenType()).isEqualTo(tokenType);
        assertThat(authToken.userId()).isEqualTo(userId);
        assertThat(authToken.createdAt()).isEqualTo(createdAt.truncatedTo(ChronoUnit.SECONDS));
        assertThat(authToken.expiresAt()).isEqualTo(expiresAt.truncatedTo(ChronoUnit.SECONDS));
        assertThat(authToken.usedAt()).isNull();
        assertThat(authToken.isRevoked()).isFalse();
        assertThat(authToken.ipAddress()).isEqualTo(ipAddress);
        assertThat(authToken.userAgent()).isEqualTo(userAgent);
    }

    @Test
    void constructor_shouldSetCurrentTimestampWhenCreatedAtIsNull() {
        // Arrange
        LocalDateTime before = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        // Act
        AuthToken authToken = new AuthToken("id", "token", AuthToken.TokenType.SESSION, "userId", null, null, null, false, "ip", "agent");

        LocalDateTime after = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        // Assert
        assertThat(authToken.createdAt()).isNotNull();
        LocalDateTime createdAt = authToken.createdAt().truncatedTo(ChronoUnit.SECONDS);
        assertThat(createdAt).isAfterOrEqualTo(before);
        assertThat(createdAt).isBeforeOrEqualTo(after);
    }

    @Test
    void constructor_shouldTruncateTimestampsToSeconds() {
        // Arrange
        LocalDateTime timestampWithNanos = LocalDateTime.now().withNano(123456789);
        LocalDateTime expectedTruncated = timestampWithNanos.truncatedTo(ChronoUnit.SECONDS);

        // Act
        AuthToken authToken = new AuthToken(
            "id",
            "token",
            AuthToken.TokenType.SESSION,
            "userId",
            timestampWithNanos,
            timestampWithNanos,
            timestampWithNanos,
            false,
            "ip",
            "agent"
        );

        // Assert
        assertThat(authToken.createdAt()).isEqualTo(expectedTruncated);
        assertThat(authToken.expiresAt()).isEqualTo(expectedTruncated);
        assertThat(authToken.usedAt()).isEqualTo(expectedTruncated);
    }

    @Test
    void isValid_shouldReturnTrue_whenTokenIsNotExpiredNotRevokedNotUsed() {
        // Arrange
        AuthToken authToken = new AuthToken(
            "id",
            "token",
            AuthToken.TokenType.SESSION,
            "userId",
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            null,
            false,
            "ip",
            "agent"
        );

        // Act & Assert
        assertThat(authToken.isValid()).isTrue();
    }

    @Test
    void isValid_shouldReturnFalse_whenTokenIsRevoked() {
        // Arrange
        AuthToken authToken = new AuthToken(
            "id",
            "token",
            AuthToken.TokenType.SESSION,
            "userId",
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            null,
            true,
            "ip",
            "agent"
        );

        // Act & Assert
        assertThat(authToken.isValid()).isFalse();
    }

    @Test
    void isValid_shouldReturnFalse_whenTokenIsUsed() {
        // Arrange
        AuthToken authToken = new AuthToken(
            "id",
            "token",
            AuthToken.TokenType.SESSION,
            "userId",
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now(),
            false,
            "ip",
            "agent"
        );

        // Act & Assert
        assertThat(authToken.isValid()).isFalse();
    }

    @Test
    void isValid_shouldReturnFalse_whenTokenIsExpired() {
        // Arrange
        AuthToken authToken = new AuthToken(
            "id",
            "token",
            AuthToken.TokenType.SESSION,
            "userId",
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusHours(1),
            null,
            false,
            "ip",
            "agent"
        );

        // Act & Assert
        assertThat(authToken.isValid()).isFalse();
    }

    @Test
    void isValid_shouldReturnTrue_whenTokenHasNoExpirationTime() {
        // Arrange
        AuthToken authToken = new AuthToken("id", "token", AuthToken.TokenType.SESSION, "userId", LocalDateTime.now(), null, null, false, "ip", "agent");

        // Act & Assert
        assertThat(authToken.isValid()).isTrue();
    }

    @Test
    void isExpired_shouldReturnTrue_whenTokenIsExpired() {
        // Arrange
        AuthToken authToken = new AuthToken(
            "id",
            "token",
            AuthToken.TokenType.SESSION,
            "userId",
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusHours(1),
            null,
            false,
            "ip",
            "agent"
        );

        // Act & Assert
        assertThat(authToken.isExpired()).isTrue();
    }

    @Test
    void isExpired_shouldReturnFalse_whenTokenIsNotExpired() {
        // Arrange
        AuthToken authToken = new AuthToken(
            "id",
            "token",
            AuthToken.TokenType.SESSION,
            "userId",
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            null,
            false,
            "ip",
            "agent"
        );

        // Act & Assert
        assertThat(authToken.isExpired()).isFalse();
    }

    @Test
    void isExpired_shouldReturnFalse_whenTokenHasNoExpirationTime() {
        // Arrange
        AuthToken authToken = new AuthToken("id", "token", AuthToken.TokenType.SESSION, "userId", LocalDateTime.now(), null, null, false, "ip", "agent");

        // Act & Assert
        assertThat(authToken.isExpired()).isFalse();
    }

    @Test
    void markAsUsed_shouldCreateNewTokenWithUsedTimestamp() {
        // Arrange
        AuthToken originalToken = new AuthToken(
            "id",
            "token",
            AuthToken.TokenType.SESSION,
            "userId",
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            null,
            false,
            "ip",
            "agent"
        );
        LocalDateTime before = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        // Act
        AuthToken usedToken = originalToken.markAsUsed();

        LocalDateTime after = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        // Assert
        assertThat(usedToken.usedAt()).isNotNull();
        assertThat(usedToken.usedAt().truncatedTo(ChronoUnit.SECONDS)).isAfterOrEqualTo(before);
        assertThat(usedToken.usedAt().truncatedTo(ChronoUnit.SECONDS)).isBeforeOrEqualTo(after);
        assertThat(usedToken.id()).isEqualTo(originalToken.id());
        assertThat(usedToken.token()).isEqualTo(originalToken.token());
        assertThat(usedToken.isRevoked()).isEqualTo(originalToken.isRevoked());
    }

    @Test
    void markAsRevoked_shouldCreateNewTokenWithRevokedFlag() {
        // Arrange
        AuthToken originalToken = new AuthToken(
            "id",
            "token",
            AuthToken.TokenType.SESSION,
            "userId",
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            null,
            false,
            "ip",
            "agent"
        );

        // Act
        AuthToken revokedToken = originalToken.markAsRevoked();

        // Assert
        assertThat(revokedToken.isRevoked()).isTrue();
        assertThat(revokedToken.id()).isEqualTo(originalToken.id());
        assertThat(revokedToken.token()).isEqualTo(originalToken.token());
        assertThat(revokedToken.usedAt()).isEqualTo(originalToken.usedAt());
    }

    @Test
    void getSecondsUntilExpiration_shouldReturnNullWhenNoExpirationTime() {
        // Arrange
        AuthToken authToken = new AuthToken("id", "token", AuthToken.TokenType.SESSION, "userId", LocalDateTime.now(), null, null, false, "ip", "agent");

        // Act & Assert
        assertThat(authToken.getSecondsUntilExpiration()).isNull();
    }

    @Test
    void getSecondsUntilExpiration_shouldReturnZeroWhenTokenIsExpired() {
        // Arrange
        AuthToken authToken = new AuthToken(
            "id",
            "token",
            AuthToken.TokenType.SESSION,
            "userId",
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusHours(1),
            null,
            false,
            "ip",
            "agent"
        );

        // Act & Assert
        assertThat(authToken.getSecondsUntilExpiration()).isEqualTo(0L);
    }

    @Test
    void getSecondsUntilExpiration_shouldReturnPositiveValueWhenTokenIsValid() {
        // Arrange
        AuthToken authToken = new AuthToken(
            "id",
            "token",
            AuthToken.TokenType.SESSION,
            "userId",
            LocalDateTime.now(),
            LocalDateTime.now().plusSeconds(100),
            null,
            false,
            "ip",
            "agent"
        );

        // Act
        Long secondsUntilExpiration = authToken.getSecondsUntilExpiration();

        // Assert
        assertThat(secondsUntilExpiration).isNotNull();
        assertThat(secondsUntilExpiration).isGreaterThan(0L);
        assertThat(secondsUntilExpiration).isLessThanOrEqualTo(100L);
    }

    @Test
    void tokenType_fromString_shouldReturnCorrectType() {
        // Act & Assert
        assertThat(AuthToken.TokenType.fromString("SESSION")).isEqualTo(AuthToken.TokenType.SESSION);
        assertThat(AuthToken.TokenType.fromString("REFRESH")).isEqualTo(AuthToken.TokenType.REFRESH);
        assertThat(AuthToken.TokenType.fromString("PASSWORD_RESET")).isEqualTo(AuthToken.TokenType.PASSWORD_RESET);
        assertThat(AuthToken.TokenType.fromString("EMAIL_VERIFICATION")).isEqualTo(AuthToken.TokenType.EMAIL_VERIFICATION);
    }

    @Test
    void tokenType_fromString_shouldBeCaseInsensitive() {
        // Act & Assert
        assertThat(AuthToken.TokenType.fromString("session")).isEqualTo(AuthToken.TokenType.SESSION);
        assertThat(AuthToken.TokenType.fromString("Session")).isEqualTo(AuthToken.TokenType.SESSION);
    }

    @Test
    void tokenType_fromString_shouldThrowExceptionForInvalidType() {
        // Act & Assert
        assertThatThrownBy(() -> AuthToken.TokenType.fromString("INVALID"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown token type: INVALID");
    }

    @Test
    void tokenType_getValue_shouldReturnCorrectValue() {
        // Act & Assert
        assertThat(AuthToken.TokenType.SESSION.getValue()).isEqualTo("SESSION");
        assertThat(AuthToken.TokenType.REFRESH.getValue()).isEqualTo("REFRESH");
        assertThat(AuthToken.TokenType.PASSWORD_RESET.getValue()).isEqualTo("PASSWORD_RESET");
        assertThat(AuthToken.TokenType.EMAIL_VERIFICATION.getValue()).isEqualTo("EMAIL_VERIFICATION");
    }
}
