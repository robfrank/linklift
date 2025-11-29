package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

/**
 * Use case for deleting a collection.
 */
public interface DeleteCollectionUseCase {
  void deleteCollection(@NonNull String collectionId, @NonNull String userId);
}
