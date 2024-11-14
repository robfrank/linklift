package it.robfrank.linklift.config;

import com.arcadedb.database.Database;
import com.arcadedb.database.DatabaseFactory;
import com.arcadedb.schema.Schema;
import com.arcadedb.schema.VertexType;

public class DatabaseConfig {
  private static final String   DB_PATH = "./linklift-db";
  private static       Database database;

  public static Database getDatabase() {
    if (database == null) {
      synchronized (DatabaseConfig.class) {
        if (database == null) {
          database = new DatabaseFactory(DB_PATH).setAutoTransaction(true).create();
          initializeSchema(database);
        }
      }
    }
    return database;
  }

  private static void initializeSchema(Database db) {
    if (!db.getSchema().existsType("Link")) {
      db.begin();

      // Creo il tipo vertice per i Link
      VertexType linkType = db.getSchema().createVertexType("Link");

      // Proprietà del vertice Link
      linkType.createProperty("url", String.class).setMandatory(true).setNotNull(true)
          .createIndex(Schema.INDEX_TYPE.LSM_TREE, true);

      linkType.createProperty("title", String.class);
      linkType.createProperty("description", String.class);
      linkType.createProperty("extractedAt", Long.class);
      linkType.createProperty("contentType", String.class);

      db.commit();
    }
  }

  public static void close() {
    if (database != null && database.isOpen()) {
      database.close();
    }
  }
}