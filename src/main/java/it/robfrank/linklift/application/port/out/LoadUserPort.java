package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for loading user data.
 * Follows the established port pattern in the codebase.
 */
public interface LoadUserPort {

    /**
     * Finds a user by their unique ID.
     */
    Optional<User> findUserById(String id);

    /**
     * Finds a user by their username.
     */
    Optional<User> findUserByUsername(String username);

    /**
     * Finds a user by their email address.
     */
    Optional<User> findUserByEmail(String email);

    /**
     * Checks if a username already exists.
     */
    boolean existsByUsername(String username);

    /**
     * Checks if an email already exists.
     */
    boolean existsByEmail(String email);

    /**
     * Gets all active users.
     */
    List<User> findAllActiveUsers();

    /**
     * Deactivates a user (soft delete).
     */
    User deactivateUser(String userId);
}
