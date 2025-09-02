package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.model.Role;
import it.robfrank.linklift.application.domain.model.SecurityContext;
import it.robfrank.linklift.application.domain.model.User;
import it.robfrank.linklift.application.port.out.JwtTokenPort;
import it.robfrank.linklift.application.port.out.LoadUserPort;
import it.robfrank.linklift.application.port.out.LoadUserRolesPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

  @Mock
  private JwtTokenPort jwtTokenPort;

  @Mock
  private LoadUserPort loadUserPort;

  @Mock
  private LoadUserRolesPort loadUserRolesPort;

  private AuthorizationService authorizationService;

  @BeforeEach
  void setUp() {
    authorizationService = new AuthorizationService(jwtTokenPort, loadUserPort, loadUserRolesPort);
  }

  @Test
  void createSecurityContext_shouldReturnAuthenticatedContext_whenValidToken() {
    // Arrange
    String token = "valid-jwt-token";
    String ipAddress = "192.168.1.1";
    String userAgent = "Test-Agent";
    String userId = "user-123";

    JwtTokenPort.TokenClaims tokenClaims = new JwtTokenPort.TokenClaims(
      userId,
      "testuser",
      "test@example.com",
      LocalDateTime.now(),
      LocalDateTime.now().plusHours(1),
      "access",
      Map.of()
    );

    User user = new User(userId, "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);

    List<String> permissions = List.of(Role.Permissions.CREATE_LINK, Role.Permissions.READ_OWN_LINKS);

    when(jwtTokenPort.validateToken(token)).thenReturn(Optional.of(tokenClaims));
    when(loadUserPort.findUserById(userId)).thenReturn(Optional.of(user));
    when(loadUserRolesPort.getUserPermissions(userId)).thenReturn(permissions);

    // Act
    SecurityContext context = authorizationService.createSecurityContext(token, ipAddress, userAgent);

    // Assert
    assertThat(context.isAuthenticated()).isTrue();
    assertThat(context.userId()).isEqualTo(userId);
    assertThat(context.username()).isEqualTo("testuser");
    assertThat(context.email()).isEqualTo("test@example.com");
    assertThat(context.permissions()).isEqualTo(permissions);
    assertThat(context.ipAddress()).isEqualTo(ipAddress);
    assertThat(context.userAgent()).isEqualTo(userAgent);

    verify(jwtTokenPort).validateToken(token);
    verify(loadUserPort).findUserById(userId);
    verify(loadUserRolesPort).getUserPermissions(userId);
  }

  @Test
  void createSecurityContext_shouldReturnAnonymousContext_whenTokenIsNull() {
    // Act
    SecurityContext context = authorizationService.createSecurityContext(null, "ip", "agent");

    // Assert
    assertThat(context.isAuthenticated()).isFalse();
    verify(jwtTokenPort, never()).validateToken(anyString());
  }

  @Test
  void createSecurityContext_shouldReturnAnonymousContext_whenTokenIsEmpty() {
    // Act
    SecurityContext context = authorizationService.createSecurityContext("", "ip", "agent");

    // Assert
    assertThat(context.isAuthenticated()).isFalse();
    verify(jwtTokenPort, never()).validateToken(anyString());
  }

  @Test
  void createSecurityContext_shouldReturnAnonymousContext_whenTokenIsInvalid() {
    // Arrange
    String invalidToken = "invalid-token";

    when(jwtTokenPort.validateToken(invalidToken)).thenReturn(Optional.empty());

    // Act
    SecurityContext context = authorizationService.createSecurityContext(invalidToken, "ip", "agent");

    // Assert
    assertThat(context.isAuthenticated()).isFalse();
    verify(jwtTokenPort).validateToken(invalidToken);
    verify(loadUserPort, never()).findUserById(anyString());
  }

  @Test
  void createSecurityContext_shouldReturnAnonymousContext_whenUserNotFound() {
    // Arrange
    String token = "valid-jwt-token";
    String userId = "user-123";

    JwtTokenPort.TokenClaims tokenClaims = new JwtTokenPort.TokenClaims(
      userId,
      "testuser",
      "test@example.com",
      LocalDateTime.now(),
      LocalDateTime.now().plusHours(1),
      "access",
      Map.of()
    );

    when(jwtTokenPort.validateToken(token)).thenReturn(Optional.of(tokenClaims));
    when(loadUserPort.findUserById(userId)).thenReturn(Optional.empty());

    // Act
    SecurityContext context = authorizationService.createSecurityContext(token, "ip", "agent");

    // Assert
    assertThat(context.isAuthenticated()).isFalse();
    verify(loadUserPort).findUserById(userId);
    verify(loadUserRolesPort, never()).getUserPermissions(anyString());
  }

  @Test
  void createSecurityContext_shouldReturnAnonymousContext_whenUserIsInactive() {
    // Arrange
    String token = "valid-jwt-token";
    String userId = "user-123";

    JwtTokenPort.TokenClaims tokenClaims = new JwtTokenPort.TokenClaims(
      userId,
      "testuser",
      "test@example.com",
      LocalDateTime.now(),
      LocalDateTime.now().plusHours(1),
      "access",
      Map.of()
    );

    User inactiveUser = new User(
      userId,
      "testuser",
      "test@example.com",
      "hash",
      "salt",
      LocalDateTime.now(),
      null,
      false, // inactive
      "John",
      "Doe",
      null
    );

    when(jwtTokenPort.validateToken(token)).thenReturn(Optional.of(tokenClaims));
    when(loadUserPort.findUserById(userId)).thenReturn(Optional.of(inactiveUser));

    // Act
    SecurityContext context = authorizationService.createSecurityContext(token, "ip", "agent");

    // Assert
    assertThat(context.isAuthenticated()).isFalse();
    verify(loadUserRolesPort, never()).getUserPermissions(anyString());
  }

  @Test
  void createSecurityContext_shouldReturnAnonymousContext_whenExceptionThrown() {
    // Arrange
    String token = "problematic-token";

    when(jwtTokenPort.validateToken(token)).thenThrow(new RuntimeException("Token validation error"));

    // Act
    SecurityContext context = authorizationService.createSecurityContext(token, "ip", "agent");

    // Assert
    assertThat(context.isAuthenticated()).isFalse();
  }

  @Test
  void requireAuthentication_shouldPass_whenUserIsAuthenticated() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    SecurityContext authenticatedContext = SecurityContext.authenticated(user, List.of(), "ip", "agent");

    // Act & Assert - should not throw
    authorizationService.requireAuthentication(authenticatedContext);
  }

  @Test
  void requireAuthentication_shouldThrowException_whenUserIsNotAuthenticated() {
    // Arrange
    SecurityContext anonymousContext = SecurityContext.anonymous();

    // Act & Assert
    assertThatThrownBy(() -> authorizationService.requireAuthentication(anonymousContext)).isInstanceOf(AuthenticationException.class);
  }

  @Test
  void requirePermission_shouldPass_whenUserHasPermission() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.CREATE_LINK);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "ip", "agent");

    // Act & Assert - should not throw
    authorizationService.requirePermission(context, Role.Permissions.CREATE_LINK);
  }

  @Test
  void requirePermission_shouldThrowException_whenUserLacksPermission() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.CREATE_LINK);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "ip", "agent");

    // Act & Assert
    assertThatThrownBy(() -> authorizationService.requirePermission(context, Role.Permissions.ADMIN_ACCESS)).isInstanceOf(AuthenticationException.class);
  }

  @Test
  void requirePermission_shouldThrowException_whenUserIsNotAuthenticated() {
    // Arrange
    SecurityContext anonymousContext = SecurityContext.anonymous();

    // Act & Assert
    assertThatThrownBy(() -> authorizationService.requirePermission(anonymousContext, Role.Permissions.CREATE_LINK)).isInstanceOf(
      AuthenticationException.class
    );
  }

  @Test
  void requireAnyPermission_shouldPass_whenUserHasOneOfThePermissions() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.CREATE_LINK);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "ip", "agent");

    // Act & Assert - should not throw
    authorizationService.requireAnyPermission(context, Role.Permissions.CREATE_LINK, Role.Permissions.ADMIN_ACCESS);
  }

  @Test
  void requireAnyPermission_shouldThrowException_whenUserHasNoneOfThePermissions() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.CREATE_LINK);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "ip", "agent");

    // Act & Assert
    assertThatThrownBy(() -> authorizationService.requireAnyPermission(context, Role.Permissions.ADMIN_ACCESS, Role.Permissions.SYSTEM_CONFIG)).isInstanceOf(
      AuthenticationException.class
    );
  }

  @Test
  void requireResourceAccess_shouldPass_whenUserOwnsResource() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    SecurityContext context = SecurityContext.authenticated(user, List.of(), "ip", "agent");

    // Act & Assert - should not throw
    authorizationService.requireResourceAccess(context, "user-123", Role.Permissions.READ_ALL_LINKS);
  }

  @Test
  void requireResourceAccess_shouldPass_whenUserHasAdminPermission() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.READ_ALL_LINKS);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "ip", "agent");

    // Act & Assert - should not throw
    authorizationService.requireResourceAccess(context, "other-user-id", Role.Permissions.READ_ALL_LINKS);
  }

  @Test
  void requireResourceAccess_shouldThrowException_whenUserCannotAccess() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.CREATE_LINK);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "ip", "agent");

    // Act & Assert
    assertThatThrownBy(() -> authorizationService.requireResourceAccess(context, "other-user-id", Role.Permissions.READ_ALL_LINKS)).isInstanceOf(
      AuthenticationException.class
    );
  }

  @Test
  void extractUserIdFromToken_shouldReturnUserId_whenTokenIsValid() {
    // Arrange
    String token = "valid-token";
    String expectedUserId = "user-123";

    when(jwtTokenPort.extractUserIdFromToken(token)).thenReturn(Optional.of(expectedUserId));

    // Act
    Optional<String> result = authorizationService.extractUserIdFromToken(token);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(expectedUserId);
    verify(jwtTokenPort).extractUserIdFromToken(token);
  }

  @Test
  void extractUserIdFromToken_shouldReturnEmpty_whenTokenIsInvalid() {
    // Arrange
    String token = "invalid-token";

    when(jwtTokenPort.extractUserIdFromToken(token)).thenReturn(Optional.empty());

    // Act
    Optional<String> result = authorizationService.extractUserIdFromToken(token);

    // Assert
    assertThat(result).isEmpty();
    verify(jwtTokenPort).extractUserIdFromToken(token);
  }
}
