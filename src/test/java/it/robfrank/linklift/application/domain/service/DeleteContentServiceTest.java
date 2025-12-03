package it.robfrank.linklift.application.domain.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import it.robfrank.linklift.application.port.in.DeleteContentCommand;
import it.robfrank.linklift.application.port.out.SaveContentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DeleteContentServiceTest {

  @Mock
  private SaveContentPort saveContentPort;

  private DeleteContentService deleteContentService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    deleteContentService = new DeleteContentService(saveContentPort);
  }

  @Test
  void deleteContent_shouldDeleteContent_whenValidCommand() {
    // Arrange
    String linkId = "link-123";
    DeleteContentCommand command = new DeleteContentCommand(linkId);

    // Act
    deleteContentService.deleteContent(command);

    // Assert
    verify(saveContentPort, times(1)).deleteContentByLinkId(linkId);
  }

  @Test
  void deleteContent_shouldCallPortForEachInvocation() {
    // Arrange
    String linkId = "link-123";
    DeleteContentCommand command = new DeleteContentCommand(linkId);

    // Act
    deleteContentService.deleteContent(command);
    deleteContentService.deleteContent(command);

    // Assert
    verify(saveContentPort, times(2)).deleteContentByLinkId(linkId);
  }
}
