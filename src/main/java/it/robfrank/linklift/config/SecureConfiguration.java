package it.robfrank.linklift.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secure configuration utility for managing sensitive configuration values.
 * Implements security best practices for externalized configuration.
 */
public final class SecureConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SecureConfiguration.class);

    // Environment variable names
    private static final String JWT_SECRET_ENV = "LINKLIFT_JWT_SECRET";
    private static final String JWT_SECRET_FILE_ENV = "LINKLIFT_JWT_SECRET_FILE";

    // Development fallback - WARNING: Never use in production
    private static final String DEVELOPMENT_JWT_SECRET = generateSecureDevSecret();

    private SecureConfiguration() {
        // Utility class - prevent instantiation
    }

    /**
     * Retrieves the JWT secret from secure configuration sources.
     * Priority order:
     * 1. Environment variable LINKLIFT_JWT_SECRET
     * 2. File path from LINKLIFT_JWT_SECRET_FILE environment variable
     * 3. Development fallback (only for development environments)
     *
     * @return The JWT secret key (minimum 256 bits)
     * @throws IllegalStateException if no secure secret is available in production
     */
    public static String getJwtSecret() {
        // Try environment variable first
        String secret = System.getenv(JWT_SECRET_ENV);
        if (isValidSecret(secret)) {
            logger.debug("JWT secret loaded from environment variable");
            return secret;
        }

        // Try secret from file
        String secretFile = System.getenv(JWT_SECRET_FILE_ENV);
        if (secretFile != null && !secretFile.trim().isEmpty()) {
            try {
                secret = java.nio.file.Files.readString(java.nio.file.Paths.get(secretFile.trim()), StandardCharsets.UTF_8).trim();
                if (isValidSecret(secret)) {
                    logger.debug("JWT secret loaded from file: {}", secretFile);
                    return secret;
                }
            } catch (Exception e) {
                logger.warn("Failed to read JWT secret from file: {}", secretFile, e);
            }
        }

        // Development fallback
        if (isDevelopmentEnvironment()) {
            logger.warn("Using development JWT secret. DO NOT USE IN PRODUCTION!");
            logger.warn("Set {} environment variable or {} for production deployment", JWT_SECRET_ENV, JWT_SECRET_FILE_ENV);
            return DEVELOPMENT_JWT_SECRET;
        }

        // Production without proper secret configuration
        throw new IllegalStateException(
            String.format("JWT secret not configured. Set environment variable '%s' or '%s' with a secure 256-bit secret", JWT_SECRET_ENV, JWT_SECRET_FILE_ENV)
        );
    }

    /**
     * Validates that the secret meets minimum security requirements.
     */
    private static boolean isValidSecret(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            return false;
        }

        // Minimum 256 bits (32 bytes) when UTF-8 encoded
        byte[] secretBytes = secret.trim().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            logger.warn("JWT secret is too short. Minimum 256 bits (32 characters) required, got {} bytes", secretBytes.length);
            return false;
        }

        return true;
    }

    /**
     * Detects if running in development environment.
     */
    private static boolean isDevelopmentEnvironment() {
        String env = System.getenv("ENVIRONMENT");
        if (env != null) {
            return env.toLowerCase().contains("dev") || env.toLowerCase().contains("local");
        }

        String profile = System.getProperty("spring.profiles.active", "");
        if (profile.toLowerCase().contains("dev") || profile.toLowerCase().contains("local")) {
            return true;
        }

        // Check if running from IDE or test environment
        String javaClassPath = System.getProperty("java.class.path", "");
        return javaClassPath.contains("target/classes") || javaClassPath.contains("target/test-classes");
    }

    /**
     * Generates a secure development secret that's different per installation.
     * This prevents accidental use of the same development secret across environments.
     */
    private static String generateSecureDevSecret() {
        try {
            // Create a deterministic but installation-specific secret
            String systemInfo = System.getProperty("user.name", "linklift") + System.getProperty("java.home", "unknown") + "linklift-dev-secret-v1";

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(systemInfo.getBytes(StandardCharsets.UTF_8));

            // Generate additional entropy for better security
            SecureRandom random = new SecureRandom();
            byte[] entropy = new byte[16];
            random.nextBytes(entropy);

            // Combine hash and entropy
            byte[] combined = new byte[hash.length + entropy.length];
            System.arraycopy(hash, 0, combined, 0, hash.length);
            System.arraycopy(entropy, 0, combined, hash.length, entropy.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to a static secure secret if SHA-256 is not available
            return "linklift-development-fallback-secret-do-not-use-in-production-minimum-32-chars-required-for-256bit-security";
        }
    }

    /**
     * Gets environment-specific configuration hints for logging.
     */
    public static String getConfigurationHints() {
        if (isDevelopmentEnvironment()) {
            return String.format(
                "Development environment detected. For production, set '%s' environment variable with a secure 256-bit secret",
                JWT_SECRET_ENV
            );
        } else {
            return String.format("Production environment. Ensure '%s' or '%s' is configured with a secure secret", JWT_SECRET_ENV, JWT_SECRET_FILE_ENV);
        }
    }
}
