package it.robfrank.linklift.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcadedb.Constants;
import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.adapter.out.event.SimpleEventPublisher;
import it.robfrank.linklift.adapter.out.persistence.*;
import it.robfrank.linklift.adapter.out.security.BCryptPasswordSecurityAdapter;
import it.robfrank.linklift.adapter.out.security.JwtTokenAdapter;
import it.robfrank.linklift.application.domain.service.AuthenticationService;
import it.robfrank.linklift.application.domain.service.CreateUserService;
import it.robfrank.linklift.application.port.in.AuthenticateUserCommand;
import it.robfrank.linklift.application.port.in.CreateUserCommand;
import it.robfrank.linklift.application.port.in.RefreshTokenCommand;
import it.robfrank.linklift.config.DatabaseInitializer;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for the complete authentication flow.
 * Tests user registration → login → token refresh → protected endpoint access.
 */
@Testcontainers
class AuthenticationIntegrationTest {

  @Container
  private static final GenericContainer arcadeDBContainer = new GenericContainer("arcadedata/arcadedb:" + Constants.getRawVersion())
    .withExposedPorts(2480)
    .withStartupTimeout(Duration.ofSeconds(90))
    .withEnv(
      "JAVA_OPTS",
      """
      -Darcadedb.dateImplementation=java.time.LocalDate
      -Darcadedb.dateTimeImplementation=java.time.LocalDateTime
      -Darcadedb.server.rootPassword=playwithdata
      -Darcadedb.server.plugins=Postgres:com.arcadedb.postgres.PostgresProtocolPlugin
      """
    )
    .waitingFor(Wait.forHttp("/api/v1/ready").forPort(2480).forStatusCode(204));

  // Test-specific JWT secret (256-bit minimum for security)
  private static final String JWT_SECRET = "test-integration-secret-key-for-authentication-tests-minimum-32-chars-for-256bit-security-compliance";

  private RemoteDatabase database;
  private CreateUserService createUserService;
  private AuthenticationService authenticationService;
  private UserPersistenceAdapter userPersistenceAdapter;
  private AuthTokenPersistenceAdapter authTokenPersistenceAdapter;
  private BCryptPasswordSecurityAdapter passwordSecurityAdapter;
  private JwtTokenAdapter jwtTokenAdapter;

  @BeforeAll
  static void setup() {
    new DatabaseInitializer(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "root", "playwithdata").initializeDatabase();
  }

  @BeforeEach
  void setUp() {
    database = new RemoteDatabase(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "linklift", "root", "playwithdata");

    // Clean up test data - delete tokens first (foreign key constraint), then users
    database.command("sql", "DELETE FROM AuthToken WHERE userId IN (SELECT id FROM User WHERE username LIKE 'integrationtest%')");
    database.command("sql", "DELETE FROM User WHERE username LIKE 'integrationtest%'");

    // Initialize adapters
    var userRepository = new ArcadeUserRepository(database, new UserMapper());
    var authTokenRepository = new ArcadeAuthTokenRepository(database, new AuthTokenMapper());
    var eventPublisher = new SimpleEventPublisher();

    userPersistenceAdapter = new UserPersistenceAdapter(userRepository);
    authTokenPersistenceAdapter = new AuthTokenPersistenceAdapter(authTokenRepository);
    passwordSecurityAdapter = new BCryptPasswordSecurityAdapter();
    jwtTokenAdapter = new JwtTokenAdapter(JWT_SECRET);

    // Initialize services
    createUserService = new CreateUserService(userPersistenceAdapter, userPersistenceAdapter, passwordSecurityAdapter, eventPublisher);

    authenticationService = new AuthenticationService(
      userPersistenceAdapter,
      userPersistenceAdapter,
      passwordSecurityAdapter,
      jwtTokenAdapter,
      authTokenPersistenceAdapter,
      eventPublisher
    );
  }

  @Test
  void completeAuthenticationFlow_shouldWorkEndToEnd() {
    // Step 1: User Registration
    var createUserCommand = new CreateUserCommand("integrationtestuser", "integration@example.com", "StrongPassword123!", "Integration", "Test");

    var createdUser = createUserService.createUser(createUserCommand);

    assertThat(createdUser).isNotNull();
    assertThat(createdUser.username()).isEqualTo("integrationtestuser");
    assertThat(createdUser.email()).isEqualTo("integration@example.com");
    assertThat(createdUser.firstName()).isEqualTo("Integration");
    assertThat(createdUser.lastName()).isEqualTo("Test");
    assertThat(createdUser.isActive()).isTrue();
    assertThat(createdUser.passwordHash()).isNull(); // Should be stripped by toPublic()

    // Step 2: User Login
    var loginCommand = new AuthenticateUserCommand("integrationtestuser", "StrongPassword123!", "192.168.1.1", "Integration-Test-Agent", false);

    var authResult = authenticationService.authenticate(loginCommand);

    assertThat(authResult).isNotNull();
    assertThat(authResult.userId()).isEqualTo(createdUser.id());
    assertThat(authResult.username()).isEqualTo("integrationtestuser");
    assertThat(authResult.email()).isEqualTo("integration@example.com");
    assertThat(authResult.accessToken()).isNotNull();
    assertThat(authResult.refreshToken()).isNotNull();
    assertThat(authResult.accessTokenExpiresIn()).isGreaterThan(0);
    assertThat(authResult.refreshTokenExpiresIn()).isGreaterThan(0);

    // Step 3: Token Validation
    var tokenClaims = jwtTokenAdapter.validateToken(authResult.accessToken());
    assertThat(tokenClaims).isPresent();
    assertThat(tokenClaims.get().userId()).isEqualTo(createdUser.id());
    assertThat(tokenClaims.get().username()).isEqualTo("integrationtestuser");
    assertThat(tokenClaims.get().email()).isEqualTo("integration@example.com");
    assertThat(tokenClaims.get().tokenType()).isEqualTo("access");

    // Step 4: Token Refresh
    var refreshCommand = new RefreshTokenCommand(authResult.refreshToken(), "192.168.1.1", "Integration-Test-Agent");

    var refreshResult = authenticationService.refreshToken(refreshCommand);

    assertThat(refreshResult).isNotNull();
    assertThat(refreshResult.userId()).isEqualTo(createdUser.id());
    assertThat(refreshResult.accessToken()).isNotEqualTo(authResult.accessToken()); // New access token
    assertThat(refreshResult.refreshToken()).isNotEqualTo(authResult.refreshToken()); // New refresh token

    // Step 5: Verify old refresh token is marked as used
    var oldRefreshToken = authTokenPersistenceAdapter.findByToken(authResult.refreshToken());
    assertThat(oldRefreshToken).isPresent();
    assertThat(oldRefreshToken.get().usedAt()).isNotNull();

    // Step 6: Verify new tokens are valid
    var newAccessTokenClaims = jwtTokenAdapter.validateToken(refreshResult.accessToken());
    assertThat(newAccessTokenClaims).isPresent();
    assertThat(newAccessTokenClaims.get().userId()).isEqualTo(createdUser.id());

    var newRefreshTokenClaims = jwtTokenAdapter.validateToken(refreshResult.refreshToken());
    assertThat(newRefreshTokenClaims).isPresent();
    assertThat(newRefreshTokenClaims.get().tokenType()).isEqualTo("refresh");

    // Step 7: Verify user's last login was updated
    var updatedUser = userPersistenceAdapter.findUserById(createdUser.id());
    assertThat(updatedUser).isPresent();
    assertThat(updatedUser.get().lastLoginAt()).isNotNull();
  }

  @Test
  void authenticationFlow_shouldFailWithInvalidCredentials() {
    // Step 1: Create user
    var createUserCommand = new CreateUserCommand("integrationtestuser2", "integration2@example.com", "StrongPassword123!", "Integration", "Test");

    createUserService.createUser(createUserCommand);

    // Step 2: Try to login with wrong password
    var loginCommand = new AuthenticateUserCommand("integrationtestuser2", "WrongPassword123!", "192.168.1.1", "Integration-Test-Agent", false);

    assertThatThrownBy(() -> authenticationService.authenticate(loginCommand)).hasMessageContaining("Invalid username or password");
  }

  @Test
  void authenticationFlow_shouldFailWithNonexistentUser() {
    // Try to login with nonexistent user
    var loginCommand = new AuthenticateUserCommand("nonexistentuser", "SomePassword123!", "192.168.1.1", "Integration-Test-Agent", false);

    assertThatThrownBy(() -> authenticationService.authenticate(loginCommand)).hasMessageContaining("Invalid username or password");
  }

  @Test
  void refreshTokenFlow_shouldFailWithInvalidToken() {
    // Try to refresh with invalid token
    var refreshCommand = new RefreshTokenCommand("invalid-refresh-token", "192.168.1.1", "Integration-Test-Agent");

    assertThatThrownBy(() -> authenticationService.refreshToken(refreshCommand)).hasMessageContaining("Invalid authentication token");
  }

  @Test
  void refreshTokenFlow_shouldFailWithUsedToken() {
    // Step 1: Create user and login
    var createUserCommand = new CreateUserCommand("integrationtestuser3", "integration3@example.com", "StrongPassword123!", "Integration", "Test");

    var user = createUserService.createUser(createUserCommand);

    var loginCommand = new AuthenticateUserCommand("integrationtestuser3", "StrongPassword123!", "192.168.1.1", "Integration-Test-Agent", false);

    var authResult = authenticationService.authenticate(loginCommand);

    // Step 2: Use refresh token once
    var refreshCommand = new RefreshTokenCommand(authResult.refreshToken(), "192.168.1.1", "Integration-Test-Agent");

    authenticationService.refreshToken(refreshCommand);

    // Step 3: Try to use the same refresh token again
    assertThatThrownBy(() -> authenticationService.refreshToken(refreshCommand)).hasMessageContaining("Invalid authentication token");
  }

  @Test
  void emailLogin_shouldWorkSameAsUsernameLogin() {
    // Step 1: Create user
    var createUserCommand = new CreateUserCommand("integrationtestuser4", "integration4@example.com", "StrongPassword123!", "Integration", "Test");

    var createdUser = createUserService.createUser(createUserCommand);

    // Step 2: Login with email instead of username
    var loginCommand = new AuthenticateUserCommand(
      "integration4@example.com", // Using email
      "StrongPassword123!",
      "192.168.1.1",
      "Integration-Test-Agent",
      false
    );

    var authResult = authenticationService.authenticate(loginCommand);

    assertThat(authResult).isNotNull();
    assertThat(authResult.userId()).isEqualTo(createdUser.id());
    assertThat(authResult.username()).isEqualTo("integrationtestuser4");
    assertThat(authResult.email()).isEqualTo("integration4@example.com");
  }

  @Test
  void rememberMeLogin_shouldGenerateLongerRefreshToken() {
    // Step 1: Create user
    var createUserCommand = new CreateUserCommand("integrationtestuser5", "integration5@example.com", "StrongPassword123!", "Integration", "Test");

    createUserService.createUser(createUserCommand);

    // Step 2: Login with remember me = true
    var loginCommand = new AuthenticateUserCommand(
      "integrationtestuser5",
      "StrongPassword123!",
      "192.168.1.1",
      "Integration-Test-Agent",
      true // remember me
    );

    var authResult = authenticationService.authenticate(loginCommand);

    assertThat(authResult).isNotNull();
    // Remember me should give longer refresh token expiry (30 days vs 7 days)
    assertThat(authResult.refreshTokenExpiresIn()).isGreaterThan(7 * 24 * 60 * 60L); // More than 7 days
  }

  @Test
  void passwordStrengthValidation_shouldPreventWeakPasswords() {
    var createUserCommand = new CreateUserCommand(
      "integrationtestuser6",
      "integration6@example.com",
      "weak", // Too weak password
      "Integration",
      "Test"
    );

    assertThatThrownBy(() -> createUserService.createUser(createUserCommand)).hasMessageContaining("Password does not meet security requirements");
  }
}
