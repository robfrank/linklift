package it.robfrank.linklift.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void constructor_shouldSetAllPropertiesCorrectly() {
        // Arrange
        String id = "user-123";
        String username = "testuser";
        String email = "test@example.com";
        String passwordHash = "hashed-password";
        String salt = "salt-value";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean isActive = true;
        String firstName = "John";
        String lastName = "Doe";
        LocalDateTime lastLoginAt = LocalDateTime.now();

        // Act
        User user = new User(id, username, email, passwordHash, salt, createdAt, updatedAt, isActive, firstName, lastName, lastLoginAt);

        // Assert
        assertThat(user.id()).isEqualTo(id);
        assertThat(user.username()).isEqualTo(username);
        assertThat(user.email()).isEqualTo(email);
        assertThat(user.passwordHash()).isEqualTo(passwordHash);
        assertThat(user.salt()).isEqualTo(salt);
        assertThat(user.createdAt()).isEqualTo(createdAt.truncatedTo(ChronoUnit.SECONDS));
        assertThat(user.updatedAt()).isEqualTo(updatedAt.truncatedTo(ChronoUnit.SECONDS));
        assertThat(user.isActive()).isEqualTo(isActive);
        assertThat(user.firstName()).isEqualTo(firstName);
        assertThat(user.lastName()).isEqualTo(lastName);
        assertThat(user.lastLoginAt()).isEqualTo(lastLoginAt.truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void constructor_shouldSetCurrentTimestampWhenCreatedAtIsNull() {
        // Arrange
        LocalDateTime before = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        // Act
        User user = new User("id", "username", "email", "hash", "salt", null, null, true, "John", "Doe", null);

        LocalDateTime after = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        // Assert
        assertThat(user.createdAt()).isNotNull();
        LocalDateTime createdAt = user.createdAt().truncatedTo(ChronoUnit.SECONDS);
        assertThat(createdAt).isAfterOrEqualTo(before);
        assertThat(createdAt).isBeforeOrEqualTo(after);
    }

    @Test
    void constructor_shouldTruncateTimestampsToSeconds() {
        // Arrange
        LocalDateTime timestampWithNanos = LocalDateTime.now().withNano(123456789);
        LocalDateTime expectedTruncated = timestampWithNanos.truncatedTo(ChronoUnit.SECONDS);

        // Act
        User user = new User("id", "username", "email", "hash", "salt", timestampWithNanos, timestampWithNanos, true, "John", "Doe", timestampWithNanos);

        // Assert
        assertThat(user.createdAt()).isEqualTo(expectedTruncated);
        assertThat(user.updatedAt()).isEqualTo(expectedTruncated);
        assertThat(user.lastLoginAt()).isEqualTo(expectedTruncated);
    }

    @Test
    void withLastLogin_shouldUpdateLastLoginAndUpdatedAt() {
        // Arrange
        User originalUser = new User("id", "username", "email", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);
        LocalDateTime loginTime = LocalDateTime.now().plusHours(1);

        // Act
        User updatedUser = originalUser.withLastLogin(loginTime);

        // Assert
        assertThat(updatedUser.lastLoginAt()).isEqualTo(loginTime.truncatedTo(ChronoUnit.SECONDS));
        assertThat(updatedUser.updatedAt()).isNotNull();
        assertThat(updatedUser.id()).isEqualTo(originalUser.id());
        assertThat(updatedUser.username()).isEqualTo(originalUser.username());
        assertThat(updatedUser.email()).isEqualTo(originalUser.email());
    }

    @Test
    void withActiveStatus_shouldUpdateActiveStatusAndUpdatedAt() {
        // Arrange
        User originalUser = new User("id", "username", "email", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);

        // Act
        User deactivatedUser = originalUser.withActiveStatus(false);

        // Assert
        assertThat(deactivatedUser.isActive()).isFalse();
        assertThat(deactivatedUser.updatedAt()).isNotNull();
        assertThat(deactivatedUser.id()).isEqualTo(originalUser.id());
        assertThat(deactivatedUser.username()).isEqualTo(originalUser.username());
    }

    @Test
    void withPassword_shouldUpdatePasswordHashAndSaltAndUpdatedAt() {
        // Arrange
        User originalUser = new User("id", "username", "email", "old-hash", "old-salt", LocalDateTime.now(), null, true, "John", "Doe", null);
        String newHash = "new-hash";
        String newSalt = "new-salt";

        // Act
        User updatedUser = originalUser.withPassword(newHash, newSalt);

        // Assert
        assertThat(updatedUser.passwordHash()).isEqualTo(newHash);
        assertThat(updatedUser.salt()).isEqualTo(newSalt);
        assertThat(updatedUser.updatedAt()).isNotNull();
        assertThat(updatedUser.id()).isEqualTo(originalUser.id());
        assertThat(updatedUser.username()).isEqualTo(originalUser.username());
    }

    @Test
    void toPublic_shouldRemoveSensitiveInformation() {
        // Arrange
        User user = new User("id", "username", "email", "hash", "salt", LocalDateTime.now(), LocalDateTime.now(), true, "John", "Doe", LocalDateTime.now());

        // Act
        User publicUser = user.toPublic();

        // Assert
        assertThat(publicUser.passwordHash()).isNull();
        assertThat(publicUser.salt()).isNull();
        assertThat(publicUser.id()).isEqualTo(user.id());
        assertThat(publicUser.username()).isEqualTo(user.username());
        assertThat(publicUser.email()).isEqualTo(user.email());
        assertThat(publicUser.firstName()).isEqualTo(user.firstName());
        assertThat(publicUser.lastName()).isEqualTo(user.lastName());
    }

    @Test
    void getFullName_shouldReturnUsernameWhenNamesAreNull() {
        // Arrange
        User user = new User("id", "testuser", "email", "hash", "salt", LocalDateTime.now(), null, true, null, null, null);

        // Act
        String fullName = user.getFullName();

        // Assert
        assertThat(fullName).isEqualTo("testuser");
    }

    @Test
    void getFullName_shouldReturnLastNameWhenFirstNameIsNull() {
        // Arrange
        User user = new User("id", "testuser", "email", "hash", "salt", LocalDateTime.now(), null, true, null, "Doe", null);

        // Act
        String fullName = user.getFullName();

        // Assert
        assertThat(fullName).isEqualTo("Doe");
    }

    @Test
    void getFullName_shouldReturnFirstNameWhenLastNameIsNull() {
        // Arrange
        User user = new User("id", "testuser", "email", "hash", "salt", LocalDateTime.now(), null, true, "John", null, null);

        // Act
        String fullName = user.getFullName();

        // Assert
        assertThat(fullName).isEqualTo("John");
    }

    @Test
    void getFullName_shouldReturnFullNameWhenBothNamesPresent() {
        // Arrange
        User user = new User("id", "testuser", "email", "hash", "salt", LocalDateTime.now(), null, true, "John", "Doe", null);

        // Act
        String fullName = user.getFullName();

        // Assert
        assertThat(fullName).isEqualTo("John Doe");
    }

    @Test
    void equals_shouldReturnTrue_whenUsersHaveSameData() {
        // Arrange
        LocalDateTime timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        User user1 = new User("id", "username", "email", "hash", "salt", timestamp, timestamp, true, "John", "Doe", timestamp);
        User user2 = new User("id", "username", "email", "hash", "salt", timestamp, timestamp, true, "John", "Doe", timestamp);

        // Assert
        assertThat(user1).isEqualTo(user2);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    void equals_shouldReturnFalse_whenUsersHaveDifferentData() {
        // Arrange
        LocalDateTime timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        User user1 = new User("id1", "username", "email", "hash", "salt", timestamp, timestamp, true, "John", "Doe", timestamp);
        User user2 = new User("id2", "username", "email", "hash", "salt", timestamp, timestamp, true, "John", "Doe", timestamp);

        // Assert
        assertThat(user1).isNotEqualTo(user2);
    }
}
