package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.exception.ErrorCode;
import it.robfrank.linklift.application.domain.model.AuthToken;
import it.robfrank.linklift.application.domain.model.User;
import it.robfrank.linklift.application.port.in.AuthenticateUserCommand;
import it.robfrank.linklift.application.port.in.AuthenticateUserUseCase;
import it.robfrank.linklift.application.port.in.RefreshTokenCommand;
import it.robfrank.linklift.application.port.in.RefreshTokenUseCase;
import it.robfrank.linklift.application.port.out.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.jspecify.annotations.NonNull;

/**
 * Domain service implementing authentication use cases.
 * Handles user login, token generation, and token refresh.
 */
public class AuthenticationService implements AuthenticateUserUseCase, RefreshTokenUseCase {

  private static final long ACCESS_TOKEN_EXPIRY_MINUTES = 15; // 15 minutes
  private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7; // 7 days
  private static final long REMEMBER_ME_REFRESH_TOKEN_EXPIRY_DAYS = 30; // 30 days

  private final LoadUserPort loadUserPort;
  private final SaveUserPort saveUserPort;
  private final PasswordSecurityPort passwordSecurityPort;
  private final JwtTokenPort jwtTokenPort;
  private final AuthTokenPort authTokenPort;
  private final DomainEventPublisher eventPublisher;

  public AuthenticationService(
    @NonNull LoadUserPort loadUserPort,
    @NonNull SaveUserPort saveUserPort,
    @NonNull PasswordSecurityPort passwordSecurityPort,
    @NonNull JwtTokenPort jwtTokenPort,
    @NonNull AuthTokenPort authTokenPort,
    @NonNull DomainEventPublisher eventPublisher
  ) {
    this.loadUserPort = loadUserPort;
    this.saveUserPort = saveUserPort;
    this.passwordSecurityPort = passwordSecurityPort;
    this.jwtTokenPort = jwtTokenPort;
    this.authTokenPort = authTokenPort;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public AuthenticateUserUseCase.@NonNull AuthenticationResult authenticate(@NonNull AuthenticateUserCommand command) {
    // Find user by username or email
    var user = findUserForAuthentication(command);

    // Verify password
    verifyPassword(command.password(), user);

    // Check if user is active
    if (!user.isActive()) {
      throw new AuthenticationException("User account is inactive", ErrorCode.USER_INACTIVE);
    }

    // Update last login
    var updatedUser = user.withLastLogin(LocalDateTime.now());
    saveUserPort.updateUser(updatedUser);

    // Generate tokens
    var tokens = generateTokens(updatedUser, command.ipAddress(), command.userAgent(), command.rememberMe());

    // Publish domain event
    eventPublisher.publish(new UserAuthenticatedEvent(user.id(), user.username(), command.ipAddress(), command.userAgent(), LocalDateTime.now()));

    return new AuthenticateUserUseCase.AuthenticationResult(
      updatedUser.id(),
      updatedUser.username(),
      updatedUser.email(),
      updatedUser.firstName(),
      updatedUser.lastName(),
      tokens.accessToken(),
      tokens.refreshToken(),
      ACCESS_TOKEN_EXPIRY_MINUTES * 60, // convert to seconds
      getRefreshTokenExpirySeconds(command.rememberMe())
    );
  }

  @Override
  public AuthenticateUserUseCase.@NonNull AuthenticationResult refreshToken(@NonNull RefreshTokenCommand command) {
    // Validate refresh token JWT
    var tokenClaims = jwtTokenPort.validateToken(command.refreshToken()).orElseThrow(AuthenticationException::tokenInvalid);

    // Find stored refresh token
    var storedToken = authTokenPort.findByToken(command.refreshToken()).orElseThrow(AuthenticationException::tokenInvalid);

    // Verify token is valid and not revoked
    if (!storedToken.isValid() || storedToken.tokenType() != AuthToken.TokenType.REFRESH) {
      throw AuthenticationException.tokenInvalid();
    }

    // Find user
    var user = loadUserPort.findUserById(storedToken.userId()).orElseThrow(AuthenticationException::tokenInvalid);

    // Check if user is still active
    if (!user.isActive()) {
      throw new AuthenticationException("User account is inactive", ErrorCode.USER_INACTIVE);
    }

    // Mark old refresh token as used
    authTokenPort.markTokenAsUsed(storedToken.id());

    // Generate new tokens
    var tokens = generateTokens(user, command.ipAddress(), command.userAgent(), false);

    // Publish domain event
    eventPublisher.publish(new TokenRefreshedEvent(user.id(), user.username(), command.ipAddress(), LocalDateTime.now()));

    return new AuthenticateUserUseCase.AuthenticationResult(
      user.id(),
      user.username(),
      user.email(),
      user.firstName(),
      user.lastName(),
      tokens.accessToken(),
      tokens.refreshToken(),
      ACCESS_TOKEN_EXPIRY_MINUTES * 60,
      getRefreshTokenExpirySeconds(false) // Default to non-remember-me
    );
  }

  private @NonNull User findUserForAuthentication(@NonNull AuthenticateUserCommand command) {
    if (command.isEmailLogin()) {
      return loadUserPort.findUserByEmail(command.loginIdentifier()).orElseThrow(() -> AuthenticationException.invalidCredentials());
    } else {
      return loadUserPort.findUserByUsername(command.loginIdentifier()).orElseThrow(() -> AuthenticationException.invalidCredentials());
    }
  }

  private void verifyPassword(@NonNull String plainPassword, @NonNull User user) {
    boolean isValid = passwordSecurityPort.verifyPassword(plainPassword, user.passwordHash(), user.salt());

    if (!isValid) {
      throw AuthenticationException.invalidCredentials();
    }
  }

  private @NonNull TokenPair generateTokens(@NonNull User user, @NonNull String ipAddress, @NonNull String userAgent, boolean rememberMe) {
    var now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    var accessTokenExpiry = now.plusMinutes(ACCESS_TOKEN_EXPIRY_MINUTES);
    var refreshTokenExpiry = now.plusDays(rememberMe ? REMEMBER_ME_REFRESH_TOKEN_EXPIRY_DAYS : REFRESH_TOKEN_EXPIRY_DAYS);

    // Generate JWT tokens
    var accessToken = jwtTokenPort.generateAccessToken(user, accessTokenExpiry);
    var refreshToken = jwtTokenPort.generateRefreshToken(user, refreshTokenExpiry);

    // Store refresh token in database for revocation capabilities
    var storedRefreshToken = new AuthToken(
      UUID.randomUUID().toString(),
      refreshToken,
      AuthToken.TokenType.REFRESH,
      user.id(),
      now,
      refreshTokenExpiry,
      null, // usedAt
      false, // isRevoked
      ipAddress,
      userAgent
    );

    authTokenPort.saveToken(storedRefreshToken);

    return new TokenPair(accessToken, refreshToken);
  }

  private long getRefreshTokenExpirySeconds(boolean rememberMe) {
    long days = rememberMe ? REMEMBER_ME_REFRESH_TOKEN_EXPIRY_DAYS : REFRESH_TOKEN_EXPIRY_DAYS;
    return days * 24 * 60 * 60; // Convert to seconds
  }

  private record TokenPair(@NonNull String accessToken, @NonNull String refreshToken) {}

  /**
   * Domain event published when a user successfully authenticates.
   */
  public record UserAuthenticatedEvent(String userId, String username, String ipAddress, String userAgent, LocalDateTime timestamp)
    implements it.robfrank.linklift.application.domain.event.DomainEvent {
    public String getEventType() {
      return "USER_AUTHENTICATED";
    }
  }

  /**
   * Domain event published when a token is refreshed.
   */
  public record TokenRefreshedEvent(String userId, String username, String ipAddress, LocalDateTime timestamp)
    implements it.robfrank.linklift.application.domain.event.DomainEvent {
    public String getEventType() {
      return "TOKEN_REFRESHED";
    }
  }
}
