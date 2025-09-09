package it.robfrank.linklift.application.port.in;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Command for user authentication.
 * Supports both username and email login.
 */
public record AuthenticateUserCommand(
    @JsonProperty("loginIdentifier") String loginIdentifier,
    @JsonProperty("password") String password,
    @JsonProperty("ipAddress") String ipAddress,
    @JsonProperty("userAgent") String userAgent,
    @JsonProperty("rememberMe") boolean rememberMe
) {
    public AuthenticateUserCommand {
        // Validation
        if (loginIdentifier == null || loginIdentifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Login identifier (username or email) is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        // Normalize login identifier
        loginIdentifier = loginIdentifier.trim().toLowerCase();
    }

    /**
     * Determines if the login identifier is an email address.
     */
    public boolean isEmailLogin() {
        return loginIdentifier.contains("@");
    }
}
