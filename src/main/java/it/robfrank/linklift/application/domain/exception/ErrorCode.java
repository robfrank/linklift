package it.robfrank.linklift.application.domain.exception;

/**
 * Error codes for the LinkLift application.
 * These codes provide a consistent way to identify different types of errors
 * and can be mapped to HTTP status codes and error messages.
 */
public enum ErrorCode {
    // Generic errors
    INTERNAL_ERROR(1000, "Internal server error"),
    VALIDATION_ERROR(1001, "Validation error"),

    // Domain-specific errors - Links
    LINK_NOT_FOUND(2000, "Link not found"),
    LINK_ALREADY_EXISTS(2001, "Link already exists"),
    INVALID_LINK_URL(2002, "Invalid link URL"),

    // Domain-specific errors - Users
    USER_NOT_FOUND(2100, "User not found"),
    USER_ALREADY_EXISTS(2101, "User already exists"),
    INVALID_USERNAME(2102, "Invalid username"),
    INVALID_EMAIL(2103, "Invalid email address"),
    WEAK_PASSWORD(2104, "Password does not meet security requirements"),
    USER_INACTIVE(2105, "User account is inactive"),

    // Authentication errors
    INVALID_CREDENTIALS(2200, "Invalid username or password"),
    TOKEN_EXPIRED(2201, "Authentication token has expired"),
    TOKEN_INVALID(2202, "Invalid authentication token"),
    TOKEN_REVOKED(2203, "Authentication token has been revoked"),
    UNAUTHORIZED_ACCESS(2204, "Unauthorized access"),
    INSUFFICIENT_PERMISSIONS(2205, "Insufficient permissions"),

    // Domain-specific errors - Content
    CONTENT_NOT_FOUND(2300, "Content not found"),
    CONTENT_DOWNLOAD_FAILED(2301, "Content download failed"),
    CONTENT_TOO_LARGE(2302, "Content size exceeds limit"),

    // Infrastructure errors
    DATABASE_ERROR(3000, "Database error"),
    NETWORK_ERROR(3001, "Network error");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
