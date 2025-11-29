package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.adapter.out.event.SimpleEventPublisher;
import it.robfrank.linklift.adapter.out.persistence.LinkPersistenceAdapter;
import it.robfrank.linklift.application.domain.event.LinkCreatedEvent;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.in.DownloadContentCommand;
import it.robfrank.linklift.application.port.in.DownloadContentUseCase;
import it.robfrank.linklift.application.port.in.NewLinkCommand;
import it.robfrank.linklift.application.port.out.DomainEventPublisher;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class NewLinkServiceTest {

  @Mock
  private LinkPersistenceAdapter linkPersistenceAdapter;

  private DomainEventPublisher eventPublisher;

  @Mock
  private DownloadContentUseCase downloadContentUseCase;

  private NewLinkService newLinkService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    eventPublisher = new SimpleEventPublisher();
    ((SimpleEventPublisher) eventPublisher).subscribe(LinkCreatedEvent.class, event ->
        downloadContentUseCase.downloadContentAsync(new DownloadContentCommand(event.getLink().id(), event.getLink().url()))
      );

    newLinkService = new NewLinkService(linkPersistenceAdapter, eventPublisher);
  }

  @Test
  void newLink_shouldCreateLinkWithCorrectData() {
    // Arrange
    NewLinkCommand command = new NewLinkCommand("https://example.com", "Example Title", "Example Description", "user-123");

    Link expectedLink = new Link("test-id", "https://example.com", "Example Title", "Example Description", LocalDateTime.now(), "text/html");

    when(linkPersistenceAdapter.saveLinkForUser(any(Link.class), eq("user-123"))).thenReturn(expectedLink);
    // Act
    Link result = newLinkService.newLink(command);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.url()).isEqualTo(command.url());
    assertThat(result.title()).isEqualTo(command.title());
    assertThat(result.description()).isEqualTo(command.description());

    // Verify link was saved with correct data
    ArgumentCaptor<Link> linkCaptor = ArgumentCaptor.forClass(Link.class);
    verify(linkPersistenceAdapter, times(1)).saveLinkForUser(linkCaptor.capture(), eq("user-123"));

    Link capturedLink = linkCaptor.getValue();
    assertThat(capturedLink.id()).isNotNull();
    assertThat(capturedLink.url()).isEqualTo(command.url());
    assertThat(capturedLink.title()).isEqualTo(command.title());
    assertThat(capturedLink.description()).isEqualTo(command.description());
    assertThat(capturedLink.contentType()).isEqualTo("text/html");

    // Verify event was published
    //        ArgumentCaptor<LinkCreatedEvent> eventCaptor = ArgumentCaptor.forClass(LinkCreatedEvent.class);
    //        verify(eventPublisher, times(1)).publish(eventCaptor.capture());

    //        LinkCreatedEvent capturedEvent = eventCaptor.getValue();
    //        assertThat(capturedEvent.getLink()).isEqualTo(expectedLink);
    //        assertThat(capturedEvent.getUserId()).isEqualTo("user-123");

    // Verify async content download was triggered
    verify(downloadContentUseCase, times(1)).downloadContentAsync(any());
  }
}
