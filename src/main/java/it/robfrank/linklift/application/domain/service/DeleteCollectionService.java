package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.ErrorCode;
import it.robfrank.linklift.application.domain.exception.LinkLiftException;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.port.in.DeleteCollectionUseCase;
import it.robfrank.linklift.application.port.out.CollectionRepository;
import org.jspecify.annotations.NonNull;

/**
 * Service implementation for deleting a collection.
 */
public class DeleteCollectionService implements DeleteCollectionUseCase {

  private final CollectionRepository collectionRepository;

  public DeleteCollectionService(CollectionRepository collectionRepository) {
    this.collectionRepository = collectionRepository;
  }

  @Override
  public void deleteCollection(@NonNull String collectionId, @NonNull String userId) {
    // Verify collection exists and belongs to user
    Collection collection = collectionRepository
      .findById(collectionId)
      .orElseThrow(() -> new LinkLiftException("Collection not found: " + collectionId, ErrorCode.COLLECTION_NOT_FOUND));

    if (!collection.userId().equals(userId)) {
      throw new LinkLiftException("User does not have access to this collection", ErrorCode.UNAUTHORIZED);
    }

    collectionRepository.deleteCollection(collectionId);
  }
}
