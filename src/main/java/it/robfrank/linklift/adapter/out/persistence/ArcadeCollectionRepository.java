package it.robfrank.linklift.adapter.out.persistence;

import com.arcadedb.database.Document;
import com.arcadedb.exception.ArcadeDBException;
import com.arcadedb.query.sql.executor.Result;
import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.exception.DatabaseException;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.domain.model.Link;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArcadeCollectionRepository {

  private static final Logger logger = LoggerFactory.getLogger(ArcadeCollectionRepository.class);
  private final RemoteDatabase database;
  private final LinkMapper linkMapper;

  public ArcadeCollectionRepository(RemoteDatabase database) {
    this.database = database;
    this.linkMapper = new LinkMapper();
  }

  private Collection toCollection(Document vertex) {
    return new Collection(
      vertex.getString("id"),
      vertex.getString("name"),
      vertex.getString("description"),
      vertex.getString("userId"),
      vertex.getString("query"),
      vertex.getString("summary")
    );
  }

  public Collection save(Collection collection) {
    try {
      database.transaction(() -> {
        database.command(
          "sql",
          """
          INSERT INTO Collection SET
          id = ?,
          name = ?,
          description = ?,
          userId = ?,
          query = ?,
          summary = ?
          """,
          collection.id(),
          collection.name(),
          collection.description(),
          collection.userId(),
          collection.query(),
          collection.summary()
        );
      });
      return collection;
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to save collection: " + collection.name(), e);
    }
  }

  public List<Collection> findByUserId(String userId) {
    try {
      return database
        .query("sql", "SELECT FROM Collection WHERE userId = ? ORDER BY name ASC", userId)
        .stream()
        .map(Result::getVertex)
        .flatMap(Optional::stream)
        .map(this::toCollection)
        .toList();
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to find collections for user: " + userId, e);
    }
  }

  public Optional<Collection> findById(String collectionId) {
    try {
      return database.query("sql", "SELECT FROM Collection WHERE id = ?", collectionId).stream().findFirst().flatMap(Result::getVertex).map(this::toCollection);
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to find collection by ID: " + collectionId, e);
    }
  }

  public void addLinkToCollection(String collectionId, String linkId) {
    try {
      database.transaction(() -> {
        // Create edge from Collection to Link
        database.command(
          "sql",
          """
          CREATE EDGE ContainsLink
          FROM (SELECT FROM Collection WHERE id = ?)
          TO (SELECT FROM Link WHERE id = ?)
          SET addedAt = SYSDATE()
          """,
          collectionId,
          linkId
        );
      });
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to add link to collection: " + collectionId, e);
    }
  }

  public void removeLinkFromCollection(String collectionId, String linkId) {
    try {
      database.transaction(() -> {
        database.command(
          "sql",
          """
          DELETE FROM ContainsLink
          WHERE @out IN (SELECT FROM Collection WHERE id = ?)
          AND @in IN (SELECT FROM Link WHERE id = ?)
          """,
          collectionId,
          linkId
        );
      });
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to remove link from collection: " + collectionId, e);
    }
  }

  public List<Link> getCollectionLinks(String collectionId) {
    try {
      return database
        .query(
          "sql",
          """
          SELECT expand(out('ContainsLink'))
          FROM Collection
          WHERE id = ?
          ORDER BY extractedAt DESC
          """,
          collectionId
        )
        .stream()
        .map(Result::getVertex)
        .flatMap(Optional::stream)
        .map(linkMapper::mapToDomain)
        .toList();
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to get links for collection: " + collectionId, e);
    }
  }

  public void deleteCollection(String collectionId) {
    try {
      database.transaction(() -> {
        // Delete the collection vertex (edges will be cascade deleted)
        database.command("sql", "DELETE FROM Collection WHERE id = ?", collectionId);
      });
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to delete collection: " + collectionId, e);
    }
  }

  public void mergeCollections(String sourceCollectionId, String targetCollectionId) {
    try {
      database.transaction(() -> {
        // 1. Create new edges for the target collection from the source collection's links
        database.command(
          "sql",
          """
          CREATE EDGE ContainsLink
          FROM (SELECT FROM Collection WHERE id = ?)
          TO (SELECT expand(out('ContainsLink')) FROM Collection WHERE id = ?)
          SET addedAt = SYSDATE()
          """,
          targetCollectionId,
          sourceCollectionId
        );

        // 2. Delete source collection (this will also delete the old edges)
        database.command("sql", "DELETE FROM Collection WHERE id = ?", sourceCollectionId);
      });
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to merge collection " + sourceCollectionId + " into " + targetCollectionId, e);
    }
  }
}
