package it.robfrank.linklift.adapter.out.persitence;

import it.robfrank.linklift.application.domain.model.Role;
import it.robfrank.linklift.application.port.out.LoadUserRolesPort;
import java.util.List;

/**
 * Persistence adapter for user roles and permissions.
 * For now, provides default permissions to all users.
 */
public class UserRolePersistenceAdapter implements LoadUserRolesPort {

  @Override
  public List<Role> getUserRoles(String userId) {
    // For now, return a default user role for all users
    return List.of(createDefaultUserRole());
  }

  @Override
  public List<String> getUserPermissions(String userId) {
    // Return default permissions for authenticated users
    return List.of(
      Role.Permissions.CREATE_LINK,
      Role.Permissions.CREATE_COLLECTION,
      Role.Permissions.READ_OWN_LINKS,
      Role.Permissions.UPDATE_OWN_LINKS,
      Role.Permissions.DELETE_OWN_LINKS
    );
  }

  @Override
  public boolean userHasPermission(String userId, String permission) {
    return getUserPermissions(userId).contains(permission);
  }

  @Override
  public void assignRoleToUser(String userId, String roleId) {
    // TODO: Implement role assignment when role management is added
    throw new UnsupportedOperationException("Role assignment not yet implemented");
  }

  @Override
  public void removeRoleFromUser(String userId, String roleId) {
    // TODO: Implement role removal when role management is added
    throw new UnsupportedOperationException("Role removal not yet implemented");
  }

  private Role createDefaultUserRole() {
    return new Role(
      "default-user-role",
      "Default User",
      "Default role for authenticated users",
      List.of(
        Role.Permissions.CREATE_LINK,
        Role.Permissions.CREATE_COLLECTION,
        Role.Permissions.READ_OWN_LINKS,
        Role.Permissions.UPDATE_OWN_LINKS,
        Role.Permissions.DELETE_OWN_LINKS
      ),
      true // isActive
    );
  }
}
