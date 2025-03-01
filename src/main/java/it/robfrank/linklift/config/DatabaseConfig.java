package it.robfrank.linklift.config;

import com.arcadedb.database.Database;
import com.arcadedb.database.DatabaseFactory;
import com.arcadedb.remote.RemoteDatabase;
import com.arcadedb.schema.Schema;
import com.arcadedb.schema.VertexType;
import io.javalin.util.FileUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class DatabaseConfig {

  private static final String DB_PATH = "./linklift-db";
  private static Database database;

  //  public static Database getDatabase() {
  //    if (database == null) {
  //      synchronized (DatabaseConfig.class) {
  //        if (database == null) {
  //          database = new DatabaseFactory(DB_PATH).setAutoTransaction(true).create();
  //          initializeSchema(database);
  //        }
  //      }
  //    }
  //    return database;
  //  }

  public static void initializeSchema(RemoteDatabase db) {
    if (!db.getSchema().existsType("Link")) {
      InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream("linklift-schema-0.sql");
      System.out.println("is = " + is);
      BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      String line;
      try {
        while ((line = reader.readLine()) != null) {
          sb.append(line).append("\n");
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      System.out.println("sb = " + sb.toString());
      //      String script = FileUtil.readResource("linklift-schema-0.sql");
      //
      db.begin();
      db.command("sqlscript", sb.toString());
      db.commit();
    }
  }

  public static void close() {
    if (database != null && database.isOpen()) {
      database.close();
    }
  }
}
