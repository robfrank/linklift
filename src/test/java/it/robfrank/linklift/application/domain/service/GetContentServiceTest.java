package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.DownloadStatus;
import it.robfrank.linklift.application.port.in.GetContentQuery;
import it.robfrank.linklift.application.port.out.LoadContentPort;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GetContentServiceTest {

    @Mock
    private LoadContentPort loadContentPort;

    private GetContentService getContentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        getContentService = new GetContentService(loadContentPort);
    }

    @Test
    void getContent_shouldReturnContentWhenFound() {
        // Arrange
        GetContentQuery query = new GetContentQuery("link-123");
        Content expectedContent = new Content(
            "content-456",
            "link-123",
            "<html><body>Test</body></html>",
            "Test",
            1024,
            LocalDateTime.now(),
            "text/html",
            DownloadStatus.COMPLETED
        );

        when(loadContentPort.findContentByLinkId("link-123")).thenReturn(Optional.of(expectedContent));

        // Act
        Optional<Content> result = getContentService.getContent(query);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("content-456");
        assertThat(result.get().linkId()).isEqualTo("link-123");
        assertThat(result.get().htmlContent()).isEqualTo("<html><body>Test</body></html>");
        assertThat(result.get().textContent()).isEqualTo("Test");
        assertThat(result.get().contentLength()).isEqualTo(1024);
        assertThat(result.get().mimeType()).isEqualTo("text/html");
        assertThat(result.get().status()).isEqualTo(DownloadStatus.COMPLETED);

        verify(loadContentPort, times(1)).findContentByLinkId("link-123");
    }

    @Test
    void getContent_shouldReturnEmptyWhenNotFound() {
        // Arrange
        GetContentQuery query = new GetContentQuery("link-123");

        when(loadContentPort.findContentByLinkId("link-123")).thenReturn(Optional.empty());

        // Act
        Optional<Content> result = getContentService.getContent(query);

        // Assert
        assertThat(result).isEmpty();

        verify(loadContentPort, times(1)).findContentByLinkId("link-123");
    }

    @Test
    void getContent_shouldHandleNullQuery() {
        // Act & Assert
        assertThatThrownBy(() -> getContentService.getContent(null)).isInstanceOf(NullPointerException.class);

        verify(loadContentPort, never()).findContentByLinkId(any());
    }
}
