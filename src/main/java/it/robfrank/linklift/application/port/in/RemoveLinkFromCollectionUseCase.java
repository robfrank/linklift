package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

/**
 * Use case for removing a link from a collection.
 */
public interface RemoveLinkFromCollectionUseCase {
  void removeLinkFromCollection(@NonNull RemoveLinkFromCollectionCommand command);
}
