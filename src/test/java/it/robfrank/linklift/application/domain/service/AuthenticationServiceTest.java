package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.model.AuthToken;
import it.robfrank.linklift.application.domain.model.User;
import it.robfrank.linklift.application.port.in.AuthenticateUserCommand;
import it.robfrank.linklift.application.port.out.AuthTokenPort;
import it.robfrank.linklift.application.port.out.DomainEventPublisher;
import it.robfrank.linklift.application.port.out.JwtTokenPort;
import it.robfrank.linklift.application.port.out.LoadUserPort;
import it.robfrank.linklift.application.port.out.PasswordSecurityPort;
import it.robfrank.linklift.application.port.out.SaveUserPort;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

  @Mock
  private LoadUserPort loadUserPort;

  @Mock
  private SaveUserPort saveUserPort;

  @Mock
  private PasswordSecurityPort passwordSecurityPort;

  @Mock
  private JwtTokenPort jwtTokenPort;

  @Mock
  private AuthTokenPort authTokenPort;

  @Mock
  private DomainEventPublisher eventPublisher;

  private AuthenticationService authenticationService;

  @BeforeEach
  void setUp() {
    authenticationService = new AuthenticationService(loadUserPort, saveUserPort, passwordSecurityPort, jwtTokenPort, authTokenPort, eventPublisher);
  }

  @Test
  void authenticate_shouldSucceed_whenValidCredentials() {
    // Given
    var command = new AuthenticateUserCommand("testuser", "password123", "192.168.1.1", "Test-Agent", false);

    var user = new User("user-id", "testuser", "test@example.com", "hashed-password", "salt", LocalDateTime.now(), null, true, "Test", "User", null);

    when(loadUserPort.findUserByUsername("testuser")).thenReturn(Optional.of(user));
    when(passwordSecurityPort.verifyPassword("password123", "hashed-password", "salt")).thenReturn(true);
    when(saveUserPort.updateUser(any(User.class))).thenReturn(user.withLastLogin(LocalDateTime.now()));
    when(jwtTokenPort.generateAccessToken(any(User.class), any(LocalDateTime.class))).thenReturn("access-token");
    when(jwtTokenPort.generateRefreshToken(any(User.class), any(LocalDateTime.class))).thenReturn("refresh-token");
    when(authTokenPort.saveToken(any(AuthToken.class))).thenReturn(mock(AuthToken.class));

    // When
    var result = authenticationService.authenticate(command);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.userId()).isEqualTo("user-id");
    assertThat(result.username()).isEqualTo("testuser");
    assertThat(result.accessToken()).isEqualTo("access-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");

    verify(eventPublisher).publish(any(AuthenticationService.UserAuthenticatedEvent.class));
  }

  @Test
  void authenticate_shouldFail_whenUserNotFound() {
    // Given
    var command = new AuthenticateUserCommand("nonexistent", "password123", "192.168.1.1", "Test-Agent", false);

    when(loadUserPort.findUserByUsername("nonexistent")).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> authenticationService.authenticate(command))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage("Invalid username or password");
  }

  @Test
  void authenticate_shouldFail_whenInvalidPassword() {
    // Given
    var command = new AuthenticateUserCommand("testuser", "wrongpassword", "192.168.1.1", "Test-Agent", false);

    var user = new User("user-id", "testuser", "test@example.com", "hashed-password", "salt", LocalDateTime.now(), null, true, "Test", "User", null);

    when(loadUserPort.findUserByUsername("testuser")).thenReturn(Optional.of(user));
    when(passwordSecurityPort.verifyPassword("wrongpassword", "hashed-password", "salt")).thenReturn(false);

    // When & Then
    assertThatThrownBy(() -> authenticationService.authenticate(command))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage("Invalid username or password");
  }

  @Test
  void authenticate_shouldFail_whenUserInactive() {
    // Given
    var command = new AuthenticateUserCommand("testuser", "password123", "192.168.1.1", "Test-Agent", false);

    var inactiveUser = new User(
      "user-id",
      "testuser",
      "test@example.com",
      "hashed-password",
      "salt",
      LocalDateTime.now(),
      null,
      false, // inactive
      "Test",
      "User",
      null
    );

    when(loadUserPort.findUserByUsername("testuser")).thenReturn(Optional.of(inactiveUser));
    when(passwordSecurityPort.verifyPassword("password123", "hashed-password", "salt")).thenReturn(true);

    // When & Then
    assertThatThrownBy(() -> authenticationService.authenticate(command)).isInstanceOf(AuthenticationException.class).hasMessage("User account is inactive");
  }
}
