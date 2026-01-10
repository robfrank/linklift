package it.robfrank.linklift.adapter.out.persistence;

import com.arcadedb.graph.MutableVertex;
import com.arcadedb.query.sql.executor.Result;
import com.arcadedb.query.sql.executor.ResultSet;
import com.arcadedb.remote.RemoteDatabase;
import com.arcadedb.remote.RemoteMutableVertex;
import it.robfrank.linklift.application.domain.exception.DatabaseException;
import it.robfrank.linklift.application.domain.model.Content;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public class ArcadeContentRepository {

  private static final String CONTENT_TYPE = "Content";

  private final RemoteDatabase database;
  private final ContentMapper mapper;

  public ArcadeContentRepository(@NonNull RemoteDatabase database) {
    this.database = database;
    this.mapper = new ContentMapper();
  }

  public @NonNull Content save(@NonNull Content content) {
    try {
      database.transaction(() -> {
        MutableVertex vertex = database.newVertex("Content");
        mapper.mapToVertex(content, vertex);
        vertex.save();
      });
      return content;
    } catch (Exception e) {
      throw new DatabaseException("Failed to save content: " + e.getMessage(), e);
    }
  }

  public @NonNull Content update(@NonNull Content content) {
    try {
      database.transaction(() -> {
        database
          .query("sql", "SELECT FROM Content WHERE id = ?", content.id())
          .stream()
          .findFirst()
          .flatMap(Result::getVertex)
          .ifPresent(vertex -> {
            MutableVertex mutableVertex = vertex.modify();
            mapper.mapToVertex(content, mutableVertex);
            mutableVertex.save();
          });
      });
      return content;
    } catch (Exception e) {
      throw new DatabaseException("Failed to update content: " + e.getMessage(), e);
    }
  }

  public @NonNull Optional<Content> findByLinkId(@NonNull String linkId) {
    try {
      var resultSet = database.query("sql", "SELECT FROM Content WHERE linkId = ?", linkId);

      if (resultSet.hasNext()) {
        var vertex = resultSet.next().toElement().asVertex();
        if (vertex == null) throw new DatabaseException("Failed to get vertex from result set");
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
        if (vertex == null) throw new DatabaseException("Failed to get vertex from result set");
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

  public @NonNull List<Content> findSimilar(@NonNull List<Float> queryVector, int limit) {
    try {
      // Use vectorNeighbors and handle the result set which contains {distance,
      // vertex}
      var resultSet = database.query("sql", "SELECT expand(vectorNeighbors('Content[embedding]', ?, ?))", queryVector, limit);
      List<Content> results = new ArrayList<>();
      while (resultSet.hasNext()) {
        var result = resultSet.next();
        Object vertexVal = result.getProperty("vertex");
        if (vertexVal instanceof Map map) {
          // In the remote driver, nested vertices in projections might be returned as
          // Maps
          @SuppressWarnings("unchecked")
          Map<String, Object> vertexMap = (Map<String, Object>) vertexVal;
          results.add(mapper.mapFromMap(vertexMap));
        } else if (result.isVertex()) {
          results.add(mapper.mapToDomain(result.toElement().asVertex()));
        }
      }
      return results;
    } catch (Exception e) {
      throw new DatabaseException("Failed to find similar content: " + e.getMessage(), e);
    }
  }

  public @NonNull List<Content> findContentsWithoutEmbeddings(int limit) {
    try {
      var resultSet = database.query("sql", "SELECT FROM Content WHERE needsEmbedding = true AND textContent IS NOT NULL LIMIT ?", limit);
      List<Content> results = new ArrayList<>();
      while (resultSet.hasNext()) {
        var vertex = resultSet.next().toElement().asVertex();
        if (vertex != null) {
          results.add(mapper.mapToDomain(vertex));
        }
      }
      return results;
    } catch (Exception e) {
      throw new DatabaseException("Failed to find contents without embeddings: " + e.getMessage(), e);
    }
  }
}
