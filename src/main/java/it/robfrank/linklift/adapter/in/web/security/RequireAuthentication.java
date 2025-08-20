package it.robfrank.linklift.adapter.in.web.security;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import it.robfrank.linklift.application.domain.service.AuthorizationService;

/**
 * Javalin handler that requires authentication.
 * Throws appropriate exceptions if user is not authenticated.
 */
public class RequireAuthentication implements Handler {

  private final AuthorizationService authorizationService;

  public RequireAuthentication(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Override
  public void handle(Context ctx) throws Exception {
    var securityContext = SecurityContext.getSecurityContext(ctx);
    authorizationService.requireAuthentication(securityContext);
  }
}
