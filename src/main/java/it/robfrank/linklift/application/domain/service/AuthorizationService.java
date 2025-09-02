package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.model.SecurityContext;
import it.robfrank.linklift.application.domain.model.User;
import it.robfrank.linklift.application.port.out.JwtTokenPort;
import it.robfrank.linklift.application.port.out.LoadUserPort;
import it.robfrank.linklift.application.port.out.LoadUserRolesPort;
import java.util.Optional;

/**
 * Domain service for authorization operations.
 * Handles JWT token validation and security context creation.
 */
public class AuthorizationService {

  private final JwtTokenPort jwtTokenPort;
  private final LoadUserPort loadUserPort;
  private final LoadUserRolesPort loadUserRolesPort;

  public AuthorizationService(JwtTokenPort jwtTokenPort, LoadUserPort loadUserPort, LoadUserRolesPort loadUserRolesPort) {
    this.jwtTokenPort = jwtTokenPort;
    this.loadUserPort = loadUserPort;
    this.loadUserRolesPort = loadUserRolesPort;
  }

  /**
   * Creates a security context from a JWT token.
   *
   * @param token the JWT token
   * @param ipAddress the client IP address
   * @param userAgent the client user agent
   * @return SecurityContext if token is valid, anonymous context otherwise
   */
  public SecurityContext createSecurityContext(String token, String ipAddress, String userAgent) {
    if (token == null || token.trim().isEmpty()) {
      return SecurityContext.anonymous();
    }

    try {
      // Validate JWT token
      var tokenClaims = jwtTokenPort.validateToken(token);
      if (tokenClaims.isEmpty()) {
        return SecurityContext.anonymous();
      }

      var claims = tokenClaims.get();

      // Load user from database to ensure they still exist and are active
      var user = loadUserPort.findUserById(claims.userId());
      if (user.isEmpty() || !user.get().isActive()) {
        return SecurityContext.anonymous();
      }

      // Load user permissions
      var permissions = loadUserRolesPort.getUserPermissions(claims.userId());

      return SecurityContext.authenticated(user.get(), permissions, ipAddress, userAgent);
    } catch (Exception e) {
      // Any exception during token validation results in anonymous context
      return SecurityContext.anonymous();
    }
  }

  /**
   * Validates that a user is authenticated and throws exception if not.
   *
   * @param securityContext the security context to check
   * @throws AuthenticationException if user is not authenticated
   */
  public void requireAuthentication(SecurityContext securityContext) {
    if (!securityContext.isAuthenticated()) {
      throw AuthenticationException.unauthorizedAccess();
    }
  }

  /**
   * Validates that a user has a specific permission.
   *
   * @param securityContext the security context to check
   * @param permission the required permission
   * @throws AuthenticationException if user doesn't have permission
   */
  public void requirePermission(SecurityContext securityContext, String permission) {
    requireAuthentication(securityContext);

    if (!securityContext.hasPermission(permission)) {
      throw AuthenticationException.insufficientPermissions();
    }
  }

  /**
   * Validates that a user has any of the specified permissions.
   *
   * @param securityContext the security context to check
   * @param permissions the required permissions (any one of them)
   * @throws AuthenticationException if user doesn't have any permission
   */
  public void requireAnyPermission(SecurityContext securityContext, String... permissions) {
    requireAuthentication(securityContext);

    if (!securityContext.hasAnyPermission(permissions)) {
      throw AuthenticationException.insufficientPermissions();
    }
  }

  /**
   * Validates that a user can access a specific resource.
   * User can access if they own the resource or have the required permissions.
   *
   * @param securityContext the security context to check
   * @param resourceOwnerId the owner of the resource
   * @param adminPermissions the permissions that allow admin access
   * @throws AuthenticationException if user cannot access the resource
   */
  public void requireResourceAccess(SecurityContext securityContext, String resourceOwnerId, String... adminPermissions) {
    requireAuthentication(securityContext);

    if (!securityContext.canAccess(resourceOwnerId, adminPermissions)) {
      throw AuthenticationException.insufficientPermissions();
    }
  }

  /**
   * Extracts user ID from JWT token without full validation.
   * Used for token blacklist checking.
   *
   * @param token the JWT token
   * @return user ID if extractable, empty otherwise
   */
  public Optional<String> extractUserIdFromToken(String token) {
    return jwtTokenPort.extractUserIdFromToken(token);
  }
}
