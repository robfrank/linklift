package it.robfrank.linklift.application.domain.exception;

/**
 * Exception thrown when attempting to create a link that already exists.
 */
public class LinkAlreadyExistsException extends LinkLiftException {

    public LinkAlreadyExistsException(String url) {
        super("Link already exists with URL: " + url, ErrorCode.LINK_ALREADY_EXISTS);
    }

    public LinkAlreadyExistsException(String message, Throwable cause) {
        super(message, cause, ErrorCode.LINK_ALREADY_EXISTS);
    }
}
