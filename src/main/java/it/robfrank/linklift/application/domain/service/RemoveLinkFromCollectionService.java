package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.ErrorCode;
import it.robfrank.linklift.application.domain.exception.LinkLiftException;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.port.in.RemoveLinkFromCollectionCommand;
import it.robfrank.linklift.application.port.in.RemoveLinkFromCollectionUseCase;
import it.robfrank.linklift.application.port.out.CollectionRepository;
import org.jspecify.annotations.NonNull;

/**
 * Service implementation for removing a link from a collection.
 */
public class RemoveLinkFromCollectionService implements RemoveLinkFromCollectionUseCase {

  private final CollectionRepository collectionRepository;

  public RemoveLinkFromCollectionService(CollectionRepository collectionRepository) {
    this.collectionRepository = collectionRepository;
  }

  @Override
  public void removeLinkFromCollection(@NonNull RemoveLinkFromCollectionCommand command) {
    it.robfrank.linklift.application.domain.validation.ValidationUtils.requireNotEmpty(command.collectionId(), "collectionId");
    it.robfrank.linklift.application.domain.validation.ValidationUtils.requireNotEmpty(command.linkId(), "linkId");
    it.robfrank.linklift.application.domain.validation.ValidationUtils.requireNotEmpty(command.userId(), "userId");

    // Verify collection exists and belongs to user
    Collection collection = collectionRepository
      .findById(command.collectionId())
      .orElseThrow(() -> new LinkLiftException("Collection not found: " + command.collectionId(), ErrorCode.COLLECTION_NOT_FOUND));

    if (!collection.userId().equals(command.userId())) {
      throw new LinkLiftException("User does not have access to this collection", ErrorCode.UNAUTHORIZED);
    }

    collectionRepository.removeLinkFromCollection(command.collectionId(), command.linkId());
  }
}
