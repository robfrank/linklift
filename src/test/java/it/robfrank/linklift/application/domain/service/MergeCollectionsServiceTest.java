package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.robfrank.linklift.application.domain.exception.LinkLiftException;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.port.in.MergeCollectionsCommand;
import it.robfrank.linklift.application.port.out.CollectionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MergeCollectionsServiceTest {

  @Mock
  private CollectionRepository collectionRepository;

  private MergeCollectionsService mergeCollectionsService;

  @BeforeEach
  void setUp() {
    mergeCollectionsService = new MergeCollectionsService(collectionRepository);
  }

  @Test
  void shouldMergeCollections() {
    String userId = "user-123";
    String sourceId = "source-1";
    String targetId = "target-1";
    MergeCollectionsCommand command = new MergeCollectionsCommand(sourceId, targetId, userId);

    Collection source = new Collection(sourceId, "Source", null, userId, null, null);
    Collection target = new Collection(targetId, "Target", null, userId, null, null);

    when(collectionRepository.findById(sourceId)).thenReturn(Optional.of(source));
    when(collectionRepository.findById(targetId)).thenReturn(Optional.of(target));

    mergeCollectionsService.mergeCollections(command);

    verify(collectionRepository).mergeCollections(sourceId, targetId);
  }

  @Test
  void shouldThrowExceptionWhenSourceDoesNotExist() {
    String userId = "user-123";
    MergeCollectionsCommand command = new MergeCollectionsCommand("non-existent", "target-1", userId);

    when(collectionRepository.findById("non-existent")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> mergeCollectionsService.mergeCollections(command))
      .isInstanceOf(LinkLiftException.class)
      .hasMessageContaining("Source collection not found");
  }

  @Test
  void shouldThrowExceptionWhenUserDoesNotOwnSource() {
    String userId = "user-123";
    String sourceId = "source-1";
    MergeCollectionsCommand command = new MergeCollectionsCommand(sourceId, "target-1", userId);

    Collection source = new Collection(sourceId, "Source", null, "other-user", null, null);

    when(collectionRepository.findById(sourceId)).thenReturn(Optional.of(source));

    assertThatThrownBy(() -> mergeCollectionsService.mergeCollections(command))
      .isInstanceOf(LinkLiftException.class)
      .hasMessageContaining("User does not have access to source collection");
  }
}
