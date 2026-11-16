package it.robfrank.linklift.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class FrontendBundleTest {

  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private final Logger logger = (Logger) LoggerFactory.getLogger(FrontendBundleTest.class);

  @BeforeEach
  void attachAppender() {
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detachAppender() {
    logger.detachAppender(appender);
  }

  private boolean warned(String value) {
    return appender.list.stream().anyMatch(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains(value));
  }

  @Test
  void nullValue_isEmptyWithoutWarning() {
    assertThat(FrontendBundle.fromEnvValue(null)).isEmpty();
    assertThat(appender.list).isEmpty();
  }

  @Test
  void blankValue_isEmptyWithoutWarning() {
    assertThat(FrontendBundle.fromEnvValue("   ")).isEmpty();
    assertThat(appender.list).isEmpty();
  }

  @Test
  void missingPath_isEmptyWithWarning(@TempDir Path tmp) {
    String value = tmp.resolve("missing").toString();
    assertThat(FrontendBundle.fromEnvValue(value)).isEmpty();
    assertThat(warned(value)).isTrue();
  }

  @Test
  void directoryWithoutIndex_isEmptyWithWarning(@TempDir Path tmp) {
    String value = tmp.toString();
    assertThat(FrontendBundle.fromEnvValue(value)).isEmpty();
    assertThat(warned(value)).isTrue();
  }

  @Test
  void regularFile_isEmptyWithWarning(@TempDir Path tmp) throws Exception {
    Path file = Files.writeString(tmp.resolve("index.html"), "x");
    assertThat(FrontendBundle.fromEnvValue(file.toString())).isEmpty();
    assertThat(warned(file.toString())).isTrue();
  }

  @Test
  void directoryWithIndex_isPresentWithRealPath(@TempDir Path tmp) throws Exception {
    Files.writeString(tmp.resolve("index.html"), "<html></html>");
    Optional<FrontendBundle> bundle = FrontendBundle.fromEnvValue(tmp.toString());
    assertThat(bundle).isPresent();
    assertThat(bundle.get().directory()).isEqualTo(tmp.toRealPath());
    assertThat(bundle.get().indexFile()).isEqualTo(tmp.toRealPath().resolve("index.html"));
    assertThat(appender.list).isEmpty();
  }
}
