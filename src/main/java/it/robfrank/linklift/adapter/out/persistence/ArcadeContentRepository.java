package it.robfrank.linklift.adapter.out.persistence;

import com.arcadedb.graph.MutableVertex;
import com.arcadedb.query.sql.executor.Result;
import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.exception.DatabaseException;
import it.robfrank.linklift.application.domain.model.Content;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArcadeContentRepository {

  private static final Logger logger = LoggerFactory.getLogger(ArcadeContentRepository.class);

  private static final int VECTOR_OVERFETCH_FACTOR = 5;
  private static final int VECTOR_OVERFETCH_MAX = 1000;

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

  public @NonNull List<Content> findSimilar(@NonNull List<Float> queryVector, int limit, @NonNull String userId) {
    try {
      // Restrict results to links the user owns. ArcadeDB 26.6.x cannot apply a WHERE filter to a
      // vectorNeighbors() result (wrapping it in a subquery breaks index resolution), so we over-fetch
      // and filter by ownership here. Security holds regardless of the fetch size; a larger fetch only
      // improves completeness (more owned candidates considered).
      Set<String> ownedLinkIds = findOwnedLinkIds(userId);
      if (ownedLinkIds.isEmpty()) {
        return List.of();
      }

      int fetch = Math.min(Math.max(limit, 1) * VECTOR_OVERFETCH_FACTOR, VECTOR_OVERFETCH_MAX);

      // The query vector must be inlined as a literal: ArcadeDB 26.6.x fails index resolution
      // ("No vector index found ...") when the vector is passed as a bound parameter. Inlining is safe
      // here because the values are floats (no SQL-injection surface).
      StringBuilder vectorLiteral = new StringBuilder("[");
      for (int i = 0; i < queryVector.size(); i++) {
        if (i > 0) vectorLiteral.append(',');
        vectorLiteral.append(queryVector.get(i).floatValue());
      }
      vectorLiteral.append(']');
      var resultSet = database.query("sql", "SELECT expand(vectorNeighbors('Content[embedding]', " + vectorLiteral + ", " + fetch + "))");
      List<Content> results = new ArrayList<>();
      while (resultSet.hasNext() && results.size() < limit) {
        var result = resultSet.next();
        try {
          Content content = mapper.mapFromMap(result.toMap());
          if (ownedLinkIds.contains(content.linkId())) {
            results.add(content);
          }
        } catch (Exception e) {
          // Skip results that can't be mapped (e.g. placeholder zero-vector entries)
          logger.debug("Skipping vector search result that could not be mapped to Content: {}", e.getMessage());
        }
      }
      return results;
    } catch (Exception e) {
      throw new DatabaseException("Failed to find similar content: " + e.getMessage(), e);
    }
  }

  private Set<String> findOwnedLinkIds(String userId) {
    Set<String> ids = new HashSet<>();
    var rs = database.query("sql", "SELECT expand(out('OwnsLink').id) FROM User WHERE id = ?", userId);
    while (rs.hasNext()) {
      Object value = rs.next().getProperty("value");
      if (value != null) {
        ids.add(value.toString());
      }
    }
    return ids;
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
