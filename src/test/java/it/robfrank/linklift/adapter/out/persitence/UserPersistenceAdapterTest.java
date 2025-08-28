package it.robfrank.linklift.adapter.out.persitence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserPersistenceAdapterTest {

    @Mock
    private ArcadeUserRepository userRepository;

    private UserPersistenceAdapter userPersistenceAdapter;

    @BeforeEach
    void setUp() {
        userPersistenceAdapter = new UserPersistenceAdapter(userRepository);
    }

    @Test
    void findUserById_shouldCallRepository_andReturnResult() {
        // Arrange
        String userId = "user-123";
        User user = createTestUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userPersistenceAdapter.findUserById(userId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user);
        verify(userRepository).findById(userId);
    }

    @Test
    void findUserById_shouldReturnEmpty_whenUserNotFound() {
        // Arrange
        String userId = "nonexistent-user";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userPersistenceAdapter.findUserById(userId);

        // Assert
        assertThat(result).isEmpty();
        verify(userRepository).findById(userId);
    }

    @Test
    void findUserByUsername_shouldCallRepository_andReturnResult() {
        // Arrange
        String username = "testuser";
        User user = createTestUser("user-123");
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userPersistenceAdapter.findUserByUsername(username);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user);
        verify(userRepository).findByUsername(username);
    }

    @Test
    void findUserByUsername_shouldReturnEmpty_whenUserNotFound() {
        // Arrange
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userPersistenceAdapter.findUserByUsername(username);

        // Assert
        assertThat(result).isEmpty();
        verify(userRepository).findByUsername(username);
    }

    @Test
    void findUserByEmail_shouldCallRepository_andReturnResult() {
        // Arrange
        String email = "test@example.com";
        User user = createTestUser("user-123");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userPersistenceAdapter.findUserByEmail(email);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void findUserByEmail_shouldReturnEmpty_whenUserNotFound() {
        // Arrange
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userPersistenceAdapter.findUserByEmail(email);

        // Assert
        assertThat(result).isEmpty();
        verify(userRepository).findByEmail(email);
    }

    @Test
    void existsByUsername_shouldCallRepository_andReturnResult() {
        // Arrange
        String username = "testuser";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // Act
        boolean result = userPersistenceAdapter.existsByUsername(username);

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).existsByUsername(username);
    }

    @Test
    void existsByUsername_shouldReturnFalse_whenUserDoesNotExist() {
        // Arrange
        String username = "nonexistent";
        when(userRepository.existsByUsername(username)).thenReturn(false);

        // Act
        boolean result = userPersistenceAdapter.existsByUsername(username);

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).existsByUsername(username);
    }

    @Test
    void existsByEmail_shouldCallRepository_andReturnResult() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act
        boolean result = userPersistenceAdapter.existsByEmail(email);

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    void existsByEmail_shouldReturnFalse_whenEmailDoesNotExist() {
        // Arrange
        String email = "nonexistent@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);

        // Act
        boolean result = userPersistenceAdapter.existsByEmail(email);

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    void saveUser_shouldCallRepository_andReturnResult() {
        // Arrange
        User user = createTestUser("user-123");
        User savedUser = createTestUser("user-123");
        when(userRepository.save(user)).thenReturn(savedUser);

        // Act
        User result = userPersistenceAdapter.saveUser(user);

        // Assert
        assertThat(result).isEqualTo(savedUser);
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_shouldCallRepository_andReturnResult() {
        // Arrange
        User user = createTestUser("user-123");
        User updatedUser = user.withActiveStatus(false);
        when(userRepository.update(user)).thenReturn(updatedUser);

        // Act
        User result = userPersistenceAdapter.updateUser(user);

        // Assert
        assertThat(result).isEqualTo(updatedUser);
        verify(userRepository).update(user);
    }

    @Test
    void deleteUser_shouldCallRepository() {
        // Arrange
        String userId = "user-123";

        // Act
        userPersistenceAdapter.deleteUser(userId);

        // Assert
        verify(userRepository).deactivate(userId);
    }

    private User createTestUser(String userId) {
        return new User(
            userId,
            "testuser",
            "test@example.com",
            "hashed-password",
            "salt",
            LocalDateTime.now(),
            null,
            true,
            "John",
            "Doe",
            null
        );
    }
}
