package it.robfrank.linklift.adapter.out.persitence;

import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.port.out.CollectionRepository;

public class CollectionPersistenceAdapter implements CollectionRepository {

  private final ArcadeCollectionRepository repository;

  public CollectionPersistenceAdapter(ArcadeCollectionRepository repository) {
    this.repository = repository;
  }

  @Override
  public Collection save(Collection collection) {
    return repository.save(collection);
  }
}
