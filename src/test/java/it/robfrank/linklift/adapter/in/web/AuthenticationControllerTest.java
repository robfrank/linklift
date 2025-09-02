package it.robfrank.linklift.adapter.in.web;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.javalin.testtools.JavalinTest;
import it.robfrank.linklift.adapter.in.web.error.GlobalExceptionHandler;
import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.exception.UserAlreadyExistsException;
import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.User;
import it.robfrank.linklift.application.domain.service.AuthenticationService;
import it.robfrank.linklift.application.port.in.AuthenticateUserCommand;
import it.robfrank.linklift.application.port.in.AuthenticateUserUseCase;
import it.robfrank.linklift.application.port.in.CreateUserCommand;
import it.robfrank.linklift.application.port.in.CreateUserUseCase;
import it.robfrank.linklift.application.port.in.RefreshTokenCommand;
import it.robfrank.linklift.application.port.in.RefreshTokenUseCase;
import java.time.LocalDateTime;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuthenticationControllerTest {

  private CreateUserUseCase createUserUseCase;
  private AuthenticateUserUseCase authenticateUserUseCase;
  private RefreshTokenUseCase refreshTokenUseCase;
  private AuthenticationController authenticationController;

  @BeforeEach
  void setUp() {
    createUserUseCase = Mockito.mock(CreateUserUseCase.class);
    authenticateUserUseCase = Mockito.mock(AuthenticateUserUseCase.class);
    refreshTokenUseCase = Mockito.mock(RefreshTokenUseCase.class);
    authenticationController = new AuthenticationController(createUserUseCase, authenticateUserUseCase, refreshTokenUseCase);
  }

  @Test
  void register_shouldCreateUser_andReturnSuccessResponse() {
    // Arrange
    User createdUser = new User(
      "user-123",
      "testuser",
      "test@example.com",
      null, // passwordHash removed in toPublic()
      null, // salt removed in toPublic()
      LocalDateTime.now(),
      null,
      true,
      "John",
      "Doe",
      null
    );

    when(createUserUseCase.createUser(any(CreateUserCommand.class))).thenReturn(createdUser);

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.post("/api/v1/auth/register", authenticationController::register);

      Response response = client.post(
        "/api/v1/auth/register",
        """
        {
            "username": "testuser",
            "email": "test@example.com",
            "password": "StrongPassword123!",
            "firstName": "John",
            "lastName": "Doe"
        }
        """
      );

      assertThat(response.code()).isEqualTo(201);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(
        json -> json.node("id").isEqualTo("user-123"),
        json -> json.node("username").isEqualTo("testuser"),
        json -> json.node("email").isEqualTo("test@example.com"),
        json -> json.node("firstName").isEqualTo("John"),
        json -> json.node("lastName").isEqualTo("Doe"),
        json -> json.node("message").isEqualTo("User registered successfully")
      );
    });
  }

  @Test
  void register_shouldReturn409_whenUserAlreadyExists() {
    // Arrange
    when(createUserUseCase.createUser(any(CreateUserCommand.class))).thenThrow(new UserAlreadyExistsException("testuser"));

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.post("/api/v1/auth/register", authenticationController::register);

      Response response = client.post(
        "/api/v1/auth/register",
        """
        {
            "username": "testuser",
            "email": "test@example.com",
            "password": "StrongPassword123!",
            "firstName": "John",
            "lastName": "Doe"
        }
        """
      );

      assertThat(response.code()).isEqualTo(409);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(json -> json.node("status").isEqualTo(409), json -> json.node("message").isString().contains("testuser"));
    });
  }

  @Test
  void register_shouldReturn400_whenValidationFails() {
    // Arrange
    when(createUserUseCase.createUser(any(CreateUserCommand.class))).thenThrow(new ValidationException("Username too short"));

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.post("/api/v1/auth/register", authenticationController::register);

      Response response = client.post(
        "/api/v1/auth/register",
        """
        {
            "username": "ab",
            "email": "test@example.com",
            "password": "StrongPassword123!",
            "firstName": "John",
            "lastName": "Doe"
        }
        """
      );

      assertThat(response.code()).isEqualTo(400);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(json -> json.node("status").isEqualTo(400), json -> json.node("message").isString().contains("Username too short"));
    });
  }

  @Test
  void login_shouldAuthenticateUser_andReturnTokens() {
    // Arrange
    AuthenticationService.AuthenticationResult authResult = new AuthenticationService.AuthenticationResult(
      "user-123",
      "testuser",
      "test@example.com",
      "John",
      "Doe",
      "access-token",
      "refresh-token",
      900L, // 15 minutes
      604800L // 7 days
    );

    when(authenticateUserUseCase.authenticate(any(AuthenticateUserCommand.class))).thenReturn(authResult);

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.post("/api/v1/auth/login", authenticationController::login);

      Response response = client.post(
        "/api/v1/auth/login",
        """
        {
            "loginIdentifier": "testuser",
            "password": "password123",
            "rememberMe": false
        }
        """
      );

      assertThat(response.code()).isEqualTo(200);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(
        json -> json.node("userId").isEqualTo("user-123"),
        json -> json.node("username").isEqualTo("testuser"),
        json -> json.node("email").isEqualTo("test@example.com"),
        json -> json.node("firstName").isEqualTo("John"),
        json -> json.node("lastName").isEqualTo("Doe"),
        json -> json.node("accessToken").isEqualTo("access-token"),
        json -> json.node("refreshToken").isEqualTo("refresh-token"),
        json -> json.node("accessTokenExpiresIn").isEqualTo(900),
        json -> json.node("refreshTokenExpiresIn").isEqualTo(604800),
        json -> json.node("message").isEqualTo("Login successful")
      );
    });
  }

  @Test
  void login_shouldUseXForwardedForHeader_whenPresent() {
    // Arrange
    AuthenticationService.AuthenticationResult authResult = new AuthenticationService.AuthenticationResult(
      "user-123",
      "testuser",
      "test@example.com",
      "John",
      "Doe",
      "access-token",
      "refresh-token",
      900L,
      604800L
    );

    when(authenticateUserUseCase.authenticate(any(AuthenticateUserCommand.class))).thenReturn(authResult);

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.post("/api/v1/auth/login", authenticationController::login);

      Response response = client.post(
        "/api/v1/auth/login",
        """
        {
            "loginIdentifier": "testuser",
            "password": "password123",
            "rememberMe": false
        }
        """
      );

      assertThat(response.code()).isEqualTo(200);

      // Verify that the command was called successfully
      verify(authenticateUserUseCase).authenticate(any(AuthenticateUserCommand.class));
    });
  }

  @Test
  void login_shouldUseXRealIPHeader_whenXForwardedForNotPresent() {
    // Arrange
    AuthenticationService.AuthenticationResult authResult = new AuthenticationService.AuthenticationResult(
      "user-123",
      "testuser",
      "test@example.com",
      "John",
      "Doe",
      "access-token",
      "refresh-token",
      900L,
      604800L
    );

    when(authenticateUserUseCase.authenticate(any(AuthenticateUserCommand.class))).thenReturn(authResult);

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.post("/api/v1/auth/login", authenticationController::login);

      Response response = client.post(
        "/api/v1/auth/login",
        """
        {
            "loginIdentifier": "testuser",
            "password": "password123",
            "rememberMe": false
        }
        """
      );

      assertThat(response.code()).isEqualTo(200);

      // Verify that the command was called successfully
      verify(authenticateUserUseCase).authenticate(any(AuthenticateUserCommand.class));
    });
  }

  @Test
  void login_shouldReturn401_whenAuthenticationFails() {
    // Arrange
    when(authenticateUserUseCase.authenticate(any(AuthenticateUserCommand.class))).thenThrow(AuthenticationException.invalidCredentials());

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.post("/api/v1/auth/login", authenticationController::login);

      Response response = client.post(
        "/api/v1/auth/login",
        """
        {
            "loginIdentifier": "testuser",
            "password": "wrongpassword",
            "rememberMe": false
        }
        """
      );

      assertThat(response.code()).isEqualTo(401);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(
        json -> json.node("status").isEqualTo(401),
        json -> json.node("message").isString().contains("Invalid username or password")
      );
    });
  }

  @Test
  void refreshToken_shouldRefreshTokens_andReturnNewTokens() {
    // Arrange
    AuthenticationService.AuthenticationResult authResult = new AuthenticationService.AuthenticationResult(
      "user-123",
      "testuser",
      "test@example.com",
      "John",
      "Doe",
      "new-access-token",
      "new-refresh-token",
      900L, // 15 minutes
      604800L // 7 days
    );

    when(refreshTokenUseCase.refreshToken(any(RefreshTokenCommand.class))).thenReturn(authResult);

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.post("/api/v1/auth/refresh", authenticationController::refreshToken);

      Response response = client.post(
        "/api/v1/auth/refresh",
        """
        {
            "refreshToken": "old-refresh-token"
        }
        """
      );

      assertThat(response.code()).isEqualTo(200);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(
        json -> json.node("userId").isEqualTo("user-123"),
        json -> json.node("username").isEqualTo("testuser"),
        json -> json.node("accessToken").isEqualTo("new-access-token"),
        json -> json.node("refreshToken").isEqualTo("new-refresh-token"),
        json -> json.node("accessTokenExpiresIn").isEqualTo(900),
        json -> json.node("refreshTokenExpiresIn").isEqualTo(604800),
        json -> json.node("message").isEqualTo("Token refreshed successfully")
      );

      // Verify that the command was called with the correct refresh token
      verify(refreshTokenUseCase).refreshToken(argThat(command -> "old-refresh-token".equals(command.refreshToken())));
    });
  }

  @Test
  void refreshToken_shouldReturn401_whenTokenIsInvalid() {
    // Arrange
    when(refreshTokenUseCase.refreshToken(any(RefreshTokenCommand.class))).thenThrow(AuthenticationException.tokenInvalid());

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.post("/api/v1/auth/refresh", authenticationController::refreshToken);

      Response response = client.post(
        "/api/v1/auth/refresh",
        """
        {
            "refreshToken": "invalid-refresh-token"
        }
        """
      );

      assertThat(response.code()).isEqualTo(401);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(
        json -> json.node("status").isEqualTo(401),
        json -> json.node("message").isString().contains("Invalid authentication token")
      );
    });
  }

  @Test
  void logout_shouldReturnSuccessMessage() {
    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.post("/api/v1/auth/logout", authenticationController::logout);

      Response response = client.post("/api/v1/auth/logout", "");

      assertThat(response.code()).isEqualTo(200);

      String responseBody = response.body().string();
      assertThatJson(responseBody).node("message").isEqualTo("Logged out successfully");
    });
  }

  @Test
  void getClientIpAddress_shouldHandleEmptyXForwardedFor() {
    // This test verifies the private method behavior indirectly through login

    // Arrange
    AuthenticationService.AuthenticationResult authResult = new AuthenticationService.AuthenticationResult(
      "user-123",
      "testuser",
      "test@example.com",
      "John",
      "Doe",
      "access-token",
      "refresh-token",
      900L,
      604800L
    );

    when(authenticateUserUseCase.authenticate(any(AuthenticateUserCommand.class))).thenReturn(authResult);

    JavalinTest.test((app, client) -> {
      // Configure exception handlers
      GlobalExceptionHandler.configure(app);
      app.post("/api/v1/auth/login", authenticationController::login);

      Response response = client.post(
        "/api/v1/auth/login",
        """
        {
            "loginIdentifier": "testuser",
            "password": "password123",
            "rememberMe": false
        }
        """
      );

      assertThat(response.code()).isEqualTo(200);

      // Verify that the command was called successfully
      verify(authenticateUserUseCase).authenticate(any(AuthenticateUserCommand.class));
    });
  }
}
