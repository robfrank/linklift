package it.robfrank.linklift.adapter.out.persistence;

import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.exception.DatabaseException;
import it.robfrank.linklift.application.domain.model.Content;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public class ArcadeContentRepository {

  private static final String CONTENT_TYPE = "Content";
  private static final String HAS_CONTENT_EDGE = "HasContent";

  private final RemoteDatabase database;
  private final ContentMapper mapper;

  public ArcadeContentRepository(@NonNull RemoteDatabase database) {
    this.database = database;
    this.mapper = new ContentMapper();
  }

  public @NonNull Content save(@NonNull Content content) {
    try {
      var vertex = database.newVertex(CONTENT_TYPE);
      mapper.mapToVertex(content, vertex);
      vertex.save();
      return mapper.mapToDomain(vertex);
    } catch (Exception e) {
      throw new DatabaseException("Failed to save content: " + e.getMessage(), e);
    }
  }

  public @NonNull Optional<Content> findByLinkId(@NonNull String linkId) {
    try {
      var resultSet = database.query("sql", "SELECT FROM Content WHERE linkId = ?", linkId);

      if (resultSet.hasNext()) {
        var vertex = resultSet.next().toElement().asVertex();
        return Optional.of(mapper.mapToDomain(vertex));
      }
      return Optional.empty();
    } catch (Exception e) {
      throw new DatabaseException("Failed to find content by link ID: " + e.getMessage(), e);
    }
  }

  public @NonNull Optional<Content> findById(@NonNull String contentId) {
    try {
      var resultSet = database.query("sql", "SELECT FROM Content WHERE id = ?", contentId);

      if (resultSet.hasNext()) {
        var vertex = resultSet.next().toElement().asVertex();
        return Optional.of(mapper.mapToDomain(vertex));
      }
      return Optional.empty();
    } catch (Exception e) {
      throw new DatabaseException("Failed to find content by ID: " + e.getMessage(), e);
    }
  }

  public void createHasContentEdge(@NonNull String linkId, @NonNull String contentId) {
    try {
      database.transaction(() -> {
        // Create edge using SQL command
        database.command(
          "sql",
          """
          CREATE EDGE HasContent
          FROM (SELECT FROM Link WHERE id = ?)
          TO (SELECT FROM Content WHERE id = ?)
          SET createdAt = ?
          """,
          linkId,
          contentId,
          LocalDateTime.now()
        );
      });
    } catch (Exception e) {
      throw new DatabaseException("Failed to create HasContent edge: " + e.getMessage(), e);
    }
  }

  public void deleteByLinkId(@NonNull String linkId) {
    try {
      database.transaction(() -> {
        database.command("sql", "DELETE VERTEX Content WHERE linkId = ?", linkId);
      });
    } catch (Exception e) {
      throw new DatabaseException("Failed to delete content by link ID: " + e.getMessage(), e);
    }
  }

  public List<Content> findSimilar(@NonNull List<Float> queryVector, int limit) {
    try {
      var resultSet = database.query("sql", "SELECT FROM Content WHERE embedding VECTOR KNN [?, ?]", queryVector, limit);
      List<Content> results = new ArrayList<>();
      while (resultSet.hasNext()) {
        results.add(mapper.mapToDomain(resultSet.next().toElement().asVertex()));
      }
      return results;
    } catch (Exception e) {
      throw new DatabaseException("Failed to find similar content: " + e.getMessage(), e);
    }
  }

  public List<Content> findContentsWithoutEmbeddings(int limit) {
    try {
      var resultSet = database.query("sql", "SELECT FROM Content WHERE embedding IS NULL AND textContent IS NOT NULL LIMIT ?", limit);
      List<Content> results = new ArrayList<>();
      while (resultSet.hasNext()) {
        results.add(mapper.mapToDomain(resultSet.next().toElement().asVertex()));
      }
      return results;
    } catch (Exception e) {
      throw new DatabaseException("Failed to find contents without embeddings: " + e.getMessage(), e);
    }
  }
}
