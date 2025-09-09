package it.robfrank.linklift.application.domain.exception;

/**
 * Exception thrown when attempting to create a user that already exists.
 */
public class UserAlreadyExistsException extends LinkLiftException {

    public UserAlreadyExistsException(String identifier) {
        super("User already exists: " + identifier, ErrorCode.USER_ALREADY_EXISTS);
    }

    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause, ErrorCode.USER_ALREADY_EXISTS);
    }
}
