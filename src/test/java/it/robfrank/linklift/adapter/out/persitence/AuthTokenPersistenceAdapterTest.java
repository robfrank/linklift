package it.robfrank.linklift.adapter.out.persitence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.robfrank.linklift.application.domain.model.AuthToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthTokenPersistenceAdapterTest {

  @Mock
  private ArcadeAuthTokenRepository authTokenRepository;

  private AuthTokenPersistenceAdapter authTokenPersistenceAdapter;

  @BeforeEach
  void setUp() {
    authTokenPersistenceAdapter = new AuthTokenPersistenceAdapter(authTokenRepository);
  }

  @Test
  void saveToken_shouldCallRepository_andReturnResult() {
    // Arrange
    AuthToken token = createTestToken("token-123");
    AuthToken savedToken = createTestToken("token-123");
    when(authTokenRepository.save(token)).thenReturn(savedToken);

    // Act
    AuthToken result = authTokenPersistenceAdapter.saveToken(token);

    // Assert
    assertThat(result).isEqualTo(savedToken);
    verify(authTokenRepository).save(token);
  }

  @Test
  void findByToken_shouldCallRepository_andReturnResult() {
    // Arrange
    String tokenValue = "jwt-token-value";
    AuthToken token = createTestToken("token-123");
    when(authTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

    // Act
    Optional<AuthToken> result = authTokenPersistenceAdapter.findByToken(tokenValue);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(token);
    verify(authTokenRepository).findByToken(tokenValue);
  }

  @Test
  void findByToken_shouldReturnEmpty_whenTokenNotFound() {
    // Arrange
    String tokenValue = "nonexistent-token";
    when(authTokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

    // Act
    Optional<AuthToken> result = authTokenPersistenceAdapter.findByToken(tokenValue);

    // Assert
    assertThat(result).isEmpty();
    verify(authTokenRepository).findByToken(tokenValue);
  }

  @Test
  void findByUserId_shouldCallRepository_andReturnResult() {
    // Arrange
    String userId = "user-123";
    List<AuthToken> tokens = List.of(createTestToken("token-1"), createTestToken("token-2"));
    when(authTokenRepository.findByUserId(userId)).thenReturn(tokens);

    // Act
    List<AuthToken> result = authTokenPersistenceAdapter.findByUserId(userId);

    // Assert
    assertThat(result).isEqualTo(tokens);
    assertThat(result).hasSize(2);
    verify(authTokenRepository).findByUserId(userId);
  }

  @Test
  void findByUserId_shouldReturnEmptyList_whenNoTokensFound() {
    // Arrange
    String userId = "user-no-tokens";
    when(authTokenRepository.findByUserId(userId)).thenReturn(List.of());

    // Act
    List<AuthToken> result = authTokenPersistenceAdapter.findByUserId(userId);

    // Assert
    assertThat(result).isEmpty();
    verify(authTokenRepository).findByUserId(userId);
  }

  @Test
  void findByUserIdAndType_shouldCallRepository_andReturnResult() {
    // Arrange
    String userId = "user-123";
    AuthToken.TokenType tokenType = AuthToken.TokenType.REFRESH;
    List<AuthToken> tokens = List.of(createTestToken("token-1"));
    when(authTokenRepository.findByUserIdAndType(userId, tokenType)).thenReturn(tokens);

    // Act
    List<AuthToken> result = authTokenPersistenceAdapter.findByUserIdAndType(userId, tokenType);

    // Assert
    assertThat(result).isEqualTo(tokens);
    assertThat(result).hasSize(1);
    verify(authTokenRepository).findByUserIdAndType(userId, tokenType);
  }

  @Test
  void markTokenAsUsed_shouldCallRepository() {
    // Arrange
    String tokenId = "token-123";

    // Act
    authTokenPersistenceAdapter.markTokenAsUsed(tokenId);

    // Assert
    verify(authTokenRepository).markAsUsed(tokenId);
  }

  @Test
  void markTokenAsRevoked_shouldCallRepository() {
    // Arrange
    String tokenId = "token-123";

    // Act
    authTokenPersistenceAdapter.markTokenAsRevoked(tokenId);

    // Assert
    verify(authTokenRepository).markTokenAsRevoked(tokenId);
  }

  @Test
  void revokeAllUserTokens_shouldCallRepository() {
    // Arrange
    String userId = "user-123";

    // Act
    authTokenPersistenceAdapter.revokeAllUserTokens(userId);

    // Assert
    verify(authTokenRepository).revokeAllUserTokens(userId);
  }

  @Test
  void revokeUserTokensByType_shouldCallRepository() {
    // Arrange
    String userId = "user-123";
    AuthToken.TokenType tokenType = AuthToken.TokenType.REFRESH;

    // Act
    authTokenPersistenceAdapter.revokeUserTokensByType(userId, tokenType);

    // Assert
    verify(authTokenRepository).revokeUserTokensByType(userId, tokenType);
  }

  @Test
  void deleteExpiredTokens_shouldCallRepository_andReturnCount() {
    // Arrange
    int expectedDeletedCount = 5;
    when(authTokenRepository.deleteExpiredTokens()).thenReturn(expectedDeletedCount);

    // Act
    int result = authTokenPersistenceAdapter.deleteExpiredTokens();

    // Assert
    assertThat(result).isEqualTo(expectedDeletedCount);
    verify(authTokenRepository).deleteExpiredTokens();
  }

  @Test
  void deleteUsedTokensOlderThan_shouldCallRepository_andReturnCount() {
    // Arrange
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
    int expectedDeletedCount = 3;
    when(authTokenRepository.deleteUsedTokensOlderThan(cutoffDate)).thenReturn(expectedDeletedCount);

    // Act
    int result = authTokenPersistenceAdapter.deleteUsedTokensOlderThan(cutoffDate);

    // Assert
    assertThat(result).isEqualTo(expectedDeletedCount);
    verify(authTokenRepository).deleteUsedTokensOlderThan(cutoffDate);
  }

  private AuthToken createTestToken(String tokenId) {
    return new AuthToken(
      tokenId,
      "jwt-token-value",
      AuthToken.TokenType.SESSION,
      "user-123",
      LocalDateTime.now(),
      LocalDateTime.now().plusHours(1),
      null,
      false,
      "192.168.1.1",
      "Test-Agent"
    );
  }
}
