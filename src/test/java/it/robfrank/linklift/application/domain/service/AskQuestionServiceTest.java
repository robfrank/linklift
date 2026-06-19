package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.QuestionAnswer;
import it.robfrank.linklift.application.domain.model.ReadStatus;
import it.robfrank.linklift.application.port.in.AskQuestionCommand;
import it.robfrank.linklift.application.port.out.EmbeddingGenerator;
import it.robfrank.linklift.application.port.out.LoadContentPort;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import it.robfrank.linklift.application.port.out.QuestionAnswerPort;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AskQuestionServiceTest {

  @Mock
  private EmbeddingGenerator embeddingGenerator;

  @Mock
  private LoadContentPort loadContentPort;

  @Mock
  private LoadLinksPort loadLinksPort;

  @Mock
  private QuestionAnswerPort questionAnswerPort;

  private AskQuestionService service;

  private static final List<Float> FAKE_VECTOR = List.of(0.1f, 0.2f, 0.3f);

  @BeforeEach
  void setUp() {
    service = new AskQuestionService(embeddingGenerator, loadContentPort, loadLinksPort, questionAnswerPort);
  }

  @Test
  void ask_returnsAnswerWithSources() {
    when(embeddingGenerator.generateEmbedding("What is Java?")).thenReturn(FAKE_VECTOR);

    Content content = new Content("c1", "link1", null, "Java is a programming language.", 30, LocalDateTime.now(), "text/plain", DownloadStatus.COMPLETED);
    when(loadContentPort.findSimilar(FAKE_VECTOR, 5, "user1")).thenReturn(List.of(content));

    Link link = new Link(
      "link1",
      "https://example.com",
      "Java Guide",
      null,
      LocalDateTime.now(),
      null,
      List.of(),
      ReadStatus.UNREAD,
      false,
      false
    );
    when(loadLinksPort.findLinksByIds(List.of("link1"))).thenReturn(List.of(link));

    when(questionAnswerPort.generateAnswer(eq("What is Java?"), anyString())).thenReturn("Java is a general-purpose programming language.");

    QuestionAnswer result = service.ask(new AskQuestionCommand("What is Java?", "user1"));

    assertThat(result.question()).isEqualTo("What is Java?");
    assertThat(result.answer()).isEqualTo("Java is a general-purpose programming language.");
    assertThat(result.sources()).hasSize(1);
    assertThat(result.sources().getFirst().linkId()).isEqualTo("link1");
    assertThat(result.sources().getFirst().title()).isEqualTo("Java Guide");
    assertThat(result.sources().getFirst().url()).isEqualTo("https://example.com");
  }

  @Test
  void ask_returnsEmptyResponseWhenNoContent() {
    when(embeddingGenerator.generateEmbedding("unknown topic")).thenReturn(FAKE_VECTOR);
    when(loadContentPort.findSimilar(FAKE_VECTOR, 5, "user1")).thenReturn(List.of());

    QuestionAnswer result = service.ask(new AskQuestionCommand("unknown topic", "user1"));

    assertThat(result.sources()).isEmpty();
    assertThat(result.answer()).contains("could not find");
    verifyNoInteractions(questionAnswerPort);
  }

  @Test
  void ask_rejectsBlankQuestion() {
    assertThatThrownBy(() -> service.ask(new AskQuestionCommand("", "user1"))).isInstanceOf(ValidationException.class);
  }

  @Test
  void ask_usesFallbackTitleWhenLinkTitleIsNull() {
    when(embeddingGenerator.generateEmbedding("test")).thenReturn(FAKE_VECTOR);

    Content content = new Content("c1", "link1", null, "Some content here.", 20, LocalDateTime.now(), "text/plain", DownloadStatus.COMPLETED);
    when(loadContentPort.findSimilar(FAKE_VECTOR, 5, "user1")).thenReturn(List.of(content));

    Link linkWithNullTitle = new Link(
      "link1",
      "https://example.com",
      null,
      null,
      LocalDateTime.now(),
      null,
      List.of(),
      ReadStatus.UNREAD,
      false,
      false
    );
    when(loadLinksPort.findLinksByIds(List.of("link1"))).thenReturn(List.of(linkWithNullTitle));
    when(questionAnswerPort.generateAnswer(anyString(), anyString())).thenReturn("answer");

    QuestionAnswer result = service.ask(new AskQuestionCommand("test", "user1"));

    assertThat(result.sources().getFirst().title()).isEqualTo("https://example.com");
  }

  @Test
  void ask_skipsSourceWhenLinkLoadFails() {
    when(embeddingGenerator.generateEmbedding("test")).thenReturn(FAKE_VECTOR);

    Content content = new Content("c1", "link1", null, "Content.", 10, LocalDateTime.now(), "text/plain", DownloadStatus.COMPLETED);
    when(loadContentPort.findSimilar(FAKE_VECTOR, 5, "user1")).thenReturn(List.of(content));
    // Link not returned by the batch lookup -> its source is skipped.
    when(loadLinksPort.findLinksByIds(List.of("link1"))).thenReturn(List.of());
    when(questionAnswerPort.generateAnswer(anyString(), anyString())).thenReturn("answer");

    QuestionAnswer result = service.ask(new AskQuestionCommand("test", "user1"));

    assertThat(result.sources()).isEmpty();
    assertThat(result.answer()).isEqualTo("answer");
  }
}
