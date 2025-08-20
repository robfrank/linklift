package it.robfrank.linklift.config;

import it.robfrank.linklift.adapter.in.web.AuthenticationController;
import it.robfrank.linklift.adapter.out.persitence.AuthTokenPersistenceAdapter;
import it.robfrank.linklift.adapter.out.persitence.UserPersistenceAdapter;
import it.robfrank.linklift.adapter.out.persitence.UserRolesPersistenceAdapter;
import it.robfrank.linklift.adapter.out.security.BCryptPasswordSecurityAdapter;
import it.robfrank.linklift.adapter.out.security.JwtTokenAdapter;
import it.robfrank.linklift.application.domain.service.AuthenticationService;
import it.robfrank.linklift.application.domain.service.AuthorizationService;
import it.robfrank.linklift.application.domain.service.CreateUserService;
import it.robfrank.linklift.application.port.out.DomainEventPublisher;

/**
 * Configuration class for security components.
 * Provides factory methods for creating and wiring security-related services.
 */
public class SecurityConfiguration {

  private static final String JWT_SECRET_KEY = "your-secret-key-here-change-in-production-must-be-256-bits-long";

  /**
   * Creates a JWT token adapter with the configured secret key.
   */
  public static JwtTokenAdapter createJwtTokenAdapter() {
    return new JwtTokenAdapter(JWT_SECRET_KEY);
  }

  /**
   * Creates a BCrypt password security adapter.
   */
  public static BCryptPasswordSecurityAdapter createPasswordSecurityAdapter() {
    return new BCryptPasswordSecurityAdapter();
  }

  /**
   * Creates the authorization service with required dependencies.
   */
  public static AuthorizationService createAuthorizationService(UserPersistenceAdapter userPersistenceAdapter) {
    var jwtTokenAdapter = createJwtTokenAdapter();
    var userRolesAdapter = new UserRolesPersistenceAdapter();

    return new AuthorizationService(jwtTokenAdapter, userPersistenceAdapter, userRolesAdapter);
  }

  /**
   * Creates the authentication service with required dependencies.
   */
  public static AuthenticationService createAuthenticationService(
    UserPersistenceAdapter userPersistenceAdapter,
    AuthTokenPersistenceAdapter authTokenPersistenceAdapter,
    DomainEventPublisher eventPublisher
  ) {
    var passwordSecurityAdapter = createPasswordSecurityAdapter();
    var jwtTokenAdapter = createJwtTokenAdapter();

    return new AuthenticationService(
      userPersistenceAdapter, // LoadUserPort
      userPersistenceAdapter, // SaveUserPort
      passwordSecurityAdapter,
      jwtTokenAdapter,
      authTokenPersistenceAdapter,
      eventPublisher
    );
  }

  /**
   * Creates the user creation service with required dependencies.
   */
  public static CreateUserService createUserService(UserPersistenceAdapter userPersistenceAdapter, DomainEventPublisher eventPublisher) {
    var passwordSecurityAdapter = createPasswordSecurityAdapter();

    return new CreateUserService(
      userPersistenceAdapter, // LoadUserPort
      userPersistenceAdapter, // SaveUserPort
      passwordSecurityAdapter,
      eventPublisher
    );
  }

  /**
   * Creates the authentication controller with required dependencies.
   */
  public static AuthenticationController createAuthenticationController(
    UserPersistenceAdapter userPersistenceAdapter,
    AuthTokenPersistenceAdapter authTokenPersistenceAdapter,
    DomainEventPublisher eventPublisher
  ) {
    var createUserService = createUserService(userPersistenceAdapter, eventPublisher);
    var authenticationService = createAuthenticationService(userPersistenceAdapter, authTokenPersistenceAdapter, eventPublisher);

    return new AuthenticationController(
      createUserService,
      authenticationService, // AuthenticateUserUseCase
      authenticationService // RefreshTokenUseCase
    );
  }
}
