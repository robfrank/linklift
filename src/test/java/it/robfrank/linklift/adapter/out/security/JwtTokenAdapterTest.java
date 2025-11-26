package it.robfrank.linklift.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import it.robfrank.linklift.application.domain.model.User;
import it.robfrank.linklift.application.port.out.JwtTokenPort;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenAdapterTest {

    private static final String SECRET_KEY = "test-secret-key-for-jwt-signing-must-be-long-enough";

    private JwtTokenAdapter jwtTokenAdapter;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtTokenAdapter = new JwtTokenAdapter(SECRET_KEY);
        testUser = new User(
            "user-123",
            "testuser",
            "test@example.com",
            "hashed-password",
            "salt",
            LocalDateTime.now(),
            null,
            true,
            "John",
            "Doe",
            null
        );
    }

    @Test
    void generateAccessToken_shouldCreateValidToken() {
        // Arrange
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1);

        // Act
        String token = jwtTokenAdapter.generateAccessToken(testUser, expirationTime);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    void generateRefreshToken_shouldCreateValidToken() {
        // Arrange
        LocalDateTime expirationTime = LocalDateTime.now().plusDays(7);

        // Act
        String token = jwtTokenAdapter.generateRefreshToken(testUser, expirationTime);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    void validateToken_shouldReturnTokenClaims_whenValidAccessToken() {
        // Arrange
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1);
        String token = jwtTokenAdapter.generateAccessToken(testUser, expirationTime);

        // Act
        Optional<JwtTokenPort.TokenClaims> result = jwtTokenAdapter.validateToken(token);

        // Assert
        assertThat(result).isPresent();
        JwtTokenPort.TokenClaims claims = result.get();
        assertThat(claims.userId()).isEqualTo(testUser.id());
        assertThat(claims.username()).isEqualTo(testUser.username());
        assertThat(claims.email()).isEqualTo(testUser.email());
        assertThat(claims.tokenType()).isEqualTo("access");
        assertThat(claims.issuedAt()).isNotNull();
        assertThat(claims.expiresAt()).isAfter(LocalDateTime.now());
        assertThat(claims.customClaims()).containsEntry("firstName", "John");
        assertThat(claims.customClaims()).containsEntry("lastName", "Doe");
    }

    @Test
    void validateToken_shouldReturnTokenClaims_whenValidRefreshToken() {
        // Arrange
        LocalDateTime expirationTime = LocalDateTime.now().plusDays(7);
        String token = jwtTokenAdapter.generateRefreshToken(testUser, expirationTime);

        // Act
        Optional<JwtTokenPort.TokenClaims> result = jwtTokenAdapter.validateToken(token);

        // Assert
        assertThat(result).isPresent();
        JwtTokenPort.TokenClaims claims = result.get();
        assertThat(claims.userId()).isEqualTo(testUser.id());
        assertThat(claims.username()).isEqualTo(testUser.username());
        assertThat(claims.tokenType()).isEqualTo("refresh");
    }

    @Test
    void validateToken_shouldReturnEmpty_whenTokenIsExpired() throws Exception {
        // Arrange - Create a token that expires in 1 second
        LocalDateTime expirationTime = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1);
        String token = jwtTokenAdapter.generateAccessToken(testUser, expirationTime);

        // Wait for the token to expire
        Thread.sleep(1100); // Wait 1.1 seconds to ensure expiration

        // Act
        Optional<JwtTokenPort.TokenClaims> result = jwtTokenAdapter.validateToken(token);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void validateToken_shouldReturnEmpty_whenTokenIsInvalid() {
        // Arrange
        String invalidToken = "invalid.token.format";

        // Act
        Optional<JwtTokenPort.TokenClaims> result = jwtTokenAdapter.validateToken(invalidToken);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void validateToken_shouldReturnEmpty_whenTokenIsNull() {
        // Act
        Optional<JwtTokenPort.TokenClaims> result = jwtTokenAdapter.validateToken(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void validateToken_shouldReturnEmpty_whenTokenSignatureIsInvalid() {
        // Arrange
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1);
        String validToken = jwtTokenAdapter.generateAccessToken(testUser, expirationTime);
        String tamperedToken = validToken.substring(0, validToken.lastIndexOf('.')) + ".tampered";

        // Act
        Optional<JwtTokenPort.TokenClaims> result = jwtTokenAdapter.validateToken(tamperedToken);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void extractUserIdFromToken_shouldReturnUserId_whenValidToken() {
        // Arrange
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1);
        String token = jwtTokenAdapter.generateAccessToken(testUser, expirationTime);

        // Act
        Optional<String> result = jwtTokenAdapter.extractUserIdFromToken(token);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser.id());
    }

    @Test
    void extractUserIdFromToken_shouldReturnUserId_evenWhenTokenIsExpired() {
        // Arrange
        LocalDateTime expirationTime = LocalDateTime.now(ZoneOffset.UTC).minusHours(1); // Expired
        String token = jwtTokenAdapter.generateAccessToken(testUser, expirationTime);

        // Act
        Optional<String> result = jwtTokenAdapter.extractUserIdFromToken(token);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser.id());
    }

    @Test
    void extractUserIdFromToken_shouldReturnEmpty_whenTokenIsInvalid() {
        // Arrange
        String invalidToken = "invalid-token";

        // Act
        Optional<String> result = jwtTokenAdapter.extractUserIdFromToken(invalidToken);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getTokenExpiration_shouldReturnExpirationTime_whenValidToken() {
        // Arrange
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.SECONDS);
        String token = jwtTokenAdapter.generateAccessToken(testUser, expirationTime);

        // Act
        Optional<LocalDateTime> result = jwtTokenAdapter.getTokenExpiration(token);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().truncatedTo(ChronoUnit.SECONDS)).isEqualTo(expirationTime);
    }

    @Test
    void getTokenExpiration_shouldReturnEmpty_whenTokenIsInvalid() {
        // Arrange
        String invalidToken = "invalid-token";

        // Act
        Optional<LocalDateTime> result = jwtTokenAdapter.getTokenExpiration(invalidToken);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void tokenGenerationAndValidation_shouldWorkWithUserWithNullNames() {
        // Arrange
        User userWithNullNames = new User(
            "user-456",
            "testuser2",
            "test2@example.com",
            "hash",
            "salt",
            LocalDateTime.now(),
            null,
            true,
            null, // null first name
            null, // null last name
            null
        );
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1);

        // Act
        String token = jwtTokenAdapter.generateAccessToken(userWithNullNames, expirationTime);
        Optional<JwtTokenPort.TokenClaims> result = jwtTokenAdapter.validateToken(token);

        // Assert
        assertThat(result).isPresent();
        JwtTokenPort.TokenClaims claims = result.get();
        assertThat(claims.userId()).isEqualTo(userWithNullNames.id());
        assertThat(claims.username()).isEqualTo(userWithNullNames.username());
        assertThat(claims.email()).isEqualTo(userWithNullNames.email());
        assertThat(claims.customClaims()).containsEntry("firstName", null);
        assertThat(claims.customClaims()).containsEntry("lastName", null);
    }

    @Test
    void accessTokenAndRefreshToken_shouldHaveDifferentTypes() {
        // Arrange
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1);

        // Act
        String accessToken = jwtTokenAdapter.generateAccessToken(testUser, expirationTime);
        String refreshToken = jwtTokenAdapter.generateRefreshToken(testUser, expirationTime);

        Optional<JwtTokenPort.TokenClaims> accessClaims = jwtTokenAdapter.validateToken(accessToken);
        Optional<JwtTokenPort.TokenClaims> refreshClaims = jwtTokenAdapter.validateToken(refreshToken);

        // Assert
        assertThat(accessClaims).isPresent();
        assertThat(refreshClaims).isPresent();
        assertThat(accessClaims.get().tokenType()).isEqualTo("access");
        assertThat(refreshClaims.get().tokenType()).isEqualTo("refresh");
    }

    @Test
    void tokenValidation_shouldFailWithDifferentSecretKey() {
        // Arrange
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1);
        String token = jwtTokenAdapter.generateAccessToken(testUser, expirationTime);

        JwtTokenAdapter differentKeyAdapter = new JwtTokenAdapter("different-secret-key");

        // Act
        Optional<JwtTokenPort.TokenClaims> result = differentKeyAdapter.validateToken(token);

        // Assert
        assertThat(result).isEmpty();
    }
}
