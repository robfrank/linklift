package it.robfrank.linklift.adapter.out.persitence;

import it.robfrank.linklift.application.domain.model.AuthToken;
import it.robfrank.linklift.application.port.out.AuthTokenPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Persistence adapter that implements auth token management port.
 * Bridges the domain layer with the ArcadeDB persistence infrastructure.
 */
public class AuthTokenPersistenceAdapter implements AuthTokenPort {

  private final ArcadeAuthTokenRepository authTokenRepository;

  public AuthTokenPersistenceAdapter(ArcadeAuthTokenRepository authTokenRepository) {
    this.authTokenRepository = authTokenRepository;
  }

  @Override
  public AuthToken saveToken(AuthToken token) {
    return authTokenRepository.save(token);
  }

  @Override
  public Optional<AuthToken> findByToken(String token) {
    return authTokenRepository.findByToken(token);
  }

  @Override
  public List<AuthToken> findValidTokensByUserAndType(String userId, AuthToken.TokenType tokenType) {
    return authTokenRepository.findValidTokensByUserAndType(userId, tokenType);
  }

  @Override
  public AuthToken markTokenAsUsed(String tokenId) {
    return authTokenRepository.markAsUsed(tokenId);
  }

  @Override
  public AuthToken revokeToken(String tokenId) {
    return authTokenRepository.revoke(tokenId);
  }

  @Override
  public void revokeAllUserTokens(String userId) {
    authTokenRepository.revokeAllUserTokens(userId);
  }

  @Override
  public void revokeUserTokensByType(String userId, AuthToken.TokenType tokenType) {
    authTokenRepository.revokeUserTokensByType(userId, tokenType);
  }

  @Override
  public int cleanupExpiredTokens() {
    return authTokenRepository.deleteExpiredTokens();
  }

  @Override
  public List<AuthToken> findAllTokensForUser(String userId) {
    return authTokenRepository.findAllByUserId(userId);
  }

  @Override
  public int deleteUsedTokensOlderThan(LocalDateTime cutoffDate) {
    return authTokenRepository.deleteUsedTokensOlderThan(cutoffDate);
  }

  // Additional methods for test compatibility
  public List<AuthToken> findByUserIdAndType(String userId, AuthToken.TokenType tokenType) {
    return authTokenRepository.findByUserIdAndType(userId, tokenType);
  }

  public AuthToken markTokenAsRevoked(String tokenId) {
    return authTokenRepository.markTokenAsRevoked(tokenId);
  }

  public List<AuthToken> findByUserId(String userId) {
    return authTokenRepository.findByUserId(userId);
  }

  public int deleteExpiredTokens() {
    return authTokenRepository.deleteExpiredTokens();
  }
}
