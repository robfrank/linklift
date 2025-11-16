package it.robfrank.linklift.adapter.in.web.error;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import it.robfrank.linklift.application.domain.exception.*;

/**
 * Centralized error handling for the application.
 * Registers exception handlers with Javalin to convert exceptions to appropriate HTTP responses.
 */
public class GlobalExceptionHandler {

  /**
   * Register all exception handlers with the Javalin app.
   *
   * @param app The Javalin application instance
   */
  public static void configure(Javalin app) {
    app.exception(LinkNotFoundException.class, GlobalExceptionHandler::handleLinkNotFoundException);
    app.exception(LinkAlreadyExistsException.class, GlobalExceptionHandler::handleLinkAlreadyExistsException);
    app.exception(ContentNotFoundException.class, GlobalExceptionHandler::handleContentNotFoundException);
    app.exception(ContentDownloadException.class, GlobalExceptionHandler::handleContentDownloadException);
    app.exception(ValidationException.class, GlobalExceptionHandler::handleValidationException);
    app.exception(AuthenticationException.class, GlobalExceptionHandler::handleAuthenticationException);
    app.exception(UserAlreadyExistsException.class, GlobalExceptionHandler::handleUserAlreadyExistsException);
    app.exception(DatabaseException.class, GlobalExceptionHandler::handleDatabaseException);
    app.exception(LinkLiftException.class, GlobalExceptionHandler::handleLinkLiftException);
    app.exception(Exception.class, GlobalExceptionHandler::handleGenericException);
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
    ctx.status(HttpStatus.UNAUTHORIZED);
    ctx.json(
      ErrorResponse.builder()
        .status(HttpStatus.UNAUTHORIZED.getCode())
        .errorCode(exception.getErrorCode())
        .message(exception.getMessage())
        .path(ctx.path())
        .build()
    );
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
