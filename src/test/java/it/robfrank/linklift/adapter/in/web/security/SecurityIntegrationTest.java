package it.robfrank.linklift.adapter.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import io.javalin.testtools.JavalinTest;
import it.robfrank.linklift.adapter.in.web.error.GlobalExceptionHandler;
import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.exception.ErrorCode;
import it.robfrank.linklift.application.domain.model.Role;
import it.robfrank.linklift.application.domain.model.SecurityContext;
import it.robfrank.linklift.application.domain.model.User;
import it.robfrank.linklift.application.domain.service.AuthorizationService;
import java.time.LocalDateTime;
import java.util.List;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SecurityIntegrationTest {

  private AuthorizationService authorizationService;
  private JwtAuthenticationHandler jwtAuthenticationHandler;
  private RequireAuthentication requireAuthentication;

  @BeforeEach
  void setUp() {
    authorizationService = Mockito.mock(AuthorizationService.class);
    jwtAuthenticationHandler = new JwtAuthenticationHandler(authorizationService);
    requireAuthentication = new RequireAuthentication(authorizationService);
  }

  @Test
  void shouldAllowAccess_whenAuthenticated() {
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    SecurityContext authenticatedContext = SecurityContext.authenticated(user, List.of(), "127.0.0.1", "Test-Agent");

    when(authorizationService.createSecurityContext(any(), any(), any())).thenReturn(authenticatedContext);
    // requireAuthentication.handle will call authorizationService.requireAuthentication
    // which shouldn't throw anything for authenticated context

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      app.before(jwtAuthenticationHandler);
      app.before("/protected", requireAuthentication);
      app.get("/protected", ctx -> ctx.result("success"));

      Response response = client.get("/protected", builder -> builder.header("Authorization", "Bearer valid-token"));

      assertThat(response.code()).isEqualTo(200);
      assertThat(response.body().string()).isEqualTo("success");
    });
  }

  @Test
  void shouldDenyAccess_whenNotAuthenticated() {
    SecurityContext anonymousContext = SecurityContext.anonymous();

    when(authorizationService.createSecurityContext(any(), any(), any())).thenReturn(anonymousContext);
    doThrow(new AuthenticationException("Not authenticated"))
      .when(authorizationService)
      .requireAuthentication(argThat(ctx -> !ctx.isAuthenticated()));

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      app.before(jwtAuthenticationHandler);
      app.before("/protected", requireAuthentication);
      app.get("/protected", ctx -> ctx.result("success"));

      Response response = client.get("/protected");

      assertThat(response.code()).isEqualTo(401);
    });
  }

  @Test
  void shouldAllowAccess_whenUserHasRequiredPermission() {
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.CREATE_LINK);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "127.0.0.1", "Test-Agent");

    when(authorizationService.createSecurityContext(any(), any(), any())).thenReturn(context);

    RequirePermission requirePermission = RequirePermission.any(authorizationService, Role.Permissions.CREATE_LINK);

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      app.before(jwtAuthenticationHandler);
      app.before("/guarded", requirePermission);
      app.get("/guarded", ctx -> ctx.result("granted"));

      Response response = client.get("/guarded", builder -> builder.header("Authorization", "Bearer valid-token"));

      assertThat(response.code()).isEqualTo(200);
      assertThat(response.body().string()).isEqualTo("granted");
    });
  }

  @Test
  void shouldDenyAccess_whenUserLacksRequiredPermission() {
    User user = new User("user-123", "testuser", "test@example.com", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
    List<String> permissions = List.of(Role.Permissions.READ_OWN_LINKS);
    SecurityContext context = SecurityContext.authenticated(user, permissions, "127.0.0.1", "Test-Agent");

    when(authorizationService.createSecurityContext(any(), any(), any())).thenReturn(context);

    doThrow(
      new AuthenticationException(
        "Forbidden",
        ErrorCode.INSUFFICIENT_PERMISSIONS
      )
    )
      .when(authorizationService)
      .requireAnyPermission(any(), eq(Role.Permissions.ADMIN_ACCESS));

    RequirePermission requirePermission = RequirePermission.any(authorizationService, Role.Permissions.ADMIN_ACCESS);

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      app.before(jwtAuthenticationHandler);
      app.before("/admin", requirePermission);
      app.get("/admin", ctx -> ctx.result("secret"));

      Response response = client.get("/admin", builder -> builder.header("Authorization", "Bearer valid-token"));

      assertThat(response.code()).isEqualTo(403);
    });
  }
}
