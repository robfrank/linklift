package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.ErrorCode;
import it.robfrank.linklift.application.domain.exception.LinkLiftException;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.port.in.AddLinkToCollectionCommand;
import it.robfrank.linklift.application.port.out.CollectionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddLinkToCollectionServiceTest {

  @Mock
  private CollectionRepository collectionRepository;

  private AddLinkToCollectionService addLinkToCollectionService;

  @BeforeEach
  void setUp() {
    addLinkToCollectionService = new AddLinkToCollectionService(collectionRepository);
  }

  @Test
  void addLinkToCollection_shouldAddLink_whenCollectionExistsAndUserOwnsIt() {
    // Arrange
    String collectionId = "collection-123";
    String linkId = "link-123";
    String userId = "user-123";
    AddLinkToCollectionCommand command = new AddLinkToCollectionCommand(collectionId, linkId, userId);

    Collection collection = new Collection(collectionId, "My Collection", "Description", userId, null, null);
    when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));

    // Act
    addLinkToCollectionService.addLinkToCollection(command);

    // Assert
    verify(collectionRepository, times(1)).findById(collectionId);
    verify(collectionRepository, times(1)).addLinkToCollection(collectionId, linkId);
  }

  @Test
  void addLinkToCollection_shouldThrowException_whenCollectionDoesNotExist() {
    // Arrange
    String collectionId = "non-existent-collection";
    String linkId = "link-123";
    String userId = "user-123";
    AddLinkToCollectionCommand command = new AddLinkToCollectionCommand(collectionId, linkId, userId);

    when(collectionRepository.findById(collectionId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> addLinkToCollectionService.addLinkToCollection(command))
      .isInstanceOf(LinkLiftException.class)
      .hasMessageContaining("Collection not found")
      .extracting("errorCode")
      .isEqualTo(ErrorCode.COLLECTION_NOT_FOUND);

    verify(collectionRepository, times(1)).findById(collectionId);
    verify(collectionRepository, never()).addLinkToCollection(any(), any());
  }

  @Test
  void addLinkToCollection_shouldThrowException_whenUserDoesNotOwnCollection() {
    // Arrange
    String collectionId = "collection-123";
    String linkId = "link-123";
    String userId = "user-123";
    String otherUserId = "other-user-456";
    AddLinkToCollectionCommand command = new AddLinkToCollectionCommand(collectionId, linkId, userId);

    Collection collection = new Collection(collectionId, "My Collection", "Description", otherUserId, null, null);
    when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));

    // Act & Assert
    assertThatThrownBy(() -> addLinkToCollectionService.addLinkToCollection(command))
      .isInstanceOf(LinkLiftException.class)
      .hasMessageContaining("User does not have access to this collection")
      .extracting("errorCode")
      .isEqualTo(ErrorCode.UNAUTHORIZED);

    verify(collectionRepository, times(1)).findById(collectionId);
    verify(collectionRepository, never()).addLinkToCollection(any(), any());
  }
}
