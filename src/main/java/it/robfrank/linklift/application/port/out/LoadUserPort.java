package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.User;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Port interface for loading user data.
 * Follows the established port pattern in the codebase.
 */
public interface LoadUserPort {

    /**
     * Finds a user by their unique ID.
     */
    @NonNull Optional<User> findUserById(@NonNull String id);

    /**
     * Finds a user by their username.
     */
    @NonNull Optional<User> findUserByUsername(@NonNull String username);

    /**
     * Finds a user by their email address.
     */
    @NonNull Optional<User> findUserByEmail(@NonNull String email);

    /**
     * Checks if a username already exists.
     */
    boolean existsByUsername(@NonNull String username);

    /**
     * Checks if an email already exists.
     */
    boolean existsByEmail(@NonNull String email);

    /**
     * Gets all active users.
     */
    @NonNull List<User> findAllActiveUsers();

    /**
     * Deactivates a user (soft delete).
     */
    @NonNull User deactivateUser(@NonNull String userId);
}
