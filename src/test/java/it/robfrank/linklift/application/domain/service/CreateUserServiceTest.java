package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.UserAlreadyExistsException;
import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.User;
import it.robfrank.linklift.application.port.in.CreateUserCommand;
import it.robfrank.linklift.application.port.out.DomainEventPublisher;
import it.robfrank.linklift.application.port.out.LoadUserPort;
import it.robfrank.linklift.application.port.out.PasswordSecurityPort;
import it.robfrank.linklift.application.port.out.SaveUserPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateUserServiceTest {

  @Mock
  private LoadUserPort loadUserPort;

  @Mock
  private SaveUserPort saveUserPort;

  @Mock
  private PasswordSecurityPort passwordSecurityPort;

  @Mock
  private DomainEventPublisher eventPublisher;

  private CreateUserService createUserService;

  @BeforeEach
  void setUp() {
    createUserService = new CreateUserService(loadUserPort, saveUserPort, passwordSecurityPort, eventPublisher);
  }

  @Test
  void createUser_shouldCreateUserSuccessfully_whenValidData() {
    // Arrange
    CreateUserCommand command = new CreateUserCommand("testuser", "test@example.com", "StrongPassword123!", "John", "Doe");

    PasswordSecurityPort.PasswordHash passwordHash = new PasswordSecurityPort.PasswordHash("hashed-password", "salt");

    User savedUser = new User(
      "user-123",
      "testuser",
      "test@example.com",
      "hashed-password",
      "salt",
      null, // Will be set by constructor
      null,
      true,
      "John",
      "Doe",
      null
    );

    when(loadUserPort.existsByUsername("testuser")).thenReturn(false);
    when(loadUserPort.existsByEmail("test@example.com")).thenReturn(false);
    when(passwordSecurityPort.isPasswordStrong("StrongPassword123!")).thenReturn(true);
    when(passwordSecurityPort.hashPassword("StrongPassword123!")).thenReturn(passwordHash);
    when(saveUserPort.saveUser(any(User.class))).thenReturn(savedUser);

    // Act
    User result = createUserService.createUser(command);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.username()).isEqualTo("testuser");
    assertThat(result.email()).isEqualTo("test@example.com");
    assertThat(result.firstName()).isEqualTo("John");
    assertThat(result.lastName()).isEqualTo("Doe");
    assertThat(result.isActive()).isTrue();
    assertThat(result.passwordHash()).isNull(); // Should be stripped by toPublic()
    assertThat(result.salt()).isNull(); // Should be stripped by toPublic()

    // Verify user was saved with correct data
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(saveUserPort).saveUser(userCaptor.capture());

    User capturedUser = userCaptor.getValue();
    assertThat(capturedUser.id()).isNotNull();
    assertThat(capturedUser.username()).isEqualTo("testuser");
    assertThat(capturedUser.email()).isEqualTo("test@example.com");
    assertThat(capturedUser.passwordHash()).isEqualTo("hashed-password");
    assertThat(capturedUser.salt()).isEqualTo("salt");
    assertThat(capturedUser.isActive()).isTrue();

    // Verify event was published
    ArgumentCaptor<CreateUserService.UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CreateUserService.UserCreatedEvent.class);
    verify(eventPublisher).publish(eventCaptor.capture());

    CreateUserService.UserCreatedEvent capturedEvent = eventCaptor.getValue();
    assertThat(capturedEvent.userId()).isEqualTo(savedUser.id());
    assertThat(capturedEvent.username()).isEqualTo("testuser");
    assertThat(capturedEvent.email()).isEqualTo("test@example.com");
    assertThat(capturedEvent.getEventType()).isEqualTo("USER_CREATED");
  }

  @Test
  void createUser_shouldThrowValidationException_whenUsernameIsInvalid() {
    // Arrange
    CreateUserCommand command = new CreateUserCommand(
      "ab", // Too short
      "test@example.com",
      "StrongPassword123!",
      "John",
      "Doe"
    );

    // Act & Assert
    assertThatThrownBy(() -> createUserService.createUser(command))
      .isInstanceOf(ValidationException.class)
      .hasMessage("Username must be 3-30 characters and contain only letters, numbers, and underscores");

    verify(loadUserPort, never()).existsByUsername(anyString());
    verify(saveUserPort, never()).saveUser(any(User.class));
  }

  @Test
  void createUser_shouldThrowValidationException_whenUsernameHasInvalidCharacters() {
    // Arrange
    CreateUserCommand command = new CreateUserCommand(
      "test-user", // Contains hyphen
      "test@example.com",
      "StrongPassword123!",
      "John",
      "Doe"
    );

    // Act & Assert
    assertThatThrownBy(() -> createUserService.createUser(command))
      .isInstanceOf(ValidationException.class)
      .hasMessage("Username must be 3-30 characters and contain only letters, numbers, and underscores");
  }

  @Test
  void createUser_shouldThrowValidationException_whenEmailIsInvalid() {
    // Arrange
    CreateUserCommand command = new CreateUserCommand(
      "testuser",
      "invalid-email", // Invalid email format
      "StrongPassword123!",
      "John",
      "Doe"
    );

    // Act & Assert
    assertThatThrownBy(() -> createUserService.createUser(command)).isInstanceOf(ValidationException.class).hasMessage("Invalid email address format");
  }

  @Test
  void createUser_shouldThrowValidationException_whenPasswordIsWeak() {
    // Arrange
    CreateUserCommand command = new CreateUserCommand(
      "testuser",
      "test@example.com",
      "weak", // Weak password
      "John",
      "Doe"
    );

    when(passwordSecurityPort.isPasswordStrong("weak")).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> createUserService.createUser(command))
      .isInstanceOf(ValidationException.class)
      .hasMessage("Password does not meet security requirements");
  }

  @Test
  void createUser_shouldThrowValidationException_whenFirstNameTooLong() {
    // Arrange
    String longFirstName = "a".repeat(51); // 51 characters
    CreateUserCommand command = new CreateUserCommand("testuser", "test@example.com", "StrongPassword123!", longFirstName, "Doe");

    // Act & Assert
    assertThatThrownBy(() -> createUserService.createUser(command)).isInstanceOf(ValidationException.class).hasMessage("First name must be 1-50 characters");
  }

  @Test
  void createUser_shouldThrowValidationException_whenLastNameTooLong() {
    // Arrange
    String longLastName = "a".repeat(51); // 51 characters
    CreateUserCommand command = new CreateUserCommand("testuser", "test@example.com", "StrongPassword123!", "John", longLastName);

    // Act & Assert
    assertThatThrownBy(() -> createUserService.createUser(command)).isInstanceOf(ValidationException.class).hasMessage("Last name must be 1-50 characters");
  }

  @Test
  void createUser_shouldThrowUserAlreadyExistsException_whenUsernameExists() {
    // Arrange
    CreateUserCommand command = new CreateUserCommand("testuser", "test@example.com", "StrongPassword123!", "John", "Doe");

    when(loadUserPort.existsByUsername("testuser")).thenReturn(true);
    when(passwordSecurityPort.isPasswordStrong("StrongPassword123!")).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> createUserService.createUser(command))
      .isInstanceOf(UserAlreadyExistsException.class)
      .hasMessage("Username already exists: testuser");

    verify(saveUserPort, never()).saveUser(any(User.class));
  }

  @Test
  void createUser_shouldThrowUserAlreadyExistsException_whenEmailExists() {
    // Arrange
    CreateUserCommand command = new CreateUserCommand("testuser", "test@example.com", "StrongPassword123!", "John", "Doe");

    when(loadUserPort.existsByUsername("testuser")).thenReturn(false);
    when(loadUserPort.existsByEmail("test@example.com")).thenReturn(true);
    when(passwordSecurityPort.isPasswordStrong("StrongPassword123!")).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> createUserService.createUser(command))
      .isInstanceOf(UserAlreadyExistsException.class)
      .hasMessage("Email already exists: test@example.com");

    verify(saveUserPort, never()).saveUser(any(User.class));
  }

  @Test
  void createUser_shouldSucceedWithNullOptionalFields() {
    // Arrange
    CreateUserCommand command = new CreateUserCommand(
      "testuser",
      "test@example.com",
      "StrongPassword123!",
      null, // No first name
      null // No last name
    );

    PasswordSecurityPort.PasswordHash passwordHash = new PasswordSecurityPort.PasswordHash("hashed-password", "salt");

    User savedUser = new User("user-123", "testuser", "test@example.com", "hashed-password", "salt", null, null, true, null, null, null);

    when(loadUserPort.existsByUsername("testuser")).thenReturn(false);
    when(loadUserPort.existsByEmail("test@example.com")).thenReturn(false);
    when(passwordSecurityPort.isPasswordStrong("StrongPassword123!")).thenReturn(true);
    when(passwordSecurityPort.hashPassword("StrongPassword123!")).thenReturn(passwordHash);
    when(saveUserPort.saveUser(any(User.class))).thenReturn(savedUser);

    // Act
    User result = createUserService.createUser(command);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.firstName()).isNull();
    assertThat(result.lastName()).isNull();

    verify(saveUserPort).saveUser(any(User.class));
    verify(eventPublisher).publish(any(CreateUserService.UserCreatedEvent.class));
  }

  @Test
  void createUser_shouldSucceedWithEmptyOptionalFields() {
    // Arrange
    CreateUserCommand command = new CreateUserCommand(
      "testuser",
      "test@example.com",
      "StrongPassword123!",
      "", // Empty first name
      "" // Empty last name
    );

    // Act & Assert
    assertThatThrownBy(() -> createUserService.createUser(command)).isInstanceOf(ValidationException.class).hasMessage("First name must be 1-50 characters");
  }
}
