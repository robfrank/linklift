package it.robfrank.linklift.adapter.out.persitence;

import it.robfrank.linklift.application.domain.model.Role;
import it.robfrank.linklift.application.port.out.LoadUserRolesPort;

import java.util.List;

/**
 * Persistence adapter for user roles operations.
 * For now, implements a simple role assignment strategy.
 */
public class UserRolesPersistenceAdapter implements LoadUserRolesPort {

    @Override
    public List<Role> getUserRoles(String userId) {
        // For now, assign default USER role to all users
        // In a real implementation, this would query the database
        return List.of(createDefaultUserRole());
    }

    @Override
    public List<String> getUserPermissions(String userId) {
        // For now, return default user permissions
        // In a real implementation, this would aggregate permissions from all user roles
        return List.of(
            Role.Permissions.CREATE_LINK,
            Role.Permissions.READ_OWN_LINKS,
            Role.Permissions.UPDATE_OWN_LINKS,
            Role.Permissions.DELETE_OWN_LINKS
        );
    }

    @Override
    public boolean userHasPermission(String userId, String permission) {
        var permissions = getUserPermissions(userId);
        return permissions.contains(permission);
    }

    @Override
    public void assignRoleToUser(String userId, String roleId) {
        // TODO: Implement role assignment in database
        throw new UnsupportedOperationException("Role assignment not yet implemented");
    }

    @Override
    public void removeRoleFromUser(String userId, String roleId) {
        // TODO: Implement role removal in database
        throw new UnsupportedOperationException("Role removal not yet implemented");
    }

    private Role createDefaultUserRole() {
        return new Role(
            "user-role",
            Role.RoleNames.USER,
            "Default user role with basic permissions",
            List.of(
                Role.Permissions.CREATE_LINK,
                Role.Permissions.READ_OWN_LINKS,
                Role.Permissions.UPDATE_OWN_LINKS,
                Role.Permissions.DELETE_OWN_LINKS
            ),
            true
        );
    }
}
