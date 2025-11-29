package it.robfrank.linklift.adapter.out.persitence;

import it.robfrank.linklift.application.domain.model.User;
import it.robfrank.linklift.application.port.out.LoadUserPort;
import it.robfrank.linklift.application.port.out.SaveUserPort;
import java.util.List;
import java.util.Optional;

/**
 * Persistence adapter that implements user loading and saving ports.
 * Bridges the domain layer with the ArcadeDB persistence infrastructure.
 */
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

  private final ArcadeUserRepository userRepository;

  public UserPersistenceAdapter(ArcadeUserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public Optional<User> findUserById(String userId) {
    return userRepository.findById(userId);
  }

  @Override
  public Optional<User> findUserByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  @Override
  public Optional<User> findUserByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  @Override
  public boolean existsByUsername(String username) {
    return userRepository.existsByUsername(username);
  }

  @Override
  public boolean existsByEmail(String email) {
    return userRepository.existsByEmail(email);
  }

  @Override
  public User saveUser(User user) {
    return userRepository.save(user);
  }

  @Override
  public User updateUser(User user) {
    return userRepository.update(user);
  }

  public void deleteUser(String userId) {
    userRepository.deactivate(userId);
  }

  @Override
  public User deactivateUser(String userId) {
    return userRepository.deactivate(userId);
  }

  @Override
  public List<User> findAllActiveUsers() {
    return userRepository.findAllActive();
  }
}
