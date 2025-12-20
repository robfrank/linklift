package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.ErrorCode;
import it.robfrank.linklift.application.domain.exception.LinkLiftException;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.port.in.RemoveLinkFromCollectionCommand;
import it.robfrank.linklift.application.port.out.CollectionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemoveLinkFromCollectionServiceTest {

  @Mock
  private CollectionRepository collectionRepository;

  private RemoveLinkFromCollectionService removeLinkFromCollectionService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    removeLinkFromCollectionService = new RemoveLinkFromCollectionService(collectionRepository);
  }

  @Test
  void removeLinkFromCollection_shouldRemoveLink_whenCollectionExistsAndUserOwnsIt() {
    // Arrange
    String collectionId = "collection-123";
    String linkId = "link-123";
    String userId = "user-123";
    RemoveLinkFromCollectionCommand command = new RemoveLinkFromCollectionCommand(collectionId, linkId, userId);

    Collection collection = new Collection(collectionId, "My Collection", "Description", userId, null);
    when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));

    // Act
    removeLinkFromCollectionService.removeLinkFromCollection(command);

    // Assert
    verify(collectionRepository, times(1)).findById(collectionId);
    verify(collectionRepository, times(1)).removeLinkFromCollection(collectionId, linkId);
  }

  @Test
  void removeLinkFromCollection_shouldThrowException_whenCollectionDoesNotExist() {
    // Arrange
    String collectionId = "non-existent-collection";
    String linkId = "link-123";
    String userId = "user-123";
    RemoveLinkFromCollectionCommand command = new RemoveLinkFromCollectionCommand(collectionId, linkId, userId);

    when(collectionRepository.findById(collectionId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> removeLinkFromCollectionService.removeLinkFromCollection(command))
      .isInstanceOf(LinkLiftException.class)
      .hasMessageContaining("Collection not found")
      .extracting("errorCode")
      .isEqualTo(ErrorCode.COLLECTION_NOT_FOUND);

    verify(collectionRepository, times(1)).findById(collectionId);
    verify(collectionRepository, never()).removeLinkFromCollection(any(), any());
  }

  @Test
  void removeLinkFromCollection_shouldThrowException_whenUserDoesNotOwnCollection() {
    // Arrange
    String collectionId = "collection-123";
    String linkId = "link-123";
    String userId = "user-123";
    String otherUserId = "other-user-456";
    RemoveLinkFromCollectionCommand command = new RemoveLinkFromCollectionCommand(collectionId, linkId, userId);

    Collection collection = new Collection(collectionId, "My Collection", "Description", otherUserId, null);
    when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));

    // Act & Assert
    assertThatThrownBy(() -> removeLinkFromCollectionService.removeLinkFromCollection(command))
      .isInstanceOf(LinkLiftException.class)
      .hasMessageContaining("User does not have access to this collection")
      .extracting("errorCode")
      .isEqualTo(ErrorCode.UNAUTHORIZED);

    verify(collectionRepository, times(1)).findById(collectionId);
    verify(collectionRepository, never()).removeLinkFromCollection(any(), any());
  }
}
