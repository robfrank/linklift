package it.robfrank.linklift.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RoleTest {

  @Test
  void constructor_shouldSetAllPropertiesCorrectly() {
    // Arrange
    String id = "role-123";
    String name = "USER";
    String description = "Standard user role";
    List<String> permissions = List.of(Role.Permissions.CREATE_LINK, Role.Permissions.READ_OWN_LINKS);
    boolean isActive = true;

    // Act
    Role role = new Role(id, name, description, permissions, isActive);

    // Assert
    assertThat(role.id()).isEqualTo(id);
    assertThat(role.name()).isEqualTo(name);
    assertThat(role.description()).isEqualTo(description);
    assertThat(role.permissions()).isEqualTo(permissions);
    assertThat(role.isActive()).isEqualTo(isActive);
  }

  @Test
  void hasPermission_shouldReturnTrue_whenRoleHasPermission() {
    // Arrange
    Role role = new Role("id", "USER", "desc", List.of(Role.Permissions.CREATE_LINK, Role.Permissions.READ_OWN_LINKS), true);

    // Act & Assert
    assertThat(role.hasPermission(Role.Permissions.CREATE_LINK)).isTrue();
    assertThat(role.hasPermission(Role.Permissions.READ_OWN_LINKS)).isTrue();
  }

  @Test
  void hasPermission_shouldReturnFalse_whenRoleDoesNotHavePermission() {
    // Arrange
    Role role = new Role("id", "USER", "desc", List.of(Role.Permissions.CREATE_LINK), true);

    // Act & Assert
    assertThat(role.hasPermission(Role.Permissions.ADMIN_ACCESS)).isFalse();
  }

  @Test
  void hasPermission_shouldReturnFalse_whenPermissionsIsNull() {
    // Arrange
    Role role = new Role("id", "USER", "desc", null, true);

    // Act & Assert
    assertThat(role.hasPermission(Role.Permissions.CREATE_LINK)).isFalse();
  }

  @Test
  void hasAnyPermission_shouldReturnTrue_whenRoleHasAtLeastOnePermission() {
    // Arrange
    Role role = new Role("id", "USER", "desc", List.of(Role.Permissions.CREATE_LINK), true);

    // Act & Assert
    assertThat(role.hasAnyPermission(Role.Permissions.CREATE_LINK, Role.Permissions.ADMIN_ACCESS)).isTrue();
  }

  @Test
  void hasAnyPermission_shouldReturnFalse_whenRoleHasNoneOfThePermissions() {
    // Arrange
    Role role = new Role("id", "USER", "desc", List.of(Role.Permissions.CREATE_LINK), true);

    // Act & Assert
    assertThat(role.hasAnyPermission(Role.Permissions.ADMIN_ACCESS, Role.Permissions.SYSTEM_CONFIG)).isFalse();
  }

  @Test
  void hasAnyPermission_shouldReturnFalse_whenPermissionsIsNull() {
    // Arrange
    Role role = new Role("id", "USER", "desc", null, true);

    // Act & Assert
    assertThat(role.hasAnyPermission(Role.Permissions.CREATE_LINK, Role.Permissions.ADMIN_ACCESS)).isFalse();
  }

  @Test
  void hasAllPermissions_shouldReturnTrue_whenRoleHasAllPermissions() {
    // Arrange
    Role role = new Role("id", "ADMIN", "desc", List.of(Role.Permissions.CREATE_LINK, Role.Permissions.READ_OWN_LINKS, Role.Permissions.ADMIN_ACCESS), true);

    // Act & Assert
    assertThat(role.hasAllPermissions(Role.Permissions.CREATE_LINK, Role.Permissions.READ_OWN_LINKS)).isTrue();
  }

  @Test
  void hasAllPermissions_shouldReturnFalse_whenRoleIsMissingOnePermission() {
    // Arrange
    Role role = new Role("id", "USER", "desc", List.of(Role.Permissions.CREATE_LINK), true);

    // Act & Assert
    assertThat(role.hasAllPermissions(Role.Permissions.CREATE_LINK, Role.Permissions.ADMIN_ACCESS)).isFalse();
  }

  @Test
  void hasAllPermissions_shouldReturnFalse_whenPermissionsIsNull() {
    // Arrange
    Role role = new Role("id", "USER", "desc", null, true);

    // Act & Assert
    assertThat(role.hasAllPermissions(Role.Permissions.CREATE_LINK)).isFalse();
  }

  @Test
  void withAdditionalPermissions_shouldAddNewPermissions() {
    // Arrange
    Role originalRole = new Role("id", "USER", "desc", List.of(Role.Permissions.CREATE_LINK), true);
    List<String> additionalPermissions = List.of(Role.Permissions.READ_OWN_LINKS, Role.Permissions.UPDATE_OWN_LINKS);

    // Act
    Role updatedRole = originalRole.withAdditionalPermissions(additionalPermissions);

    // Assert
    assertThat(updatedRole.permissions()).contains(Role.Permissions.CREATE_LINK, Role.Permissions.READ_OWN_LINKS, Role.Permissions.UPDATE_OWN_LINKS);
    assertThat(updatedRole.permissions()).hasSize(3);
    assertThat(updatedRole.id()).isEqualTo(originalRole.id());
    assertThat(updatedRole.name()).isEqualTo(originalRole.name());
  }

  @Test
  void withAdditionalPermissions_shouldHandleNullOriginalPermissions() {
    // Arrange
    Role originalRole = new Role("id", "USER", "desc", null, true);
    List<String> additionalPermissions = List.of(Role.Permissions.CREATE_LINK, Role.Permissions.READ_OWN_LINKS);

    // Act
    Role updatedRole = originalRole.withAdditionalPermissions(additionalPermissions);

    // Assert
    assertThat(updatedRole.permissions()).isEqualTo(additionalPermissions);
  }

  @Test
  void withAdditionalPermissions_shouldRemoveDuplicates() {
    // Arrange
    Role originalRole = new Role("id", "USER", "desc", List.of(Role.Permissions.CREATE_LINK), true);
    List<String> additionalPermissions = List.of(
      Role.Permissions.CREATE_LINK, // Duplicate
      Role.Permissions.READ_OWN_LINKS
    );

    // Act
    Role updatedRole = originalRole.withAdditionalPermissions(additionalPermissions);

    // Assert
    assertThat(updatedRole.permissions()).contains(Role.Permissions.CREATE_LINK, Role.Permissions.READ_OWN_LINKS);
    assertThat(updatedRole.permissions()).hasSize(2); // No duplicates
  }

  @Test
  void withActiveStatus_shouldUpdateActiveStatus() {
    // Arrange
    Role originalRole = new Role("id", "USER", "desc", List.of(Role.Permissions.CREATE_LINK), true);

    // Act
    Role deactivatedRole = originalRole.withActiveStatus(false);

    // Assert
    assertThat(deactivatedRole.isActive()).isFalse();
    assertThat(deactivatedRole.id()).isEqualTo(originalRole.id());
    assertThat(deactivatedRole.name()).isEqualTo(originalRole.name());
    assertThat(deactivatedRole.permissions()).isEqualTo(originalRole.permissions());
  }

  @Test
  void permissions_constantsExist() {
    // Assert that all expected permission constants exist
    assertThat(Role.Permissions.CREATE_LINK).isEqualTo("CREATE_LINK");
    assertThat(Role.Permissions.READ_OWN_LINKS).isEqualTo("READ_OWN_LINKS");
    assertThat(Role.Permissions.READ_ALL_LINKS).isEqualTo("READ_ALL_LINKS");
    assertThat(Role.Permissions.UPDATE_OWN_LINKS).isEqualTo("UPDATE_OWN_LINKS");
    assertThat(Role.Permissions.UPDATE_ALL_LINKS).isEqualTo("UPDATE_ALL_LINKS");
    assertThat(Role.Permissions.DELETE_OWN_LINKS).isEqualTo("DELETE_OWN_LINKS");
    assertThat(Role.Permissions.DELETE_ALL_LINKS).isEqualTo("DELETE_ALL_LINKS");
    assertThat(Role.Permissions.MANAGE_USERS).isEqualTo("MANAGE_USERS");
    assertThat(Role.Permissions.VIEW_USERS).isEqualTo("VIEW_USERS");
    assertThat(Role.Permissions.MANAGE_ROLES).isEqualTo("MANAGE_ROLES");
    assertThat(Role.Permissions.ADMIN_ACCESS).isEqualTo("ADMIN_ACCESS");
    assertThat(Role.Permissions.SYSTEM_CONFIG).isEqualTo("SYSTEM_CONFIG");
  }

  @Test
  void roleNames_constantsExist() {
    // Assert that all expected role name constants exist
    assertThat(Role.RoleNames.USER).isEqualTo("USER");
    assertThat(Role.RoleNames.ADMIN).isEqualTo("ADMIN");
    assertThat(Role.RoleNames.MODERATOR).isEqualTo("MODERATOR");
  }
}
