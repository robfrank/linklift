package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import it.robfrank.linklift.application.port.in.AuthenticateUserCommand;
import it.robfrank.linklift.application.port.in.AuthenticateUserUseCase;
import it.robfrank.linklift.application.port.in.CreateUserCommand;
import it.robfrank.linklift.application.port.in.CreateUserUseCase;
import it.robfrank.linklift.application.port.in.RefreshTokenCommand;
import it.robfrank.linklift.application.port.in.RefreshTokenUseCase;

/**
 * REST controller for authentication operations.
 * Handles user registration, login, and token refresh.
 */
public class AuthenticationController {

  private final CreateUserUseCase createUserUseCase;
  private final AuthenticateUserUseCase authenticateUserUseCase;
  private final RefreshTokenUseCase refreshTokenUseCase;

  public AuthenticationController(
    CreateUserUseCase createUserUseCase,
    AuthenticateUserUseCase authenticateUserUseCase,
    RefreshTokenUseCase refreshTokenUseCase
  ) {
    this.createUserUseCase = createUserUseCase;
    this.authenticateUserUseCase = authenticateUserUseCase;
    this.refreshTokenUseCase = refreshTokenUseCase;
  }

  /**
   * Handles user registration.
   * POST /api/v1/auth/register
   */
  public void register(Context ctx) {
    var command = ctx.bodyAsClass(CreateUserCommand.class);
    var user = createUserUseCase.createUser(command);

    ctx
      .status(201)
      .json(new UserRegistrationResponse(user.id(), user.username(), user.email(), user.firstName(), user.lastName(), "User registered successfully"));
  }

  /**
   * Handles user login.
   * POST /api/v1/auth/login
   */
  public void login(Context ctx) {
    var loginRequest = ctx.bodyAsClass(LoginRequest.class);

    var command = new AuthenticateUserCommand(
      loginRequest.loginIdentifier(),
      loginRequest.password(),
      getClientIpAddress(ctx),
      ctx.header("User-Agent"),
      loginRequest.rememberMe()
    );

    var result = authenticateUserUseCase.authenticate(command);

    ctx
      .status(200)
      .json(
        new LoginResponse(
          result.userId(),
          result.username(),
          result.email(),
          result.firstName(),
          result.lastName(),
          result.accessToken(),
          result.refreshToken(),
          result.accessTokenExpiresIn(),
          result.refreshTokenExpiresIn(),
          "Login successful"
        )
      );
  }

  /**
   * Handles token refresh.
   * POST /api/v1/auth/refresh
   */
  public void refreshToken(Context ctx) {
    var request = ctx.bodyAsClass(RefreshTokenRequest.class);
    var command = new RefreshTokenCommand(request.refreshToken(), getClientIpAddress(ctx), ctx.header("User-Agent"));

    var result = refreshTokenUseCase.refreshToken(command);

    ctx
      .status(200)
      .json(
        new LoginResponse(
          result.userId(),
          result.username(),
          result.email(),
          result.firstName(),
          result.lastName(),
          result.accessToken(),
          result.refreshToken(),
          result.accessTokenExpiresIn(),
          result.refreshTokenExpiresIn(),
          "Token refreshed successfully"
        )
      );
  }

  /**
   * Handles user logout.
   * POST /api/v1/auth/logout
   */
  public void logout(Context ctx) {
    // For JWT-based authentication, logout is typically handled client-side
    // by removing the token. Here we just return a success message.
    // In a more sophisticated implementation, you might maintain a token blacklist.

    ctx.status(200).json(new LogoutResponse("Logged out successfully"));
  }

  private String getClientIpAddress(Context ctx) {
    // Check X-Forwarded-For header (for reverse proxy/load balancer)
    String xForwardedFor = ctx.header("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }

    // Check X-Real-IP header (for nginx proxy)
    String xRealIp = ctx.header("X-Real-IP");
    if (xRealIp != null && !xRealIp.trim().isEmpty()) {
      return xRealIp.trim();
    }

    return ctx.ip();
  }

  // Request/Response DTOs
  public record LoginRequest(String loginIdentifier, String password, boolean rememberMe) {}

  public record RefreshTokenRequest(String refreshToken) {}

  public record UserRegistrationResponse(String id, String username, String email, String firstName, String lastName, String message) {}

  public record LoginResponse(
    String userId,
    String username,
    String email,
    String firstName,
    String lastName,
    String accessToken,
    String refreshToken,
    long accessTokenExpiresIn,
    long refreshTokenExpiresIn,
    String message
  ) {}

  public record LogoutResponse(String message) {}
}
