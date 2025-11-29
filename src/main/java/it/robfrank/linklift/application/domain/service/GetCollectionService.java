package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.ErrorCode;
import it.robfrank.linklift.application.domain.exception.LinkLiftException;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.domain.model.CollectionWithLinks;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.in.GetCollectionUseCase;
import it.robfrank.linklift.application.port.out.CollectionRepository;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Service implementation for getting a collection with its links.
 */
public class GetCollectionService implements GetCollectionUseCase {

  private final CollectionRepository collectionRepository;

  public GetCollectionService(CollectionRepository collectionRepository) {
    this.collectionRepository = collectionRepository;
  }

  @Override
  @NonNull
  public CollectionWithLinks getCollection(@NonNull String collectionId, @NonNull String userId) {
    Collection collection = collectionRepository
      .findById(collectionId)
      .orElseThrow(() -> new LinkLiftException("Collection not found: " + collectionId, ErrorCode.COLLECTION_NOT_FOUND));

    // Verify the collection belongs to the user
    if (!collection.userId().equals(userId)) {
      throw new LinkLiftException("User does not have access to this collection", ErrorCode.UNAUTHORIZED);
    }

    List<Link> links = collectionRepository.getCollectionLinks(collectionId);

    return new CollectionWithLinks(collection, links);
  }
}
