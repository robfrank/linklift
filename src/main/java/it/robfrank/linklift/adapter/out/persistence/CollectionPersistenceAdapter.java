package it.robfrank.linklift.adapter.out.persistence;

import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.out.CollectionRepository;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public class CollectionPersistenceAdapter implements CollectionRepository {

  private final ArcadeCollectionRepository repository;

  public CollectionPersistenceAdapter(ArcadeCollectionRepository repository) {
    this.repository = repository;
  }

  @Override
  @NonNull
  public Collection save(@NonNull Collection collection) {
    return repository.save(collection);
  }

  @Override
  @NonNull
  public List<Collection> findByUserId(@NonNull String userId) {
    return repository.findByUserId(userId);
  }

  @Override
  @NonNull
  public Optional<Collection> findById(@NonNull String collectionId) {
    return repository.findById(collectionId);
  }

  @Override
  public void addLinkToCollection(@NonNull String collectionId, @NonNull String linkId) {
    repository.addLinkToCollection(collectionId, linkId);
  }

  @Override
  public void removeLinkFromCollection(@NonNull String collectionId, @NonNull String linkId) {
    repository.removeLinkFromCollection(collectionId, linkId);
  }

  @Override
  @NonNull
  public List<Link> getCollectionLinks(@NonNull String collectionId) {
    return repository.getCollectionLinks(collectionId);
  }

  @Override
  public void deleteCollection(@NonNull String collectionId) {
    repository.deleteCollection(collectionId);
  }
}
