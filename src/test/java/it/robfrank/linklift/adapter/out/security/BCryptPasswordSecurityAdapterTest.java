package it.robfrank.linklift.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import it.robfrank.linklift.application.port.out.PasswordSecurityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BCryptPasswordSecurityAdapterTest {

    private BCryptPasswordSecurityAdapter passwordSecurityAdapter;

    @BeforeEach
    void setUp() {
        passwordSecurityAdapter = new BCryptPasswordSecurityAdapter();
    }

    @Test
    void hashPassword_shouldReturnPasswordHashWithSalt() {
        // Arrange
        String plainPassword = "TestPassword123!";

        // Act
        PasswordSecurityPort.PasswordHash result = passwordSecurityAdapter.hashPassword(plainPassword);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.hash()).isNotNull();
        assertThat(result.hash()).isNotEmpty();
        assertThat(result.salt()).isNotNull();
        assertThat(result.salt()).isNotEmpty();
        assertThat(result.hash()).isNotEqualTo(plainPassword);
    }

    @Test
    void hashPassword_shouldGenerateDifferentHashesForSamePassword() {
        // Arrange
        String plainPassword = "TestPassword123!";

        // Act
        PasswordSecurityPort.PasswordHash hash1 = passwordSecurityAdapter.hashPassword(plainPassword);
        PasswordSecurityPort.PasswordHash hash2 = passwordSecurityAdapter.hashPassword(plainPassword);

        // Assert
        assertThat(hash1.hash()).isNotEqualTo(hash2.hash());
        assertThat(hash1.salt()).isNotEqualTo(hash2.salt());
    }

    @Test
    void verifyPassword_shouldReturnTrue_whenPasswordMatches() {
        // Arrange
        String plainPassword = "TestPassword123!";
        PasswordSecurityPort.PasswordHash passwordHash = passwordSecurityAdapter.hashPassword(plainPassword);

        // Act
        boolean result = passwordSecurityAdapter.verifyPassword(
            plainPassword,
            passwordHash.hash(),
            passwordHash.salt()
        );

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void verifyPassword_shouldReturnFalse_whenPasswordDoesNotMatch() {
        // Arrange
        String correctPassword = "TestPassword123!";
        String wrongPassword = "WrongPassword456!";
        PasswordSecurityPort.PasswordHash passwordHash = passwordSecurityAdapter.hashPassword(correctPassword);

        // Act
        boolean result = passwordSecurityAdapter.verifyPassword(
            wrongPassword,
            passwordHash.hash(),
            passwordHash.salt()
        );

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void verifyPassword_shouldReturnFalse_whenPlainPasswordIsNull() {
        // Arrange
        PasswordSecurityPort.PasswordHash passwordHash = passwordSecurityAdapter.hashPassword("TestPassword123!");

        // Act
        boolean result = passwordSecurityAdapter.verifyPassword(
            null,
            passwordHash.hash(),
            passwordHash.salt()
        );

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void verifyPassword_shouldReturnFalse_whenStoredHashIsNull() {
        // Act
        boolean result = passwordSecurityAdapter.verifyPassword(
            "TestPassword123!",
            null,
            "salt"
        );

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void verifyPassword_shouldReturnFalse_whenSaltIsNull() {
        // Arrange
        PasswordSecurityPort.PasswordHash passwordHash = passwordSecurityAdapter.hashPassword("TestPassword123!");

        // Act
        boolean result = passwordSecurityAdapter.verifyPassword(
            "TestPassword123!",
            passwordHash.hash(),
            null
        );

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void verifyPassword_shouldReturnFalse_whenWrongSalt() {
        // Arrange
        String plainPassword = "TestPassword123!";
        PasswordSecurityPort.PasswordHash passwordHash = passwordSecurityAdapter.hashPassword(plainPassword);
        String wrongSalt = "wrong-salt";

        // Act
        boolean result = passwordSecurityAdapter.verifyPassword(
            plainPassword,
            passwordHash.hash(),
            wrongSalt
        );

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isPasswordStrong_shouldReturnTrue_whenPasswordMeetsRequirements() {
        // Arrange
        String strongPassword = "StrongPass123!";

        // Act
        boolean result = passwordSecurityAdapter.isPasswordStrong(strongPassword);

        // Assert
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Password123!",  // Upper, lower, digit, special
        "password123!",  // Lower, digit, special (missing upper)
        "PASSWORD123!",  // Upper, digit, special (missing lower)
        "Password!",     // Upper, lower, special (missing digit)
        "Password123"    // Upper, lower, digit (missing special)
    })
    void isPasswordStrong_shouldReturnTrue_whenPasswordHasAtLeastThreeCharacterTypes(String password) {
        // Act
        boolean result = passwordSecurityAdapter.isPasswordStrong(password);

        // Assert
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "password",      // Only lowercase
        "PASSWORD",      // Only uppercase
        "12345678",      // Only digits
        "!@#$%^&*",      // Only special characters
        "password123",   // Only lowercase and digits
        "PASSWORD!",     // Only uppercase and special
    })
    void isPasswordStrong_shouldReturnFalse_whenPasswordHasLessThanThreeCharacterTypes(String password) {
        // Act
        boolean result = passwordSecurityAdapter.isPasswordStrong(password);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isPasswordStrong_shouldReturnFalse_whenPasswordTooShort() {
        // Arrange
        String shortPassword = "Pass1!"; // 6 characters, minimum is 8

        // Act
        boolean result = passwordSecurityAdapter.isPasswordStrong(shortPassword);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isPasswordStrong_shouldReturnFalse_whenPasswordTooLong() {
        // Arrange
        String longPassword = "A".repeat(129) + "1!"; // 131 characters, maximum is 128

        // Act
        boolean result = passwordSecurityAdapter.isPasswordStrong(longPassword);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isPasswordStrong_shouldReturnFalse_whenPasswordIsNull() {
        // Act
        boolean result = passwordSecurityAdapter.isPasswordStrong(null);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isPasswordStrong_shouldReturnTrue_whenPasswordIsAtMinimumLength() {
        // Arrange
        String minimumPassword = "Pass123!"; // 8 characters

        // Act
        boolean result = passwordSecurityAdapter.isPasswordStrong(minimumPassword);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isPasswordStrong_shouldReturnTrue_whenPasswordIsAtMaximumLength() {
        // Arrange
        String maximumPassword = "A".repeat(125) + "1!a"; // 128 characters

        // Act
        boolean result = passwordSecurityAdapter.isPasswordStrong(maximumPassword);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void passwordHashAndVerify_shouldWorkWithSpecialCharacters() {
        // Arrange - shorter password to account for 44-byte salt (32+44=76 > 72 BCrypt limit)
        String passwordWithSpecialChars = "Pass@#$%^&*()_+-=!";

        // Act
        PasswordSecurityPort.PasswordHash hash = passwordSecurityAdapter.hashPassword(passwordWithSpecialChars);
        boolean verified = passwordSecurityAdapter.verifyPassword(
            passwordWithSpecialChars,
            hash.hash(),
            hash.salt()
        );

        // Assert
        assertThat(verified).isTrue();
    }

    @Test
    void passwordHashAndVerify_shouldWorkWithUnicodeCharacters() {
        // Arrange
        String passwordWithUnicode = "Tëst€∆Ω123!";

        // Act
        PasswordSecurityPort.PasswordHash hash = passwordSecurityAdapter.hashPassword(passwordWithUnicode);
        boolean verified = passwordSecurityAdapter.verifyPassword(
            passwordWithUnicode,
            hash.hash(),
            hash.salt()
        );

        // Assert
        assertThat(verified).isTrue();
    }

    @Test
    void verifyPassword_shouldHandleCorruptedHash() {
        // Arrange
        String plainPassword = "TestPassword123!";
        String corruptedHash = "corrupted-hash-value";
        String salt = "valid-salt";

        // Act
        boolean result = passwordSecurityAdapter.verifyPassword(plainPassword, corruptedHash, salt);

        // Assert
        assertThat(result).isFalse();
    }
}
