package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.CollectionWithLinks;
import org.jspecify.annotations.NonNull;

/**
 * Use case for getting a collection with its links.
 */
public interface GetCollectionUseCase {
  @NonNull
  CollectionWithLinks getCollection(@NonNull String collectionId, @NonNull String userId);
}
