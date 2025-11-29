package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.port.in.ListCollectionsUseCase;
import it.robfrank.linklift.application.port.out.CollectionRepository;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Service implementation for listing collections.
 */
public class ListCollectionsService implements ListCollectionsUseCase {

  private final CollectionRepository collectionRepository;

  public ListCollectionsService(CollectionRepository collectionRepository) {
    this.collectionRepository = collectionRepository;
  }

  @Override
  @NonNull
  public List<Collection> listCollections(@NonNull String userId) {
    return collectionRepository.findByUserId(userId);
  }
}
