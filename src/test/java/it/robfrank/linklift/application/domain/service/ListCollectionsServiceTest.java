package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.port.out.CollectionRepository;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ListCollectionsServiceTest {

  @Mock
  private CollectionRepository collectionRepository;

  private ListCollectionsService listCollectionsService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    listCollectionsService = new ListCollectionsService(collectionRepository);
  }

  @Test
  void listCollections_shouldReturnCollections_whenUserHasCollections() {
    // Arrange
    String userId = "user-123";
    List<Collection> collections = Arrays.asList(
      new Collection("col-1", "Collection 1", "Description 1", userId, null),
      new Collection("col-2", "Collection 2", "Description 2", userId, null),
      new Collection("col-3", "Collection 3", "Description 3", userId, null)
    );

    when(collectionRepository.findByUserId(userId)).thenReturn(collections);

    // Act
    List<Collection> result = listCollectionsService.listCollections(userId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).hasSize(3);
    assertThat(result).containsExactlyElementsOf(collections);

    verify(collectionRepository, times(1)).findByUserId(userId);
  }

  @Test
  void listCollections_shouldReturnEmptyList_whenUserHasNoCollections() {
    // Arrange
    String userId = "user-123";
    List<Collection> emptyCollections = List.of();

    when(collectionRepository.findByUserId(userId)).thenReturn(emptyCollections);

    // Act
    List<Collection> result = listCollectionsService.listCollections(userId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();

    verify(collectionRepository, times(1)).findByUserId(userId);
  }

  @Test
  void listCollections_shouldOnlyReturnUserCollections_whenMultipleUsersExist() {
    // Arrange
    String userId = "user-123";
    List<Collection> userCollections = Arrays.asList(
      new Collection("col-1", "Collection 1", "Description 1", userId, null),
      new Collection("col-2", "Collection 2", "Description 2", userId, null)
    );

    when(collectionRepository.findByUserId(userId)).thenReturn(userCollections);

    // Act
    List<Collection> result = listCollectionsService.listCollections(userId);

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result).allMatch(collection -> collection.userId().equals(userId));
  }

  @Test
  void listCollections_shouldThrowValidationException_whenUserIdIsNull() {
    // Act & Assert
    assertThatThrownBy(() -> listCollectionsService.listCollections(null))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("userId cannot be empty");

    verify(collectionRepository, never()).findByUserId(any());
  }

  @Test
  void listCollections_shouldThrowValidationException_whenUserIdIsEmpty() {
    // Act & Assert
    assertThatThrownBy(() -> listCollectionsService.listCollections("")).isInstanceOf(ValidationException.class).hasMessageContaining("userId cannot be empty");

    verify(collectionRepository, never()).findByUserId(any());
  }

  @Test
  void listCollections_shouldThrowValidationException_whenUserIdIsBlank() {
    // Act & Assert
    assertThatThrownBy(() -> listCollectionsService.listCollections("   "))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("userId cannot be empty");

    verify(collectionRepository, never()).findByUserId(any());
  }
}
