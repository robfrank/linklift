package it.robfrank.linklift.adapter.out.persitence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcadedb.Constants;
import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.model.User;
import it.robfrank.linklift.config.DatabaseInitializer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ArcadeUserRepositoryTest {

    @Container
    private static final GenericContainer arcadeDBContainer = new GenericContainer("arcadedata/arcadedb:" + Constants.getRawVersion())
            .withExposedPorts(2480)
            .withStartupTimeout(Duration.ofSeconds(90))
            .withEnv("JAVA_OPTS", """
                    -Darcadedb.dateImplementation=java.time.LocalDate
                    -Darcadedb.dateTimeImplementation=java.time.LocalDateTime
                    -Darcadedb.server.rootPassword=playwithdata
                    -Darcadedb.server.plugins=Postgres:com.arcadedb.postgres.PostgresProtocolPlugin
                    """)
            .waitingFor(Wait.forHttp("/api/v1/ready").forPort(2480).forStatusCode(204));

    private RemoteDatabase database;
    private ArcadeUserRepository userRepository;

    @BeforeAll
    static void setup() {
        new DatabaseInitializer(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "root",
                "playwithdata").initializeDatabase();
    }

    @BeforeEach
    void setUp() {
        database = new RemoteDatabase(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "linklift", "root",
                "playwithdata");
        userRepository = new ArcadeUserRepository(database, new UserMapper());

        // Clean up existing test data
        database.command("sql", "DELETE FROM User WHERE username LIKE 'test%'");
    }

    @Test
    void save_shouldPersistUser() {
        // Arrange
        User testUser = createTestUser();

        // Act
        User savedUser = userRepository.save(testUser);

        // Assert
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.id()).isEqualTo(testUser.id());
        assertThat(savedUser.username()).isEqualTo(testUser.username());
        assertThat(savedUser.email()).isEqualTo(testUser.email());
        assertThat(savedUser.passwordHash()).isEqualTo(testUser.passwordHash());
        assertThat(savedUser.salt()).isEqualTo(testUser.salt());
        assertThat(savedUser.isActive()).isEqualTo(testUser.isActive());
        assertThat(savedUser.firstName()).isEqualTo(testUser.firstName());
        assertThat(savedUser.lastName()).isEqualTo(testUser.lastName());
    }

    @Test
    void findById_shouldReturnUser_whenUserExists() {
        // Arrange
        User testUser = createTestUser();
        userRepository.save(testUser);

        // Act
        Optional<User> foundUser = userRepository.findById(testUser.id());

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().id()).isEqualTo(testUser.id());
        assertThat(foundUser.get().username()).isEqualTo(testUser.username());
        assertThat(foundUser.get().email()).isEqualTo(testUser.email());
    }

    @Test
    void findById_shouldReturnEmpty_whenUserDoesNotExist() {
        // Act
        Optional<User> foundUser = userRepository.findById("nonexistent-id");

        // Assert
        assertThat(foundUser).isEmpty();
    }

    @Test
    void findByUsername_shouldReturnUser_whenUserExists() {
        // Arrange
        User testUser = createTestUser();
        userRepository.save(testUser);

        // Act
        Optional<User> foundUser = userRepository.findByUsername(testUser.username());

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().username()).isEqualTo(testUser.username());
        assertThat(foundUser.get().id()).isEqualTo(testUser.id());
    }

    @Test
    void findByUsername_shouldReturnEmpty_whenUserDoesNotExist() {
        // Act
        Optional<User> foundUser = userRepository.findByUsername("nonexistent-user");

        // Assert
        assertThat(foundUser).isEmpty();
    }

    @Test
    void findByEmail_shouldReturnUser_whenUserExists() {
        // Arrange
        User testUser = createTestUser();
        userRepository.save(testUser);

        // Act
        Optional<User> foundUser = userRepository.findByEmail(testUser.email());

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().email()).isEqualTo(testUser.email());
        assertThat(foundUser.get().id()).isEqualTo(testUser.id());
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenUserDoesNotExist() {
        // Act
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertThat(foundUser).isEmpty();
    }

    @Test
    void existsByUsername_shouldReturnTrue_whenUserExists() {
        // Arrange
        User testUser = createTestUser();
        userRepository.save(testUser);

        // Act
        boolean exists = userRepository.existsByUsername(testUser.username());

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUsername_shouldReturnFalse_whenUserDoesNotExist() {
        // Act
        boolean exists = userRepository.existsByUsername("nonexistent-user");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenUserExists() {
        // Arrange
        User testUser = createTestUser();
        userRepository.save(testUser);

        // Act
        boolean exists = userRepository.existsByEmail(testUser.email());

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnFalse_whenUserDoesNotExist() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void update_shouldModifyUser_whenUserExists() {
        // Arrange
        User testUser = createTestUser();
        User savedUser = userRepository.save(testUser);

        User updatedUser = savedUser.withActiveStatus(false)
                .withLastLogin(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        // Act
        User result = userRepository.update(updatedUser);

        // Assert
        assertThat(result.isActive()).isFalse();
        assertThat(result.lastLoginAt()).isNotNull();
        assertThat(result.updatedAt()).isNotNull();

        // Verify persistence
        Optional<User> persistedUser = userRepository.findById(result.id());
        assertThat(persistedUser).isPresent();
        assertThat(persistedUser.get().isActive()).isFalse();
        assertThat(persistedUser.get().lastLoginAt()).isNotNull();
    }

    @Test
    void update_shouldThrowException_whenUserDoesNotExist() {
        // Arrange
        User nonExistentUser = createTestUser();

        // Act & Assert
        assertThatThrownBy(() -> userRepository.update(nonExistentUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found with id: " + nonExistentUser.id());
    }

    @Test
    void deleteById_shouldRemoveUser_whenUserExists() {
        // Arrange
        User testUser = createTestUser();
        userRepository.save(testUser);

        // Verify user exists
        assertThat(userRepository.findById(testUser.id())).isPresent();

        // Act
        userRepository.deleteById(testUser.id());

        // Assert
        assertThat(userRepository.findById(testUser.id())).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        // Arrange
        User user1 = createTestUser("testuser1", "test1@example.com");
        User user2 = createTestUser("testuser2", "test2@example.com");

        userRepository.save(user1);
        userRepository.save(user2);

        // Act
        List<User> allUsers = userRepository.findAll();

        // Assert
        assertThat(allUsers)
                .hasSize(3)
                .extracting(User::username)
                .containsExactlyInAnyOrder("system", "testuser1", "testuser2");
    }

    @Test
    void findAll_shouldReturnEmptyList_whenNoUsers() {
        // Act
        List<User> allUsers = userRepository.findAll();

        // Assert
        assertThat(allUsers)
                .hasSize(1)
                .extracting(User::username)
                .first()
                .isEqualTo("system");
    }

    @Test
    void save_shouldHandleUserWithNullOptionalFields() {
        // Arrange
        User userWithNulls = new User(
                UUID.randomUUID().toString(),
                "testnulls",
                "testnulls@example.com",
                "hashed-password",
                "salt",
                LocalDateTime.now(),
                null,
                true,
                null, // null firstName
                null, // null lastName
                null  // null lastLoginAt
        );

        // Act
        User savedUser = userRepository.save(userWithNulls);

        // Assert
        assertThat(savedUser.firstName()).isNull();
        assertThat(savedUser.lastName()).isNull();
        assertThat(savedUser.lastLoginAt()).isNull();

        // Verify persistence
        Optional<User> persistedUser = userRepository.findById(savedUser.id());
        assertThat(persistedUser).isPresent();
        assertThat(persistedUser.get().firstName()).isNull();
        assertThat(persistedUser.get().lastName()).isNull();
    }

    @Test
    void timestampFields_shouldBeTruncatedToSeconds() {
        // Arrange
        LocalDateTime timestampWithNanos = LocalDateTime.now().withNano(123456789);
        User testUser = new User(
                UUID.randomUUID().toString(),
                "testtime",
                "testtime@example.com",
                "hashed-password",
                "salt",
                timestampWithNanos,
                timestampWithNanos,
                true,
                "John",
                "Doe",
                timestampWithNanos
        );

        // Act
        User savedUser = userRepository.save(testUser);

        // Assert
        assertThat(savedUser.createdAt()).isEqualTo(timestampWithNanos.truncatedTo(ChronoUnit.SECONDS));
        assertThat(savedUser.updatedAt()).isEqualTo(timestampWithNanos.truncatedTo(ChronoUnit.SECONDS));
        assertThat(savedUser.lastLoginAt()).isEqualTo(timestampWithNanos.truncatedTo(ChronoUnit.SECONDS));
    }

    private User createTestUser() {
        return createTestUser("testuser", "test@example.com");
    }

    private User createTestUser(String username, String email) {
        return new User(
                UUID.randomUUID().toString(),
                username,
                email,
                "hashed-password",
                "salt",
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                null,
                true,
                "John",
                "Doe",
                null
        );
    }
}
