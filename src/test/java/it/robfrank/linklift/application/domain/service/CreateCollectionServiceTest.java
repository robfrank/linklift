package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.port.in.CreateCollectionCommand;
import it.robfrank.linklift.application.port.out.CollectionRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CreateCollectionServiceTest {

  @Mock
  private CollectionRepository collectionRepository;

  private CreateCollectionService createCollectionService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    createCollectionService = new CreateCollectionService(collectionRepository);
  }

  @Test
  void createCollection_shouldCreateCollectionWithCorrectData() {
    // Arrange
    String userId = "user-123";
    CreateCollectionCommand command = new CreateCollectionCommand("My Collection", "A test collection", userId, null);

    when(collectionRepository.save(any(Collection.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Collection result = createCollectionService.createCollection(command);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("My Collection");
    assertThat(result.description()).isEqualTo("A test collection");
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.id()).isNotNull();

    // Verify repository interaction
    ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
    verify(collectionRepository, times(1)).save(captor.capture());

    Collection savedCollection = captor.getValue();
    assertThat(savedCollection.name()).isEqualTo("My Collection");
    assertThat(savedCollection.description()).isEqualTo("A test collection");
    assertThat(savedCollection.userId()).isEqualTo(userId);
  }

  @Test
  void createCollection_shouldCreateCollectionWithQuery_whenQueryProvided() {
    // Arrange
    String userId = "user-123";
    String queryTime = "test-query";
    CreateCollectionCommand command = new CreateCollectionCommand("Query Collection", "A collection with query", userId, queryTime);

    when(collectionRepository.save(any(Collection.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Collection result = createCollectionService.createCollection(command);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.query()).isEqualTo("test-query");

    // Verify repository interaction
    verify(collectionRepository, times(1)).save(any(Collection.class));
  }

  @Test
  void createCollection_shouldGenerateUniqueIds_whenCalledMultipleTimes() {
    // Arrange
    String userId = "user-123";
    CreateCollectionCommand command1 = new CreateCollectionCommand("Collection 1", "Description 1", userId, null);
    CreateCollectionCommand command2 = new CreateCollectionCommand("Collection 2", "Description 2", userId, null);

    when(collectionRepository.save(any(Collection.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Collection result1 = createCollectionService.createCollection(command1);
    Collection result2 = createCollectionService.createCollection(command2);

    // Assert
    assertThat(result1.id()).isNotEqualTo(result2.id());
    verify(collectionRepository, times(2)).save(any(Collection.class));
  }
}
