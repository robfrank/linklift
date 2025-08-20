package it.robfrank.linklift.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SecurityContextTest {

  @Test
  void anonymous_shouldCreateUnauthenticatedContext() {
    // Act
    SecurityContext context = SecurityContext.anonymous();

    // Assert
    assertThat(context.isAuthenticated()).isFalse();
    assertThat(context.userId()).isNull();
    assertThat(context.username()).isNull();
    assertThat(context.email()).isNull();
    assertThat(context.permissions()).isEmpty();
    assertThat(context.authenticatedAt()).isNull();
    assertThat(context.ipAddress()).isNull();
    assertThat(context.userAgent()).isNull();
  }

  @Test
  void authenticated_shouldCreateAuthenticatedContext() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.CREATE_LINK, Role.Permissions.READ_OWN_LINKS);
    String ipAddress = "192.168.1.1";
    String userAgent = "Test-Agent";

    // Act
    SecurityContext context = SecurityContext.authenticated(user, permissions, ipAddress, userAgent);

    // Assert
    assertThat(context.isAuthenticated()).isTrue();
    assertThat(context.userId()).isEqualTo(user.id());
    assertThat(context.username()).isEqualTo(user.username());
    assertThat(context.email()).isEqualTo(user.email());
    assertThat(context.permissions()).isEqualTo(permissions);
    assertThat(context.authenticatedAt()).isNotNull();
    assertThat(context.ipAddress()).isEqualTo(ipAddress);
    assertThat(context.userAgent()).isEqualTo(userAgent);
  }

  @Test
  void authenticated_shouldHandleNullPermissions() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);

    // Act
    SecurityContext context = SecurityContext.authenticated(user, null, "ip", "agent");

    // Assert
    assertThat(context.permissions()).isEmpty();
    assertThat(context.isAuthenticated()).isTrue();
  }

  @Test
  void hasPermission_shouldReturnTrue_whenUserHasPermission() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.CREATE_LINK, Role.Permissions.READ_OWN_LINKS);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "ip", "agent");

    // Act & Assert
    assertThat(context.hasPermission(Role.Permissions.CREATE_LINK)).isTrue();
    assertThat(context.hasPermission(Role.Permissions.READ_OWN_LINKS)).isTrue();
  }

  @Test
  void hasPermission_shouldReturnFalse_whenUserDoesNotHavePermission() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.CREATE_LINK);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "ip", "agent");

    // Act & Assert
    assertThat(context.hasPermission(Role.Permissions.ADMIN_ACCESS)).isFalse();
  }

  @Test
  void hasPermission_shouldReturnFalse_whenNotAuthenticated() {
    // Arrange
    SecurityContext context = SecurityContext.anonymous();

    // Act & Assert
    assertThat(context.hasPermission(Role.Permissions.CREATE_LINK)).isFalse();
  }

  @Test
  void hasAnyPermission_shouldReturnTrue_whenUserHasAtLeastOnePermission() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.CREATE_LINK);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "ip", "agent");

    // Act & Assert
    assertThat(context.hasAnyPermission(Role.Permissions.CREATE_LINK, Role.Permissions.ADMIN_ACCESS)).isTrue();
  }

  @Test
  void hasAnyPermission_shouldReturnFalse_whenUserHasNoneOfThePermissions() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.CREATE_LINK);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "ip", "agent");

    // Act & Assert
    assertThat(context.hasAnyPermission(Role.Permissions.ADMIN_ACCESS, Role.Permissions.SYSTEM_CONFIG)).isFalse();
  }

  @Test
  void hasAnyPermission_shouldReturnFalse_whenNotAuthenticated() {
    // Arrange
    SecurityContext context = SecurityContext.anonymous();

    // Act & Assert
    assertThat(context.hasAnyPermission(Role.Permissions.CREATE_LINK, Role.Permissions.ADMIN_ACCESS)).isFalse();
  }

  @Test
  void hasAllPermissions_shouldReturnTrue_whenUserHasAllPermissions() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.CREATE_LINK, Role.Permissions.READ_OWN_LINKS, Role.Permissions.ADMIN_ACCESS);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "ip", "agent");

    // Act & Assert
    assertThat(context.hasAllPermissions(Role.Permissions.CREATE_LINK, Role.Permissions.READ_OWN_LINKS)).isTrue();
  }

  @Test
  void hasAllPermissions_shouldReturnFalse_whenUserIsMissingOnePermission() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.CREATE_LINK);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "ip", "agent");

    // Act & Assert
    assertThat(context.hasAllPermissions(Role.Permissions.CREATE_LINK, Role.Permissions.ADMIN_ACCESS)).isFalse();
  }

  @Test
  void hasAllPermissions_shouldReturnFalse_whenNotAuthenticated() {
    // Arrange
    SecurityContext context = SecurityContext.anonymous();

    // Act & Assert
    assertThat(context.hasAllPermissions(Role.Permissions.CREATE_LINK)).isFalse();
  }

  @Test
  void isOwner_shouldReturnTrue_whenUserOwnsResource() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    SecurityContext context = SecurityContext.authenticated(user, List.of(), "ip", "agent");

    // Act & Assert
    assertThat(context.isOwner("user-123")).isTrue();
  }

  @Test
  void isOwner_shouldReturnFalse_whenUserDoesNotOwnResource() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    SecurityContext context = SecurityContext.authenticated(user, List.of(), "ip", "agent");

    // Act & Assert
    assertThat(context.isOwner("other-user-id")).isFalse();
  }

  @Test
  void isOwner_shouldReturnFalse_whenNotAuthenticated() {
    // Arrange
    SecurityContext context = SecurityContext.anonymous();

    // Act & Assert
    assertThat(context.isOwner("user-123")).isFalse();
  }

  @Test
  void canAccess_shouldReturnTrue_whenUserOwnsResource() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    SecurityContext context = SecurityContext.authenticated(user, List.of(), "ip", "agent");

    // Act & Assert
    assertThat(context.canAccess("user-123", Role.Permissions.ADMIN_ACCESS)).isTrue();
  }

  @Test
  void canAccess_shouldReturnTrue_whenUserHasRequiredPermission() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.READ_ALL_LINKS);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "ip", "agent");

    // Act & Assert
    assertThat(context.canAccess("other-user-id", Role.Permissions.READ_ALL_LINKS)).isTrue();
  }

  @Test
  void canAccess_shouldReturnFalse_whenUserDoesNotOwnResourceAndLacksPermission() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.CREATE_LINK);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "ip", "agent");

    // Act & Assert
    assertThat(context.canAccess("other-user-id", Role.Permissions.READ_ALL_LINKS)).isFalse();
  }

  @Test
  void canAccess_shouldReturnFalse_whenNotAuthenticated() {
    // Arrange
    SecurityContext context = SecurityContext.anonymous();

    // Act & Assert
    assertThat(context.canAccess("user-123", Role.Permissions.CREATE_LINK)).isFalse();
  }

  @Test
  void getCurrentUserId_shouldReturnUserIdWhenAuthenticated() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    SecurityContext context = SecurityContext.authenticated(user, List.of(), "ip", "agent");

    // Act
    Optional<String> userId = context.getCurrentUserId();

    // Assert
    assertThat(userId).isPresent();
    assertThat(userId.get()).isEqualTo("user-123");
  }

  @Test
  void getCurrentUserId_shouldReturnEmptyWhenNotAuthenticated() {
    // Arrange
    SecurityContext context = SecurityContext.anonymous();

    // Act
    Optional<String> userId = context.getCurrentUserId();

    // Assert
    assertThat(userId).isEmpty();
  }

  @Test
  void getCurrentUsername_shouldReturnUsernameWhenAuthenticated() {
    // Arrange
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    SecurityContext context = SecurityContext.authenticated(user, List.of(), "ip", "agent");

    // Act
    Optional<String> username = context.getCurrentUsername();

    // Assert
    assertThat(username).isPresent();
    assertThat(username.get()).isEqualTo("testuser");
  }

  @Test
  void getCurrentUsername_shouldReturnEmptyWhenNotAuthenticated() {
    // Arrange
    SecurityContext context = SecurityContext.anonymous();

    // Act
    Optional<String> username = context.getCurrentUsername();

    // Assert
    assertThat(username).isEmpty();
  }
}
