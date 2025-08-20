package it.robfrank.linklift.config;

import com.arcadedb.remote.RemoteDatabase;
import com.arcadedb.remote.RemoteServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class DatabaseInitializer {

  private static final String DATABASE_NAME = "linklift";
  private final String arcadedbServer;
  private final int port;
  private final String username;
  private final String password;

  public DatabaseInitializer(String arcadedbServer, int port, String username, String password) {
    this.arcadedbServer = arcadedbServer;
    this.port = port;
    this.username = username;
    this.password = password;
  }

  public void initializeDatabase() {
    RemoteServer server = new RemoteServer(arcadedbServer, port, username, password);

    if (!server.exists(DATABASE_NAME)) {
      server.create(DATABASE_NAME);
    }
    RemoteDatabase database = new RemoteDatabase(arcadedbServer, port, DATABASE_NAME, username, password);
    initializeSchema(database);

    database.close();
  }

  public void initializeSchema(RemoteDatabase db) {
    if (!db.getSchema().existsType("Link")) {
      try {
        URI uri = DatabaseInitializer.class.getResource("/schema").toURI();
        if (uri.getScheme().equals("jar")) {
          FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
          applySchemaScripts(db, fileSystem.getPath("/schema"));
        } else {
          applySchemaScripts(db, Paths.get(uri));
        }
      } catch (URISyntaxException | IOException e) {
        System.out.printf("Error while reading schema files: %s %n", e.getMessage());
      }
    }
  }

  public void applySchemaScripts(RemoteDatabase db, Path sqlFiles) throws IOException {
    try (Stream<Path> walk = Files.walk(sqlFiles, 1)) {
      walk
        .filter(Files::isRegularFile)
        .sorted()
        .peek(sqlFile -> System.out.println("applying sqlFile = " + sqlFile))
        .forEach(sqlFile -> {
          try {
            String script = readScript(sqlFile);
            db.transaction(() -> db.command("sqlscript", script));
          } catch (IOException e) {
            System.out.printf("Error while applying %s  : %s %n", sqlFile, e.getMessage());
          }
        });
    }
  }

  private void executeStatements(RemoteDatabase db, String script) {
    // Split script into individual statements, handling multi-line statements properly
    String[] lines = script.split("\n");
    StringBuilder currentStatement = new StringBuilder();

    for (String line : lines) {
      String trimmedLine = line.trim();

      // Skip empty lines and comments
      if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
        continue;
      }

      currentStatement.append(trimmedLine);

      // If line ends with semicolon, execute the statement
      if (trimmedLine.endsWith(";")) {
        String statement = currentStatement.toString();
        // Remove the trailing semicolon as ArcadeDB doesn't like it
        statement = statement.substring(0, statement.length() - 1);

        executeStatement(db, statement);
        currentStatement.setLength(0); // Clear for next statement
      } else {
        currentStatement.append(" "); // Add space between lines
      }
    }

    // Execute any remaining statement
    if (currentStatement.length() > 0) {
      String statement = currentStatement.toString().trim();
      if (!statement.isEmpty()) {
        executeStatement(db, statement);
      }
    }
  }

  private void executeStatement(RemoteDatabase db, String statement) {
    try {
      db.transaction(() -> {
        try {
          db.command("sql", statement);
          System.out.printf("Successfully executed: %s%n", statement);
        } catch (Exception e) {
          System.out.printf("Error executing statement '%s': %s%n", statement, e.getMessage());
          throw e; // Re-throw to rollback transaction
        }
      });
    } catch (Exception e) {
      // Statement failed, but continue with next ones
    }
  }

  @NotNull
  private static String readScript(Path sqlFile) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(sqlFile), StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      sb.append(line).append("\n");
    }
    return sb.toString();
  }
}
