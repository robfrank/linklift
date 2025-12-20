package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.ErrorCode;
import it.robfrank.linklift.application.domain.exception.LinkLiftException;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.port.out.CollectionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteCollectionServiceTest {

  @Mock
  private CollectionRepository collectionRepository;

  private DeleteCollectionService deleteCollectionService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    deleteCollectionService = new DeleteCollectionService(collectionRepository);
  }

  @Test
  void deleteCollection_shouldDeleteCollection_whenCollectionExistsAndUserOwnsIt() {
    // Arrange
    String collectionId = "collection-123";
    String userId = "user-123";

    Collection collection = new Collection(collectionId, "My Collection", "Description", userId, null);
    when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));

    // Act
    deleteCollectionService.deleteCollection(collectionId, userId);

    // Assert
    verify(collectionRepository, times(1)).findById(collectionId);
    verify(collectionRepository, times(1)).deleteCollection(collectionId);
  }

  @Test
  void deleteCollection_shouldThrowException_whenCollectionDoesNotExist() {
    // Arrange
    String collectionId = "non-existent-collection";
    String userId = "user-123";

    when(collectionRepository.findById(collectionId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> deleteCollectionService.deleteCollection(collectionId, userId))
      .isInstanceOf(LinkLiftException.class)
      .hasMessageContaining("Collection not found")
      .extracting("errorCode")
      .isEqualTo(ErrorCode.COLLECTION_NOT_FOUND);

    verify(collectionRepository, times(1)).findById(collectionId);
    verify(collectionRepository, never()).deleteCollection(any());
  }

  @Test
  void deleteCollection_shouldThrowException_whenUserDoesNotOwnCollection() {
    // Arrange
    String collectionId = "collection-123";
    String userId = "user-123";
    String otherUserId = "other-user-456";

    Collection collection = new Collection(collectionId, "My Collection", "Description", otherUserId, null);
    when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));

    // Act & Assert
    assertThatThrownBy(() -> deleteCollectionService.deleteCollection(collectionId, userId))
      .isInstanceOf(LinkLiftException.class)
      .hasMessageContaining("User does not have access to this collection")
      .extracting("errorCode")
      .isEqualTo(ErrorCode.UNAUTHORIZED);

    verify(collectionRepository, times(1)).findById(collectionId);
    verify(collectionRepository, never()).deleteCollection(any());
  }
}
