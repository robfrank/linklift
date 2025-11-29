package it.robfrank.linklift.adapter.out.persistence;

import com.arcadedb.exception.ArcadeDBException;
import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.exception.DatabaseException;
import it.robfrank.linklift.application.domain.model.Collection;

public class ArcadeCollectionRepository {

  private final RemoteDatabase database;

  public ArcadeCollectionRepository(RemoteDatabase database) {
    this.database = database;
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
          query = ?
          """,
          collection.id(),
          collection.name(),
          collection.description(),
          collection.userId(),
          collection.query()
        );
      });
      return collection;
    } catch (ArcadeDBException e) {
      throw new DatabaseException("Failed to save collection: " + collection.name(), e);
    }
  }
}
