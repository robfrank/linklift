package it.robfrank.linklift.adapter.in.web.error;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.router.JavalinDefaultRoutingApi;
import it.robfrank.linklift.application.domain.exception.*;

/**
 * Centralized error handling for the application.
 * Registers exception handlers with Javalin to convert exceptions to
 * appropriate HTTP responses.
 */
public class GlobalExceptionHandler {

  /**
   * Register all exception handlers with the Javalin router.
   *
   * <p>In Javalin 7 routing (including exception handlers) is configured via
   * {@code config.routes} at app-creation time, so this accepts the routing API
   * exposed by {@code JavalinConfig.routes}.
   *
   * @param router The Javalin routing API (typically {@code config.routes})
   */
  public static void configure(JavalinDefaultRoutingApi router) {
    router.exception(LinkNotFoundException.class, GlobalExceptionHandler::handleLinkNotFoundException);
    router.exception(NoteNotFoundException.class, GlobalExceptionHandler::handleNoteNotFoundException);
    router.exception(LinkAlreadyExistsException.class, GlobalExceptionHandler::handleLinkAlreadyExistsException);
    router.exception(ContentNotFoundException.class, GlobalExceptionHandler::handleContentNotFoundException);
    router.exception(ContentDownloadException.class, GlobalExceptionHandler::handleContentDownloadException);
    router.exception(ValidationException.class, GlobalExceptionHandler::handleValidationException);
    router.exception(AuthenticationException.class, GlobalExceptionHandler::handleAuthenticationException);
    router.exception(UserAlreadyExistsException.class, GlobalExceptionHandler::handleUserAlreadyExistsException);
    router.exception(DatabaseException.class, GlobalExceptionHandler::handleDatabaseException);
    router.exception(LinkLiftException.class, GlobalExceptionHandler::handleLinkLiftException);
    router.exception(Exception.class, GlobalExceptionHandler::handleGenericException);
  }

  private static void handleLinkNotFoundException(LinkNotFoundException exception, Context ctx) {
    ctx.status(HttpStatus.NOT_FOUND);
    ctx.json(
      ErrorResponse.builder()
        .status(HttpStatus.NOT_FOUND.getCode())
        .errorCode(exception.getErrorCode())
        .message(exception.getMessage())
        .path(ctx.path())
        .build()
    );
  }

  private static void handleNoteNotFoundException(NoteNotFoundException exception, Context ctx) {
    ctx.status(HttpStatus.NOT_FOUND);
    ctx.json(
      ErrorResponse.builder()
        .status(HttpStatus.NOT_FOUND.getCode())
        .errorCode(exception.getErrorCode())
        .message(exception.getMessage())
        .path(ctx.path())
        .build()
    );
  }

  private static void handleLinkAlreadyExistsException(LinkAlreadyExistsException exception, Context ctx) {
    ctx.status(HttpStatus.CONFLICT);
    ctx.json(
      ErrorResponse.builder().status(HttpStatus.CONFLICT.getCode()).errorCode(exception.getErrorCode()).message(exception.getMessage()).path(ctx.path()).build()
    );
  }

  private static void handleValidationException(ValidationException exception, Context ctx) {
    ctx.status(HttpStatus.BAD_REQUEST);
    ctx.json(
      ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.getCode())
        .errorCode(exception.getErrorCode())
        .message(exception.getMessage())
        .fieldErrors(exception.getFieldErrors())
        .path(ctx.path())
        .build()
    );
  }

  private static void handleAuthenticationException(AuthenticationException exception, Context ctx) {
    HttpStatus status =
      switch (exception.getErrorCode()) {
        case INSUFFICIENT_PERMISSIONS -> HttpStatus.FORBIDDEN;
        case UNAUTHORIZED, UNAUTHORIZED_ACCESS, TOKEN_EXPIRED, TOKEN_INVALID, TOKEN_REVOKED -> HttpStatus.UNAUTHORIZED;
        default -> HttpStatus.UNAUTHORIZED;
      };

    ctx.status(status);
    ctx.json(ErrorResponse.builder().status(status.getCode()).errorCode(exception.getErrorCode()).message(exception.getMessage()).path(ctx.path()).build());
  }

  private static void handleUserAlreadyExistsException(UserAlreadyExistsException exception, Context ctx) {
    ctx.status(HttpStatus.CONFLICT);
    ctx.json(
      ErrorResponse.builder().status(HttpStatus.CONFLICT.getCode()).errorCode(exception.getErrorCode()).message(exception.getMessage()).path(ctx.path()).build()
    );
  }

  private static void handleContentNotFoundException(ContentNotFoundException exception, Context ctx) {
    ctx.status(HttpStatus.NOT_FOUND);
    ctx.json(
      ErrorResponse.builder()
        .status(HttpStatus.NOT_FOUND.getCode())
        .errorCode(exception.getErrorCode())
        .message(exception.getMessage())
        .path(ctx.path())
        .build()
    );
  }

  private static void handleContentDownloadException(ContentDownloadException exception, Context ctx) {
    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
    ctx.json(
      ErrorResponse.builder()
        .status(HttpStatus.INTERNAL_SERVER_ERROR.getCode())
        .errorCode(exception.getErrorCode())
        .message(exception.getMessage())
        .path(ctx.path())
        .build()
    );

    // Log the exception for internal debugging
    ctx.attribute("exception", exception);
  }

  private static void handleDatabaseException(DatabaseException exception, Context ctx) {
    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
    ctx.json(
      ErrorResponse.builder()
        .status(HttpStatus.INTERNAL_SERVER_ERROR.getCode())
        .errorCode(exception.getErrorCode())
        .message("Database operation failed")
        .path(ctx.path())
        .build()
    );

    // Log the full exception details for internal debugging
    ctx.attribute("exception", exception);
  }

  private static void handleLinkLiftException(LinkLiftException exception, Context ctx) {
    HttpStatus status =
      switch (exception.getErrorCode()) {
        case COLLECTION_NOT_FOUND, LINK_NOT_FOUND, CONTENT_NOT_FOUND, USER_NOT_FOUND, NOTE_NOT_FOUND, TAG_NOT_FOUND -> HttpStatus.NOT_FOUND;
        case UNAUTHORIZED, UNAUTHORIZED_ACCESS -> HttpStatus.UNAUTHORIZED;
        case INSUFFICIENT_PERMISSIONS -> HttpStatus.FORBIDDEN;
        default -> HttpStatus.INTERNAL_SERVER_ERROR;
      };

    ctx.status(status);
    ctx.json(ErrorResponse.builder().status(status.getCode()).errorCode(exception.getErrorCode()).message(exception.getMessage()).path(ctx.path()).build());

    // Log the exception for internal debugging
    ctx.attribute("exception", exception);
  }

  private static void handleGenericException(Exception exception, Context ctx) {
    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
    ctx.json(
      ErrorResponse.builder()
        .status(HttpStatus.INTERNAL_SERVER_ERROR.getCode())
        .errorCode(ErrorCode.INTERNAL_ERROR)
        .message("An unexpected error occurred")
        .path(ctx.path())
        .build()
    );

    // Log the exception for internal debugging
    ctx.attribute("exception", exception);
  }
}
