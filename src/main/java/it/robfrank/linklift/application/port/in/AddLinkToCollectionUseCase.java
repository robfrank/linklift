package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

/**
 * Use case for adding a link to a collection.
 */
public interface AddLinkToCollectionUseCase {
  void addLinkToCollection(@NonNull AddLinkToCollectionCommand command);
}
