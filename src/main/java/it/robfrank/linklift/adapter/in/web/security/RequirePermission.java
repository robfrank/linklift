package it.robfrank.linklift.adapter.in.web.security;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import it.robfrank.linklift.application.domain.service.AuthorizationService;

/**
 * Javalin handler that requires specific permissions.
 * Can be configured to require any permission or all permissions.
 */
public class RequirePermission implements Handler {

  private final AuthorizationService authorizationService;
  private final String[] permissions;
  private final boolean requireAll;

  /**
   * Creates a handler that requires any of the specified permissions.
   */
  public RequirePermission(AuthorizationService authorizationService, String... permissions) {
    this(authorizationService, false, permissions);
  }

  /**
   * Creates a handler with configurable permission logic.
   *
   * @param authorizationService the authorization service
   * @param requireAll true to require all permissions, false to require any
   * @param permissions the permissions to check
   */
  public RequirePermission(AuthorizationService authorizationService, boolean requireAll, String... permissions) {
    this.authorizationService = authorizationService;
    this.permissions = permissions;
    this.requireAll = requireAll;
  }

  @Override
  public void handle(Context ctx) throws Exception {
    var securityContext = SecurityContext.getSecurityContext(ctx);

    if (requireAll) {
      // Check that user has ALL permissions
      for (String permission : permissions) {
        authorizationService.requirePermission(securityContext, permission);
      }
    } else {
      // Check that user has ANY permission
      authorizationService.requireAnyPermission(securityContext, permissions);
    }
  }

  /**
   * Factory method for creating handlers that require any permission.
   */
  public static RequirePermission any(AuthorizationService authorizationService, String... permissions) {
    return new RequirePermission(authorizationService, false, permissions);
  }

  /**
   * Factory method for creating handlers that require all permissions.
   */
  public static RequirePermission all(AuthorizationService authorizationService, String... permissions) {
    return new RequirePermission(authorizationService, true, permissions);
  }
}
