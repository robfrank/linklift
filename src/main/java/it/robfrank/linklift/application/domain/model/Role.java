package it.robfrank.linklift.application.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Role domain model representing user roles and permissions in the system.
 */
public record Role(
  @JsonProperty("id") String id,
  @JsonProperty("name") String name,
  @JsonProperty("description") String description,
  @JsonProperty("permissions") List<String> permissions,
  @JsonProperty("isActive") boolean isActive
) {
  /**
   * Permission constants for the LinkLift system.
   */
  public static class Permissions {

    // Link management permissions
    public static final String CREATE_LINK = "CREATE_LINK";
    public static final String READ_OWN_LINKS = "READ_OWN_LINKS";
    public static final String READ_ALL_LINKS = "READ_ALL_LINKS";
    public static final String UPDATE_OWN_LINKS = "UPDATE_OWN_LINKS";
    public static final String UPDATE_ALL_LINKS = "UPDATE_ALL_LINKS";
    public static final String DELETE_OWN_LINKS = "DELETE_OWN_LINKS";
    public static final String DELETE_ALL_LINKS = "DELETE_ALL_LINKS";

    // User management permissions
    public static final String MANAGE_USERS = "MANAGE_USERS";
    public static final String VIEW_USERS = "VIEW_USERS";
    public static final String MANAGE_ROLES = "MANAGE_ROLES";

    // System permissions
    public static final String ADMIN_ACCESS = "ADMIN_ACCESS";
    public static final String SYSTEM_CONFIG = "SYSTEM_CONFIG";
  }

  /**
   * Predefined role constants.
   */
  public static class RoleNames {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";
    public static final String MODERATOR = "MODERATOR";
  }

  /**
   * Checks if this role has a specific permission.
   */
  public boolean hasPermission(String permission) {
    return permissions != null && permissions.contains(permission);
  }

  /**
   * Checks if this role has any of the specified permissions.
   */
  public boolean hasAnyPermission(String... permissionsToCheck) {
    if (permissions == null) {
      return false;
    }
    for (String permission : permissionsToCheck) {
      if (permissions.contains(permission)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if this role has all of the specified permissions.
   */
  public boolean hasAllPermissions(String... permissionsToCheck) {
    if (permissions == null) {
      return false;
    }
    for (String permission : permissionsToCheck) {
      if (!permissions.contains(permission)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Creates a new Role instance with additional permissions.
   */
  public Role withAdditionalPermissions(List<String> additionalPermissions) {
    List<String> newPermissions = permissions == null
      ? List.copyOf(additionalPermissions)
      : List.of(permissions, additionalPermissions).stream().flatMap(List::stream).distinct().toList();

    return new Role(id, name, description, newPermissions, isActive);
  }

  /**
   * Creates a new Role instance with updated active status.
   */
  public Role withActiveStatus(boolean isActive) {
    return new Role(id, name, description, permissions, isActive);
  }
}
