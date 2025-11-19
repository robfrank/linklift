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
import org.jspecify.annotations.NonNull;

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

  public void applySchemaScripts(RemoteDatabase db, Path sqlFiles) throws IOException {
    try (Stream<Path> walk = Files.walk(sqlFiles, 1)) {
      walk
        .filter(Files::isRegularFile)
        .sorted()
        .peek(sqlFile -> System.out.println("applying sqlFile = " + sqlFile))
        .forEach(sqlFile -> {
          try {
            String script = readScript(sqlFile);
            String[] statements = script.split(";"); // Split by semicolon
            db.transaction(() -> {
              for (String statement : statements) {
                String trimmedStatement = statement.trim();
                if (!trimmedStatement.isEmpty()) {
                  db.command("sql", trimmedStatement); // Execute each statement individually
                }
              }
            });
          } catch (IOException e) {
            System.out.printf("Error while applying %s  : %s %n", sqlFile, e.getMessage());
          }
        });
    }
  }

  @NonNull
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
