package it.robfrank.linklift.application.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Security context containing the current user's authentication and authorization information.
 * This is used throughout the application to access the current user's details and permissions.
 */
public record SecurityContext(
  String userId,
  String username,
  String email,
  List<String> permissions,
  boolean isAuthenticated,
  LocalDateTime authenticatedAt,
  String ipAddress,
  String userAgent
) {
  /**
   * Creates an anonymous (unauthenticated) security context.
   */
  public static SecurityContext anonymous() {
    return new SecurityContext(null, null, null, List.of(), false, null, null, null);
  }

  /**
   * Creates an authenticated security context for a user.
   */
  public static SecurityContext authenticated(User user, List<String> permissions, String ipAddress, String userAgent) {
    return new SecurityContext(
      user.id(),
      user.username(),
      user.email(),
      permissions != null ? List.copyOf(permissions) : List.of(),
      true,
      LocalDateTime.now(),
      ipAddress,
      userAgent
    );
  }

  /**
   * Checks if the current user has a specific permission.
   */
  public boolean hasPermission(String permission) {
    return isAuthenticated && permissions.contains(permission);
  }

  /**
   * Checks if the current user has any of the specified permissions.
   */
  public boolean hasAnyPermission(String... permissionsToCheck) {
    if (!isAuthenticated) {
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
   * Checks if the current user has all of the specified permissions.
   */
  public boolean hasAllPermissions(String... permissionsToCheck) {
    if (!isAuthenticated) {
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
   * Checks if the current user owns the specified resource.
   * This is used for resource-level authorization.
   */
  public boolean isOwner(String resourceOwnerId) {
    return isAuthenticated && userId != null && userId.equals(resourceOwnerId);
  }

  /**
   * Checks if the current user can access the specified resource.
   * Combines ownership and permission checks.
   */
  public boolean canAccess(String resourceOwnerId, String... requiredPermissions) {
    if (!isAuthenticated) {
      return false;
    }

    // User can access their own resources
    if (isOwner(resourceOwnerId)) {
      return true;
    }

    // Or if they have the required admin permissions
    return hasAnyPermission(requiredPermissions);
  }

  /**
   * Gets the current user ID if authenticated.
   */
  public Optional<String> getCurrentUserId() {
    return isAuthenticated ? Optional.ofNullable(userId) : Optional.empty();
  }

  /**
   * Gets the current username if authenticated.
   */
  public Optional<String> getCurrentUsername() {
    return isAuthenticated ? Optional.ofNullable(username) : Optional.empty();
  }
}
