package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.User;

/**
 * Use case interface for creating new user accounts.
 * Follows the established use case pattern in the codebase.
 */
public interface CreateUserUseCase {
    /**
     * Creates a new user account with the provided information.
     *
     * @param command the user creation command
     * @return the created user (without sensitive information)
     * @throws it.robfrank.linklift.application.domain.exception.UserAlreadyExistsException if username or email already exists
     * @throws it.robfrank.linklift.application.domain.exception.ValidationException if validation fails
     */
    User createUser(CreateUserCommand command);
}
