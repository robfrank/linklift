package it.robfrank.linklift.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A built frontend bundle (a directory holding {@code index.html}) that the server serves.
 *
 * @param directory the real path of the bundle directory
 */
public record FrontendBundle(Path directory) {
  private static final Logger logger = LoggerFactory.getLogger(FrontendBundle.class);

  /**
   * Resolves the bundle named by the {@code LINKLIFT_WEBAPP_DIR} value.
   *
   * @return empty for a null or blank value, and (with a warning) for a value that is not a directory holding a regular {@code index.html}
   */
  public static Optional<FrontendBundle> fromEnvValue(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      Path directory = Path.of(value.strip());
      if (Files.isDirectory(directory) && Files.isRegularFile(directory.resolve("index.html"))) {
        return Optional.of(new FrontendBundle(directory.toRealPath()));
      }
    } catch (IOException | RuntimeException e) {
      logger.debug("Cannot resolve frontend bundle directory '{}'", value, e);
    }
    logger.warn("LINKLIFT_WEBAPP_DIR '{}' is not a directory containing index.html; serving without the frontend", value);
    return Optional.empty();
  }

  public Path indexFile() {
    return directory.resolve("index.html");
  }
}
