package it.robfrank.linklift.application.domain.exception;

/**
 * Exception thrown when a link is not found in the system.
 */
public class LinkNotFoundException extends LinkLiftException {

    public LinkNotFoundException(String linkId) {
        super("Link not found with id: " + linkId, ErrorCode.LINK_NOT_FOUND);
    }

    public LinkNotFoundException(String message, Throwable cause) {
        super(message, cause, ErrorCode.LINK_NOT_FOUND);
    }
}
