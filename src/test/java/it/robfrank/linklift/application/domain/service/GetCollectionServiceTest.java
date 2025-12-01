package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.ErrorCode;
import it.robfrank.linklift.application.domain.exception.LinkLiftException;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.domain.model.CollectionWithLinks;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.out.CollectionRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GetCollectionServiceTest {

  @Mock
  private CollectionRepository collectionRepository;

  private GetCollectionService getCollectionService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    getCollectionService = new GetCollectionService(collectionRepository);
  }

  @Test
  void getCollection_shouldReturnCollectionWithLinks_whenCollectionExistsAndUserOwnsIt() {
    // Arrange
    String collectionId = "collection-123";
    String userId = "user-123";

    Collection collection = new Collection(collectionId, "My Collection", "Description", userId, null);
    List<Link> links = Arrays.asList(
      new Link("link-1", "https://example.com/1", "Title 1", "Desc 1", LocalDateTime.now(), "text/html"),
      new Link("link-2", "https://example.com/2", "Title 2", "Desc 2", LocalDateTime.now(), "text/html")
    );

    when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));
    when(collectionRepository.getCollectionLinks(collectionId)).thenReturn(links);

    // Act
    CollectionWithLinks result = getCollectionService.getCollection(collectionId, userId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.collection()).isEqualTo(collection);
    assertThat(result.links()).hasSize(2);
    assertThat(result.links()).containsExactlyElementsOf(links);

    verify(collectionRepository, times(1)).findById(collectionId);
    verify(collectionRepository, times(1)).getCollectionLinks(collectionId);
  }

  @Test
  void getCollection_shouldReturnEmptyLinks_whenCollectionHasNoLinks() {
    // Arrange
    String collectionId = "collection-123";
    String userId = "user-123";

    Collection collection = new Collection(collectionId, "Empty Collection", "Description", userId, null);
    List<Link> emptyLinks = List.of();

    when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));
    when(collectionRepository.getCollectionLinks(collectionId)).thenReturn(emptyLinks);

    // Act
    CollectionWithLinks result = getCollectionService.getCollection(collectionId, userId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.collection()).isEqualTo(collection);
    assertThat(result.links()).isEmpty();
  }

  @Test
  void getCollection_shouldThrowException_whenCollectionDoesNotExist() {
    // Arrange
    String collectionId = "non-existent-collection";
    String userId = "user-123";

    when(collectionRepository.findById(collectionId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> getCollectionService.getCollection(collectionId, userId))
      .isInstanceOf(LinkLiftException.class)
      .hasMessageContaining("Collection not found")
      .extracting("errorCode")
      .isEqualTo(ErrorCode.COLLECTION_NOT_FOUND);

    verify(collectionRepository, times(1)).findById(collectionId);
    verify(collectionRepository, never()).getCollectionLinks(any());
  }

  @Test
  void getCollection_shouldThrowException_whenUserDoesNotOwnCollection() {
    // Arrange
    String collectionId = "collection-123";
    String userId = "user-123";
    String otherUserId = "other-user-456";

    Collection collection = new Collection(collectionId, "My Collection", "Description", otherUserId, null);
    when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));

    // Act & Assert
    assertThatThrownBy(() -> getCollectionService.getCollection(collectionId, userId))
      .isInstanceOf(LinkLiftException.class)
      .hasMessageContaining("User does not have access to this collection")
      .extracting("errorCode")
      .isEqualTo(ErrorCode.UNAUTHORIZED);

    verify(collectionRepository, times(1)).findById(collectionId);
    verify(collectionRepository, never()).getCollectionLinks(any());
  }
}
