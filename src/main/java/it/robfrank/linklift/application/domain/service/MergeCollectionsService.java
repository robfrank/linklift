package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.ErrorCode;
import it.robfrank.linklift.application.domain.exception.LinkLiftException;
import it.robfrank.linklift.application.port.in.MergeCollectionsCommand;
import it.robfrank.linklift.application.port.in.MergeCollectionsUseCase;
import it.robfrank.linklift.application.port.out.CollectionRepository;
import org.jspecify.annotations.NonNull;

public class MergeCollectionsService implements MergeCollectionsUseCase {

  private final CollectionRepository collectionRepository;

  public MergeCollectionsService(CollectionRepository collectionRepository) {
    this.collectionRepository = collectionRepository;
  }

  @Override
  public void mergeCollections(@NonNull MergeCollectionsCommand command) {
    var source = collectionRepository
      .findById(command.sourceCollectionId())
      .orElseThrow(() -> new LinkLiftException("Source collection not found", ErrorCode.COLLECTION_NOT_FOUND));

    if (!source.userId().equals(command.userId())) {
      throw new LinkLiftException("User does not have access to source collection", ErrorCode.UNAUTHORIZED);
    }

    var target = collectionRepository
      .findById(command.targetCollectionId())
      .orElseThrow(() -> new LinkLiftException("Target collection not found", ErrorCode.COLLECTION_NOT_FOUND));

    if (!target.userId().equals(command.userId())) {
      throw new LinkLiftException("User does not have access to target collection", ErrorCode.UNAUTHORIZED);
    }

    collectionRepository.mergeCollections(command.sourceCollectionId(), command.targetCollectionId());
  }
}
