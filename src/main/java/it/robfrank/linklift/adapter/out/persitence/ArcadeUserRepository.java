package it.robfrank.linklift.adapter.out.persitence;

import com.arcadedb.exception.ArcadeDBException;
import com.arcadedb.query.sql.executor.Result;
import com.arcadedb.query.sql.executor.ResultSet;
import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.exception.DatabaseException;
import it.robfrank.linklift.application.domain.model.User;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * ArcadeDB implementation for User repository operations.
 * Follows the established repository pattern in the codebase.
 */
public class ArcadeUserRepository {

    private final RemoteDatabase database;
    private final UserMapper userMapper;

    public ArcadeUserRepository(RemoteDatabase database, UserMapper userMapper) {
        this.database = database;
        this.userMapper = userMapper;
    }

    public User save(User user) {
        try {
            database.transaction(() -> {
                database.command(
                    "sql",
                    """
                    INSERT INTO User SET
                    id = ?,
                    username = ?,
                    email = ?,
                    passwordHash = ?,
                    salt = ?,
                    createdAt = ?,
                    updatedAt = ?,
                    isActive = ?,
                    firstName = ?,
                    lastName = ?,
                    lastLoginAt = ?
                    """,
                    user.id(),
                    user.username(),
                    user.email(),
                    user.passwordHash(),
                    user.salt(),
                    user.createdAt().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    user.updatedAt() != null ? user.updatedAt().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null,
                    user.isActive(),
                    user.firstName(),
                    user.lastName(),
                    user.lastLoginAt() != null ? user.lastLoginAt().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null
                );
            });
            return user;
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to save user: " + user.username(), e);
        }
    }

    public User update(User user) {
        try {
            // First check if user exists
            if (!findById(user.id()).isPresent()) {
                throw new RuntimeException("User not found with id: " + user.id());
            }

            database.transaction(() -> {
                database.command(
                    "sql",
                    """
                    UPDATE User SET
                    email = ?,
                    passwordHash = ?,
                    salt = ?,
                    updatedAt = ?,
                    isActive = ?,
                    firstName = ?,
                    lastName = ?,
                    lastLoginAt = ?
                    WHERE id = ?
                    """,
                    user.email(),
                    user.passwordHash(),
                    user.salt(),
                    user.updatedAt() != null ? user.updatedAt().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null,
                    user.isActive(),
                    user.firstName(),
                    user.lastName(),
                    user.lastLoginAt() != null ? user.lastLoginAt().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null,
                    user.id()
                );
            });
            return user;
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to update user: " + user.username(), e);
        }
    }

    public Optional<User> findById(String id) {
        try {
            var result = database.query("sql", "SELECT FROM User WHERE id = ?", id);
            if (result.hasNext()) {
                var vertex = result.next().toElement().asVertex();
                return Optional.of(userMapper.toDomainModel(vertex));
            }
            return Optional.empty();
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to find user by id: " + id, e);
        }
    }

    public Optional<User> findByUsername(String username) {
        try {
            var result = database.query("sql", "SELECT FROM User WHERE username = ?", username);
            if (result.hasNext()) {
                var vertex = result.next().toElement().asVertex();
                return Optional.of(userMapper.toDomainModel(vertex));
            }
            return Optional.empty();
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to find user by username: " + username, e);
        }
    }

    public Optional<User> findByEmail(String email) {
        try {
            var result = database.query("sql", "SELECT FROM User WHERE email = ?", email);
            if (result.hasNext()) {
                var vertex = result.next().toElement().asVertex();
                return Optional.of(userMapper.toDomainModel(vertex));
            }
            return Optional.empty();
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to find user by email: " + email, e);
        }
    }

    public List<User> findAll() {
        try {
            var result = database.query("sql", "SELECT FROM User");
            return result.stream()
                .map(r -> r.toElement().asVertex())
                .map(userMapper::toDomainModel)
                .toList();
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to find all users", e);
        }
    }

    public boolean existsByUsername(String username) {
        try {
            var result = database.query("sql", "SELECT count(*) as count FROM User WHERE username = ?", username);
            if (result.hasNext()) {
                var count = result.next().getProperty("count");
                return count instanceof Number && ((Number) count).longValue() > 0;
            }
            return false;
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to check if username exists: " + username, e);
        }
    }

    public boolean existsByEmail(String email) {
        try {
            var result = database.query("sql", "SELECT count(*) as count FROM User WHERE email = ?", email);
            if (result.hasNext()) {
                var count = result.next().getProperty("count");
                return count instanceof Number && ((Number) count).longValue() > 0;
            }
            return false;
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to check if email exists: " + email, e);
        }
    }

    public User deactivate(String userId) {
        try {
            database.transaction(() -> {
                database.command(
                    "sql",
                    "UPDATE User SET isActive = false, updatedAt = ? WHERE id = ?",
                    java.time.LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    userId
                );
            });

            // Fetch the updated user
            var result = database.query("sql", "SELECT FROM User WHERE id = ?", userId);
            if (result.hasNext()) {
                var vertex = result.next().toElement().asVertex();
                return userMapper.toDomainModel(vertex);
            }
            throw new DatabaseException("Failed to deactivate user: " + userId);

        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to deactivate user: " + userId, e);
        }
    }

    public List<User> findAllActive() {
        try {
            var result = database.query("sql", "SELECT FROM User WHERE isActive = true");
            return result.stream()
                .map(r -> r.toElement().asVertex())
                .map(userMapper::toDomainModel)
                .toList();
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to find all active users", e);
        }
    }

    public void deleteById(String userId) {
        try {
            database.transaction(() -> {
                database.command(
                    "sql",
                    "DELETE FROM User WHERE id = ?",
                    userId
                );
            });
        } catch (ArcadeDBException e) {
            throw new DatabaseException("Failed to delete user: " + userId, e);
        }
    }
}
