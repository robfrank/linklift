package it.robfrank.linklift.adapter.out.persitence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ContentPersistenceAdapterTest {

  @Mock
  private ArcadeContentRepository arcadeContentRepository;

  private ContentPersistenceAdapter contentPersistenceAdapter;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    contentPersistenceAdapter = new ContentPersistenceAdapter(arcadeContentRepository);
  }

  @Test
  void saveContent_shouldDelegateToRepository() {
    // Arrange
    Content content = new Content(
      "content-123",
      "link-456",
      "<html><body>Test</body></html>",
      "Test",
      1024,
      LocalDateTime.now(),
      "text/html",
      DownloadStatus.COMPLETED
    );

    when(arcadeContentRepository.save(content)).thenReturn(content);

    // Act
    Content result = contentPersistenceAdapter.saveContent(content);

    // Assert
    assertThat(result).isEqualTo(content);
    verify(arcadeContentRepository, times(1)).save(content);
  }

  @Test
  void createHasContentEdge_shouldDelegateToRepository() {
    // Arrange
    String linkId = "link-123";
    String contentId = "content-456";

    // Act
    contentPersistenceAdapter.createHasContentEdge(linkId, contentId);

    // Assert
    verify(arcadeContentRepository, times(1)).createHasContentEdge(linkId, contentId);
  }

  @Test
  void loadContentByLinkId_shouldDelegateToRepository() {
    // Arrange
    String linkId = "link-123";
    Content expectedContent = new Content(
      "content-456",
      linkId,
      "<html><body>Test</body></html>",
      "Test",
      1024,
      LocalDateTime.now(),
      "text/html",
      DownloadStatus.COMPLETED
    );

    when(arcadeContentRepository.findByLinkId(linkId)).thenReturn(Optional.of(expectedContent));

    // Act
    Optional<Content> result = contentPersistenceAdapter.findContentByLinkId(linkId);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(expectedContent);
    verify(arcadeContentRepository, times(1)).findByLinkId(linkId);
  }

  @Test
  void loadContentByLinkId_shouldReturnEmptyWhenNotFound() {
    // Arrange
    String linkId = "link-123";

    when(arcadeContentRepository.findByLinkId(linkId)).thenReturn(Optional.empty());

    // Act
    Optional<Content> result = contentPersistenceAdapter.findContentByLinkId(linkId);

    // Assert
    assertThat(result).isEmpty();
    verify(arcadeContentRepository, times(1)).findByLinkId(linkId);
  }

  @Test
  void loadContentById_shouldDelegateToRepository() {
    // Arrange
    String contentId = "content-456";
    Content expectedContent = new Content(
      contentId,
      "link-123",
      "<html><body>Test</body></html>",
      "Test",
      1024,
      LocalDateTime.now(),
      "text/html",
      DownloadStatus.COMPLETED
    );

    when(arcadeContentRepository.findById(contentId)).thenReturn(Optional.of(expectedContent));

    // Act
    Optional<Content> result = contentPersistenceAdapter.findContentById(contentId);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(expectedContent);
    verify(arcadeContentRepository, times(1)).findById(contentId);
  }

  @Test
  void loadContentById_shouldReturnEmptyWhenNotFound() {
    // Arrange
    String contentId = "content-456";

    when(arcadeContentRepository.findById(contentId)).thenReturn(Optional.empty());

    // Act
    Optional<Content> result = contentPersistenceAdapter.findContentById(contentId);

    // Assert
    assertThat(result).isEmpty();
    verify(arcadeContentRepository, times(1)).findById(contentId);
  }
}
