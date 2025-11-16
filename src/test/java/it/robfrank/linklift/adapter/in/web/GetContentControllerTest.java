package it.robfrank.linklift.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import it.robfrank.linklift.application.domain.exception.ContentNotFoundException;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import it.robfrank.linklift.application.port.in.GetContentQuery;
import it.robfrank.linklift.application.port.in.GetContentUseCase;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GetContentControllerTest {

  @Mock
  private GetContentUseCase getContentUseCase;

  @Mock
  private Context context;

  private GetContentController getContentController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    getContentController = new GetContentController(getContentUseCase);
  }

  @Test
  void getContent_shouldReturnContentWhenFound() {
    // Arrange
    String linkId = "link-123";
    Content content = new Content(
      "content-456",
      linkId,
      "<html><body>Test content</body></html>",
      "Test content",
      2048,
      LocalDateTime.of(2025, 10, 16, 10, 30),
      "text/html",
      DownloadStatus.COMPLETED
    );

    when(context.pathParam("linkId")).thenReturn(linkId);
    when(getContentUseCase.getContent(any(GetContentQuery.class))).thenReturn(Optional.of(content));

    // Act
    getContentController.getContent(context);

    // Assert
    ArgumentCaptor<GetContentQuery> queryCaptor = ArgumentCaptor.forClass(GetContentQuery.class);
    verify(getContentUseCase, times(1)).getContent(queryCaptor.capture());

    GetContentQuery capturedQuery = queryCaptor.getValue();
    assertThat(capturedQuery.linkId()).isEqualTo(linkId);

    ArgumentCaptor<GetContentController.ContentResponse> responseCaptor = ArgumentCaptor.forClass(GetContentController.ContentResponse.class);
    verify(context, times(1)).json(responseCaptor.capture());

    GetContentController.ContentResponse response = responseCaptor.getValue();
    assertThat(response.data()).isEqualTo(content);
    assertThat(response.message()).isEqualTo("Content retrieved successfully");
  }

  @Test
  void getContent_shouldThrowExceptionWhenContentNotFound() {
    // Arrange
    String linkId = "link-123";

    when(context.pathParam("linkId")).thenReturn(linkId);
    when(getContentUseCase.getContent(any(GetContentQuery.class))).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> getContentController.getContent(context)).isInstanceOf(ContentNotFoundException.class).hasMessageContaining(linkId);

    verify(getContentUseCase, times(1)).getContent(any(GetContentQuery.class));
    verify(context, never()).json(any());
  }

  @Test
  void getContent_shouldHandlePendingContent() {
    // Arrange
    String linkId = "link-123";
    Content content = new Content("content-456", linkId, null, null, 0, LocalDateTime.now(), null, DownloadStatus.IN_PROGRESS);

    when(context.pathParam("linkId")).thenReturn(linkId);
    when(getContentUseCase.getContent(any(GetContentQuery.class))).thenReturn(Optional.of(content));

    // Act
    getContentController.getContent(context);

    // Assert
    ArgumentCaptor<GetContentController.ContentResponse> responseCaptor = ArgumentCaptor.forClass(GetContentController.ContentResponse.class);
    verify(context, times(1)).json(responseCaptor.capture());

    GetContentController.ContentResponse response = responseCaptor.getValue();
    assertThat(response.data().status()).isEqualTo(DownloadStatus.IN_PROGRESS);
    assertThat(response.data().htmlContent()).isNull();
    assertThat(response.data().textContent()).isNull();
  }

  @Test
  void getContent_shouldHandleFailedContent() {
    // Arrange
    String linkId = "link-123";
    Content content = new Content("content-456", linkId, null, null, 0, LocalDateTime.now(), null, DownloadStatus.FAILED);

    when(context.pathParam("linkId")).thenReturn(linkId);
    when(getContentUseCase.getContent(any(GetContentQuery.class))).thenReturn(Optional.of(content));

    // Act
    getContentController.getContent(context);

    // Assert
    ArgumentCaptor<GetContentController.ContentResponse> responseCaptor = ArgumentCaptor.forClass(GetContentController.ContentResponse.class);
    verify(context, times(1)).json(responseCaptor.capture());

    GetContentController.ContentResponse response = responseCaptor.getValue();
    assertThat(response.data().status()).isEqualTo(DownloadStatus.FAILED);
  }

  @Test
  void getContent_shouldReturnCompleteContentData() {
    // Arrange
    String linkId = "link-123";
    Content content = new Content(
      "content-456",
      linkId,
      "<html><body><h1>Title</h1><p>Content</p></body></html>",
      "Title Content",
      5120,
      LocalDateTime.of(2025, 10, 16, 14, 45),
      "text/html; charset=UTF-8",
      DownloadStatus.COMPLETED
    );

    when(context.pathParam("linkId")).thenReturn(linkId);
    when(getContentUseCase.getContent(any(GetContentQuery.class))).thenReturn(Optional.of(content));

    // Act
    getContentController.getContent(context);

    // Assert
    ArgumentCaptor<GetContentController.ContentResponse> responseCaptor = ArgumentCaptor.forClass(GetContentController.ContentResponse.class);
    verify(context, times(1)).json(responseCaptor.capture());

    GetContentController.ContentResponse response = responseCaptor.getValue();
    Content responseContent = response.data();

    assertThat(responseContent.id()).isEqualTo("content-456");
    assertThat(responseContent.linkId()).isEqualTo(linkId);
    assertThat(responseContent.htmlContent()).contains("<h1>Title</h1>");
    assertThat(responseContent.textContent()).contains("Title Content");
    assertThat(responseContent.contentLength()).isEqualTo(5120);
    assertThat(responseContent.mimeType()).isEqualTo("text/html; charset=UTF-8");
    assertThat(responseContent.status()).isEqualTo(DownloadStatus.COMPLETED);
  }
}
