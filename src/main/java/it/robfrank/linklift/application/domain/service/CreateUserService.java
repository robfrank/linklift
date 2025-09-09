package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.UserAlreadyExistsException;
import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.User;
import it.robfrank.linklift.application.port.in.CreateUserCommand;
import it.robfrank.linklift.application.port.in.CreateUserUseCase;
import it.robfrank.linklift.application.port.out.DomainEventPublisher;
import it.robfrank.linklift.application.port.out.LoadUserPort;
import it.robfrank.linklift.application.port.out.PasswordSecurityPort;
import it.robfrank.linklift.application.port.out.SaveUserPort;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Domain service implementing user creation use case.
 * Handles user registration with proper validation and security.
 */
public class CreateUserService implements CreateUserUseCase {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordSecurityPort passwordSecurityPort;
    private final DomainEventPublisher eventPublisher;

    public CreateUserService(
        LoadUserPort loadUserPort,
        SaveUserPort saveUserPort,
        PasswordSecurityPort passwordSecurityPort,
        DomainEventPublisher eventPublisher
    ) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordSecurityPort = passwordSecurityPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public User createUser(CreateUserCommand command) {
        // Validate input
        validateUserInput(command);

        // Check for existing user
        checkUserDoesNotExist(command);

        // Hash password
        var passwordHash = passwordSecurityPort.hashPassword(command.password());

        // Create user
        var user = new User(
            UUID.randomUUID().toString(),
            command.username(),
            command.email(),
            passwordHash.hash(),
            passwordHash.salt(),
            LocalDateTime.now(),
            null,
            true, // isActive
            command.firstName(),
            command.lastName(),
            null // lastLoginAt
        );

        // Save user
        var savedUser = saveUserPort.saveUser(user);

        // Publish domain event
        eventPublisher.publish(new UserCreatedEvent(savedUser.id(), savedUser.username(), savedUser.email()));

        // Return user without sensitive information
        return savedUser.toPublic();
    }

    private void validateUserInput(CreateUserCommand command) {
        // Validate username
        if (!USERNAME_PATTERN.matcher(command.username()).matches()) {
            throw new ValidationException("Username must be 3-30 characters and contain only letters, numbers, and underscores");
        }

        // Validate email
        if (!EMAIL_PATTERN.matcher(command.email()).matches()) {
            throw new ValidationException("Invalid email address format");
        }

        // Validate password strength
        if (!passwordSecurityPort.isPasswordStrong(command.password())) {
            throw new ValidationException("Password does not meet security requirements");
        }

        // Validate optional fields
        if (command.firstName() != null && (command.firstName().length() < 1 || command.firstName().length() > 50)) {
            throw new ValidationException("First name must be 1-50 characters");
        }

        if (command.lastName() != null && (command.lastName().length() < 1 || command.lastName().length() > 50)) {
            throw new ValidationException("Last name must be 1-50 characters");
        }
    }

    private void checkUserDoesNotExist(CreateUserCommand command) {
        if (loadUserPort.existsByUsername(command.username())) {
            throw new UserAlreadyExistsException("Username already exists: " + command.username());
        }

        if (loadUserPort.existsByEmail(command.email())) {
            throw new UserAlreadyExistsException("Email already exists: " + command.email());
        }
    }

    /**
     * Domain event published when a user is created.
     */
    public record UserCreatedEvent(String userId, String username, String email) implements it.robfrank.linklift.application.domain.event.DomainEvent {
        public String getEventType() {
            return "USER_CREATED";
        }
    }
}
