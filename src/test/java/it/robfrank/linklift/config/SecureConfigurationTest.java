package it.robfrank.linklift.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class SecureConfigurationTest {

    @Test
    void getJwtSecret_shouldReturnValidSecret() {
        String secret = SecureConfiguration.getJwtSecret();

        assertThat(secret).isNotNull();
        assertThat(secret).isNotEmpty();
        assertThat(secret.trim().length()).isGreaterThanOrEqualTo(32); // Minimum 256 bits

        // Should not be the old hardcoded secrets
        assertThat(secret).isNotEqualTo("your-super-secret-jwt-key-change-in-production");
        assertThat(secret).isNotEqualTo("your-secret-key-here-change-in-production-must-be-256-bits-long");
    }

    @Test
    void getJwtSecret_shouldBeDeterministicInDevelopment() {
        // In development mode, the secret should be consistent
        String secret1 = SecureConfiguration.getJwtSecret();
        String secret2 = SecureConfiguration.getJwtSecret();

        assertThat(secret1).isEqualTo(secret2);
    }

    @Test
    void getConfigurationHints_shouldReturnHelpfulMessage() {
        String hints = SecureConfiguration.getConfigurationHints();

        assertThat(hints).isNotNull();
        assertThat(hints).isNotEmpty();
        assertThat(hints).containsAnyOf("LINKLIFT_JWT_SECRET", "development", "production");
    }

    @Test
    void validateSecret_shouldRejectShortSecrets() throws Exception {
        // Use reflection to test private method
        Method isValidSecret = SecureConfiguration.class.getDeclaredMethod("isValidSecret", String.class);
        isValidSecret.setAccessible(true);

        // Test short secret (less than 32 bytes)
        Boolean result = (Boolean) isValidSecret.invoke(null, "short");
        assertThat(result).isFalse();

        // Test null secret
        result = (Boolean) isValidSecret.invoke(null, (String) null);
        assertThat(result).isFalse();

        // Test empty secret
        result = (Boolean) isValidSecret.invoke(null, "");
        assertThat(result).isFalse();

        // Test valid secret (32+ characters)
        result = (Boolean) isValidSecret.invoke(null, "this-is-a-valid-secret-key-with-enough-length-for-256-bits");
        assertThat(result).isTrue();
    }

    @Test
    void isDevelopmentEnvironment_shouldDetectDevelopmentMode() throws Exception {
        // Use reflection to test private method
        Method isDevelopmentEnvironment = SecureConfiguration.class.getDeclaredMethod("isDevelopmentEnvironment");
        isDevelopmentEnvironment.setAccessible(true);

        Boolean result = (Boolean) isDevelopmentEnvironment.invoke(null);

        // Should be true when running from Maven (target/classes in classpath)
        assertThat(result).isTrue();
    }
}
