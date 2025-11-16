package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.Role;
import java.util.List;

/**
 * Port interface for loading user roles and permissions.
 */
public interface LoadUserRolesPort {

    /**
     * Gets all roles assigned to a user.
     *
     * @param userId the user ID
     * @return list of roles assigned to the user
     */
    List<Role> getUserRoles(String userId);

    /**
     * Gets all permissions for a user (aggregated from all their roles).
     *
     * @param userId the user ID
     * @return list of permissions the user has
     */
    List<String> getUserPermissions(String userId);

    /**
     * Checks if a user has a specific permission.
     *
     * @param userId the user ID
     * @param permission the permission to check
     * @return true if user has the permission, false otherwise
     */
    boolean userHasPermission(String userId, String permission);

    /**
     * Assigns a role to a user.
     *
     * @param userId the user ID
     * @param roleId the role ID
     */
    void assignRoleToUser(String userId, String roleId);

    /**
     * Removes a role from a user.
     *
     * @param userId the user ID
     * @param roleId the role ID
     */
    void removeRoleFromUser(String userId, String roleId);
}
