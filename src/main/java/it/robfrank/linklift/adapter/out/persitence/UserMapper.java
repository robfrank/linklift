package it.robfrank.linklift.adapter.out.persitence;

import com.arcadedb.graph.Vertex;
import it.robfrank.linklift.application.domain.model.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Maps User domain objects from ArcadeDB Vertex format.
 * Follows the established mapper pattern in the codebase.
 */
public class UserMapper {

  public User toDomainModel(Vertex vertex) {
    return new User(
      vertex.getString("id"),
      vertex.getString("username"),
      vertex.getString("email"),
      vertex.getString("passwordHash"),
      vertex.getString("salt"),
      parseDateTime(vertex.get("createdAt")),
      parseDateTime(vertex.get("updatedAt")),
      vertex.getBoolean("isActive"),
      vertex.getString("firstName"),
      vertex.getString("lastName"),
      parseDateTime(vertex.get("lastLoginAt"))
    );
  }

  private LocalDateTime parseDateTime(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof LocalDateTime time) {
      return time;
    }
    if (value instanceof String string) {
      return LocalDateTime.parse(string, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    throw new IllegalArgumentException("Cannot parse datetime from " + value.getClass());
  }
}
