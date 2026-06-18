package it.robfrank.linklift.adapter.out.persistence;

import com.arcadedb.exception.ArcadeDBException;
import com.arcadedb.query.sql.executor.Result;
import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.exception.DatabaseException;
import it.robfrank.linklift.application.domain.model.Tag;
import it.robfrank.linklift.application.port.out.TagRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArcadeTagRepository implements TagRepository {

  private static final Logger logger = LoggerFactory.getLogger(ArcadeTagRepository.class);
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final RemoteDatabase database;

  public ArcadeTagRepository(RemoteDatabase database) {
    this.database = database;
  }

  @Override
  public Tag save(@NonNull Tag tag) {
    try {
      database.transaction(() -> {
        database.command(
          "sql",
          """
          INSERT INTO Tag SET
          id = ?,
          name = ?,
          userId = ?,
          createdAt = ?
          """,
          tag.id(),
          tag.name(),
          tag.userId(),
          tag.createdAt().truncatedTo(ChronoUnit.SECONDS).format(FORMATTER)
        );
      });
      return tag;
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to save tag: " + tag.name(), e);
    }
  }

  @Override
  public void delete(@NonNull String tagId) {
    try {
      database.transaction(() -> {
        // Remove HasTag edges first, then delete the tag
        database.command("sql", "DELETE FROM HasTag WHERE @in.id = ?", tagId);
        database.command("sql", "DELETE FROM Tag WHERE id = ?", tagId);
      });
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to delete tag: " + tagId, e);
    }
  }

  @Override
  public Optional<Tag> findById(@NonNull String tagId) {
    try {
      return database.query("sql", "SELECT FROM Tag WHERE id = ?", tagId).stream().findFirst().flatMap(Result::getVertex).map(this::toTag);
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to find tag by id: " + tagId, e);
    }
  }

  @Override
  public Optional<Tag> findByNameAndUserId(@NonNull String name, @NonNull String userId) {
    try {
      return database
        .query("sql", "SELECT FROM Tag WHERE name = ? AND userId = ?", name.toLowerCase().strip(), userId)
        .stream()
        .findFirst()
        .flatMap(Result::getVertex)
        .map(this::toTag);
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to find tag by name and userId", e);
    }
  }

  @Override
  public List<Tag> findByUserId(@NonNull String userId) {
    try {
      return database
        .query("sql", "SELECT FROM Tag WHERE userId = ? ORDER BY name ASC", userId)
        .stream()
        .map(Result::getVertex)
        .flatMap(Optional::stream)
        .map(this::toTag)
        .toList();
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to find tags for user: " + userId, e);
    }
  }

  @Override
  public List<Tag> findTagsForLink(@NonNull String linkId) {
    try {
      return database
        .query(
          "sql",
          """
          SELECT expand(out('HasTag'))
          FROM Link
          WHERE id = ?
          ORDER BY name ASC
          """,
          linkId
        )
        .stream()
        .map(Result::getVertex)
        .flatMap(Optional::stream)
        .map(this::toTag)
        .toList();
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to find tags for link: " + linkId, e);
    }
  }

  @Override
  public List<Tag> findTagsForLink(@NonNull String linkId, @NonNull String userId) {
    try {
      return database
        .query(
          "sql",
          """
          SELECT FROM (SELECT expand(out('HasTag')) FROM Link WHERE id = ?)
          WHERE userId = ?
          ORDER BY name ASC
          """,
          linkId,
          userId
        )
        .stream()
        .map(Result::getVertex)
        .flatMap(Optional::stream)
        .map(this::toTag)
        .toList();
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to find tags for link: " + linkId, e);
    }
  }

  @Override
  public List<Tag> findTagsForLinks(@NonNull List<String> linkIds) {
    if (linkIds.isEmpty()) {
      return List.of();
    }
    try {
      return database
        .query(
          "sql",
          """
          SELECT expand(out('HasTag'))
          FROM Link
          WHERE id IN ?
          ORDER BY name ASC
          """,
          linkIds
        )
        .stream()
        .map(Result::getVertex)
        .flatMap(Optional::stream)
        .map(this::toTag)
        .toList();
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to find tags for links", e);
    }
  }

  @Override
  public void addTagToLink(@NonNull String linkId, @NonNull String tagId) {
    try {
      database.transaction(() -> {
        // Check if edge already exists
        var existing = database.query("sql", "SELECT count(*) as count FROM HasTag WHERE @out.id = ? AND @in.id = ?", linkId, tagId);
        long count = existing.stream().findFirst().map(r -> r.<Number>getProperty("count")).map(Number::longValue).orElse(0L);

        if (count == 0) {
          database.command(
            "sql",
            """
            CREATE EDGE HasTag
            FROM (SELECT FROM Link WHERE id = ?)
            TO (SELECT FROM Tag WHERE id = ?)
            """,
            linkId,
            tagId
          );
        }
      });
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to add tag " + tagId + " to link " + linkId, e);
    }
  }

  @Override
  public void removeTagFromLink(@NonNull String linkId, @NonNull String tagId) {
    try {
      database.transaction(() -> {
        database.command("sql", "DELETE FROM HasTag WHERE @out.id = ? AND @in.id = ?", linkId, tagId);
      });
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to remove tag " + tagId + " from link " + linkId, e);
    }
  }

  @Override
  public List<String> findLinkIdsByTagId(@NonNull String tagId) {
    try {
      return database
        .query(
          "sql",
          """
          SELECT expand(in('HasTag').id)
          FROM Tag
          WHERE id = ?
          """,
          tagId
        )
        .stream()
        .map(r -> r.<String>getProperty("value"))
        .filter(id -> id != null)
        .toList();
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to find links for tag: " + tagId, e);
    }
  }

  private Tag toTag(com.arcadedb.graph.Vertex vertex) {
    LocalDateTime createdAt = vertex.getLocalDateTime("createdAt");
    if (createdAt == null) {
      logger.warn("Tag {} is missing createdAt; substituting current time (possible data corruption)", vertex.getString("id"));
      createdAt = LocalDateTime.now();
    }
    return new Tag(vertex.getString("id"), vertex.getString("name"), vertex.getString("userId"), createdAt);
  }
}
