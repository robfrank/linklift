package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.User;
import org.jspecify.annotations.NonNull;

/**
 * Port interface for saving user data.
 * Follows the established port pattern in the codebase.
 */
public interface SaveUserPort {
  /**
   * Saves a new user to the persistence layer.
   */
  @NonNull
  User saveUser(@NonNull User user);

  /**
   * Updates an existing user in the persistence layer.
   */
  @NonNull
  User updateUser(@NonNull User user);
}
