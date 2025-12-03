package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.ErrorCode;
import it.robfrank.linklift.application.domain.exception.LinkLiftException;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.AddLinkToCollectionCommand;
import it.robfrank.linklift.application.port.in.AddLinkToCollectionUseCase;
import it.robfrank.linklift.application.port.out.CollectionRepository;
import org.jspecify.annotations.NonNull;

/**
 * Service implementation for adding a link to a collection.
 */
public class AddLinkToCollectionService implements AddLinkToCollectionUseCase {

  private final CollectionRepository collectionRepository;

  public AddLinkToCollectionService(CollectionRepository collectionRepository) {
    this.collectionRepository = collectionRepository;
  }

  @Override
  public void addLinkToCollection(@NonNull AddLinkToCollectionCommand command) {
    ValidationUtils.requireNotEmpty(command.collectionId(), "collectionId");
    ValidationUtils.requireNotEmpty(command.linkId(), "linkId");
    ValidationUtils.requireNotEmpty(command.userId(), "userId");

    // Verify collection exists and belongs to user
    Collection collection = collectionRepository
      .findById(command.collectionId())
      .orElseThrow(() -> new LinkLiftException("Collection not found: " + command.collectionId(), ErrorCode.COLLECTION_NOT_FOUND));

    if (!collection.userId().equals(command.userId())) {
      throw new LinkLiftException("User does not have access to this collection", ErrorCode.UNAUTHORIZED);
    }

    collectionRepository.addLinkToCollection(command.collectionId(), command.linkId());
  }
}
