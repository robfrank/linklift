package it.robfrank.linklift.adapter.out.persitence;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.model.AuthToken;
import it.robfrank.linklift.config.DatabaseInitializer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ArcadeAuthTokenRepositoryTest {

    @Container
    private static final GenericContainer arcadeDBContainer = new GenericContainer("arcadedata/arcadedb:latest" )
            .withExposedPorts(2480)
            .withStartupTimeout(Duration.ofSeconds(90))
            .withEnv("JAVA_OPTS", """
                    -Darcadedb.dateImplementation=java.time.LocalDate
                    -Darcadedb.dateTimeImplementation=java.time.LocalDateTime
                    -Darcadedb.server.rootPassword=playwithdata
                    -Darcadedb.server.plugins=Postgres:com.arcadedb.postgres.PostgresProtocolPlugin
                    """)
            .waitingFor(Wait.forHttp("/api/v1/ready").forPort(2480).forStatusCode(204));

    private RemoteDatabase database;
    private ArcadeAuthTokenRepository authTokenRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeAll
    static void setup() {
        new DatabaseInitializer(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "root",
                "playwithdata").initializeDatabase();
    }

    @BeforeEach
    void setUp() {
        database = new RemoteDatabase(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "linklift", "root",
                "playwithdata");
        authTokenRepository = new ArcadeAuthTokenRepository(database, new AuthTokenMapper());

        // Clean up existing test data
        try {
            database.transaction(() -> {
                database.command("sql", "DELETE FROM AuthToken WHERE token LIKE 'test%' OR token LIKE 'session-token%' OR token LIKE 'refresh-token%' OR token LIKE 'old-used-token%' OR token LIKE 'recent-used-token%'");
            });
        } catch (Exception e) {
            // Ignore cleanup errors - database might be empty
        }
    }

    @Test
    void save_shouldPersistAuthToken() {
        // Arrange
        AuthToken testToken = createTestToken();

        // Act
        AuthToken savedToken = authTokenRepository.save(testToken);

        // Assert
        assertThat(savedToken).isNotNull();
        assertThat(savedToken.id()).isEqualTo(testToken.id());
        assertThat(savedToken.token()).isEqualTo(testToken.token());
        assertThat(savedToken.tokenType()).isEqualTo(testToken.tokenType());
        assertThat(savedToken.userId()).isEqualTo(testToken.userId());
        assertThat(savedToken.isRevoked()).isEqualTo(testToken.isRevoked());
        assertThat(savedToken.ipAddress()).isEqualTo(testToken.ipAddress());
        assertThat(savedToken.userAgent()).isEqualTo(testToken.userAgent());
    }

    @Test
    void findByToken_shouldReturnToken_whenTokenExists() {
        // Arrange
        AuthToken testToken = createTestToken();
        authTokenRepository.save(testToken);

        // Act
        Optional<AuthToken> foundToken = authTokenRepository.findByToken(testToken.token());

        // Assert
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().id()).isEqualTo(testToken.id());
        assertThat(foundToken.get().token()).isEqualTo(testToken.token());
        assertThat(foundToken.get().userId()).isEqualTo(testToken.userId());
    }

    @Test
    void findByToken_shouldReturnEmpty_whenTokenDoesNotExist() {
        // Act
        Optional<AuthToken> foundToken = authTokenRepository.findByToken("nonexistent-token");

        // Assert
        assertThat(foundToken).isEmpty();
    }

    @Test
    void findByUserId_shouldReturnAllUserTokens() {
        // Arrange
        String userId = "user-123";
        AuthToken sessionToken = createTestToken(userId, AuthToken.TokenType.SESSION, "session-token-" + UUID.randomUUID());
        AuthToken refreshToken = createTestToken(userId, AuthToken.TokenType.REFRESH, "refresh-token-" + UUID.randomUUID());

        authTokenRepository.save(sessionToken);
        authTokenRepository.save(refreshToken);

        // Act
        List<AuthToken> userTokens = authTokenRepository.findByUserId(userId);

        // Assert
        assertThat(userTokens).hasSize(2);
        assertThat(userTokens)
                .extracting(AuthToken::tokenType)
                .containsExactlyInAnyOrder(AuthToken.TokenType.SESSION, AuthToken.TokenType.REFRESH);
    }

    @Test
    void findByUserId_shouldReturnEmptyList_whenNoTokensForUser() {
        // Act
        List<AuthToken> userTokens = authTokenRepository.findByUserId("nonexistent-user");

        // Assert
        assertThat(userTokens).isEmpty();
    }

    @Test
    void findByUserIdAndType_shouldReturnTokensOfSpecificType() {
        // Arrange
        String userId = "user-123";
        AuthToken sessionToken = createTestToken(userId, AuthToken.TokenType.SESSION, "session-token-" + UUID.randomUUID());
        AuthToken refreshToken = createTestToken(userId, AuthToken.TokenType.REFRESH, "refresh-token-" + UUID.randomUUID());

        authTokenRepository.save(sessionToken);
        authTokenRepository.save(refreshToken);

        // Act
        List<AuthToken> refreshTokens = authTokenRepository.findByUserIdAndType(userId, AuthToken.TokenType.REFRESH);

        // Assert
        assertThat(refreshTokens).hasSize(1);
        assertThat(refreshTokens.getFirst().tokenType()).isEqualTo(AuthToken.TokenType.REFRESH);
        assertThat(refreshTokens.getFirst().token()).startsWith("refresh-token");
    }

    @Test
    void markTokenAsUsed_shouldUpdateUsedTimestamp() {
        // Arrange
        AuthToken testToken = createTestToken();
        AuthToken savedToken = authTokenRepository.save(testToken);

        assertThat(savedToken.usedAt()).isNull();

        // Act
        authTokenRepository.markTokenAsUsed(savedToken.id());

        // Assert
        Optional<AuthToken> updatedToken = authTokenRepository.findByToken(savedToken.token());
        assertThat(updatedToken).isPresent();
        assertThat(updatedToken.get().usedAt()).isNotNull();
        assertThat(updatedToken.get().usedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    void markTokenAsRevoked_shouldUpdateRevokedFlag() {
        // Arrange
        AuthToken testToken = createTestToken();
        AuthToken savedToken = authTokenRepository.save(testToken);

        assertThat(savedToken.isRevoked()).isFalse();

        // Act
        authTokenRepository.markTokenAsRevoked(savedToken.id());

        // Assert
        Optional<AuthToken> updatedToken = authTokenRepository.findByToken(savedToken.token());
        assertThat(updatedToken).isPresent();
        assertThat(updatedToken.get().isRevoked()).isTrue();
    }

    @Test
    void revokeAllUserTokens_shouldRevokeAllTokensForUser() {
        // Arrange
        String userId = "user-123";
        AuthToken sessionToken = createTestToken(userId, AuthToken.TokenType.SESSION, "session-token-" + UUID.randomUUID());
        AuthToken refreshToken = createTestToken(userId, AuthToken.TokenType.REFRESH, "refresh-token-" + UUID.randomUUID());

        authTokenRepository.save(sessionToken);
        authTokenRepository.save(refreshToken);

        // Act
        authTokenRepository.revokeAllUserTokens(userId);

        // Assert
        List<AuthToken> userTokens = authTokenRepository.findByUserId(userId);
        assertThat(userTokens).hasSize(2);
        assertThat(userTokens).allMatch(AuthToken::isRevoked);
    }

    @Test
    void revokeUserTokensByType_shouldRevokeOnlyTokensOfSpecificType() {
        // Arrange
        String userId = "user-123";
        AuthToken sessionToken = createTestToken(userId, AuthToken.TokenType.SESSION, "session-token-" + UUID.randomUUID());
        AuthToken refreshToken = createTestToken(userId, AuthToken.TokenType.REFRESH, "refresh-token-" + UUID.randomUUID());

        authTokenRepository.save(sessionToken);
        authTokenRepository.save(refreshToken);

        // Act
        authTokenRepository.revokeUserTokensByType(userId, AuthToken.TokenType.REFRESH);

        // Assert
        List<AuthToken> userTokens = authTokenRepository.findByUserId(userId);
        Optional<AuthToken> sessionTokenResult = userTokens.stream()
                .filter(t -> t.tokenType() == AuthToken.TokenType.SESSION)
                .findFirst();
        Optional<AuthToken> refreshTokenResult = userTokens.stream()
                .filter(t -> t.tokenType() == AuthToken.TokenType.REFRESH)
                .findFirst();

        assertThat(sessionTokenResult).isPresent();
        assertThat(sessionTokenResult.get().isRevoked()).isFalse();

        assertThat(refreshTokenResult).isPresent();
        assertThat(refreshTokenResult.get().isRevoked()).isTrue();
    }

    @Test
    void deleteExpiredTokens_shouldRemoveExpiredTokens() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        AuthToken expiredToken = createTestToken(
                "user-123",
                AuthToken.TokenType.SESSION,
                "expired-token",
                now.minusHours(2), // creates 2 hours ago
                now.minusHours(1)  // expired 1 hour ago
        );


        AuthToken validToken = createTestToken(
                "user-123",
                AuthToken.TokenType.SESSION,
                "valid-token",
                now,
                now.plusHours(1) // expires in 1 hour
        );

        System.out.println("validToken = " + validToken);
        System.out.println("expiredToken = " + expiredToken);
        authTokenRepository.save(expiredToken);
        authTokenRepository.save(validToken);

        // Act
        int deletedCount = authTokenRepository.deleteExpiredTokens();

        // Assert
        assertThat(deletedCount).isEqualTo(1);
        assertThat(authTokenRepository.findByToken(validToken.token())).isPresent();
        assertThat(authTokenRepository.findByToken(expiredToken.token())).isEmpty();
    }

    @Test
    void deleteUsedTokensOlderThan_shouldRemoveOldUsedTokens() {
        // Arrange
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);

        AuthToken oldUsedToken = createTestToken("user-123", AuthToken.TokenType.SESSION, "old-used-token-" + UUID.randomUUID());
        oldUsedToken = authTokenRepository.save(oldUsedToken);
        authTokenRepository.markTokenAsUsed(oldUsedToken.id());

        // Simulate old used token by directly updating the usedAt timestamp
        database.command("sql", "UPDATE AuthToken SET usedAt = ? WHERE id = ?",
                cutoffDate.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), oldUsedToken.id());

        AuthToken recentUsedToken = createTestToken("user-123", AuthToken.TokenType.SESSION, "recent-used-token-" + UUID.randomUUID());
        recentUsedToken = authTokenRepository.save(recentUsedToken);
        authTokenRepository.markTokenAsUsed(recentUsedToken.id());

        // Act
        int deletedCount = authTokenRepository.deleteUsedTokensOlderThan(cutoffDate);

        // Assert
        assertThat(deletedCount).isEqualTo(1);
        assertThat(authTokenRepository.findByToken(oldUsedToken.token())).isEmpty();
        assertThat(authTokenRepository.findByToken(recentUsedToken.token())).isPresent();
    }

    @Test
    void timestampFields_shouldBeTruncatedToSeconds() {
        // Arrange
        LocalDateTime timestampWithNanos = LocalDateTime.now().withNano(123456789);
        AuthToken testToken = new AuthToken(
                UUID.randomUUID().toString(),
                "test-token-nanos",
                AuthToken.TokenType.SESSION,
                "user-123",
                timestampWithNanos,
                timestampWithNanos.plusHours(1),
                timestampWithNanos.plusMinutes(30),
                false,
                "192.168.1.1",
                "Test-Agent"
        );

        // Act
        AuthToken savedToken = authTokenRepository.save(testToken);

        // Assert
        assertThat(savedToken.createdAt()).isEqualTo(timestampWithNanos.truncatedTo(ChronoUnit.SECONDS));
        assertThat(savedToken.expiresAt()).isEqualTo(timestampWithNanos.plusHours(1).truncatedTo(ChronoUnit.SECONDS));
        assertThat(savedToken.usedAt()).isEqualTo(timestampWithNanos.plusMinutes(30).truncatedTo(ChronoUnit.SECONDS));
    }

    private AuthToken createTestToken() {
        return createTestToken("user-123", AuthToken.TokenType.SESSION, "test-token-" + UUID.randomUUID());
    }

    private AuthToken createTestToken(String userId, AuthToken.TokenType tokenType, String tokenValue) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        return createTestToken(userId, tokenType, tokenValue, now, now.plusHours(1));
    }

    private AuthToken createTestToken(String userId,
                                      AuthToken.TokenType tokenType,
                                      String tokenValue,
                                      LocalDateTime createdAt,
                                      LocalDateTime expiresAt) {
        return new AuthToken(
                UUID.randomUUID().toString(),
                tokenValue,
                tokenType,
                userId,
                createdAt,
                expiresAt,
                null,
                false,
                "192.168.1.1",
                "Test-Agent"
        );
    }
}
