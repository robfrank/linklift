package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.User;

/**
 * Port interface for saving user data.
 * Follows the established port pattern in the codebase.
 */
public interface SaveUserPort {

    /**
     * Saves a new user to the persistence layer.
     */
    User saveUser(User user);

    /**
     * Updates an existing user in the persistence layer.
     */
    User updateUser(User user);
}
