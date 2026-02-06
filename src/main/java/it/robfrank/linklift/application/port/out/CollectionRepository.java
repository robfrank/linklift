package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.domain.model.Link;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public interface CollectionRepository {
  @NonNull
  Collection save(@NonNull Collection collection);

  @NonNull
  List<Collection> findByUserId(@NonNull String userId);

  @NonNull
  Optional<Collection> findById(@NonNull String collectionId);

  void addLinkToCollection(@NonNull String collectionId, @NonNull String linkId);

  void removeLinkFromCollection(@NonNull String collectionId, @NonNull String linkId);

  @NonNull
  List<Link> getCollectionLinks(@NonNull String collectionId);

  void deleteCollection(@NonNull String collectionId);

  /**
   * Merges the source collection into the target collection.
   * All links from the source collection will be moved to the target collection.
   * The source collection will be deleted after the merge.
   *
   * @param sourceCollectionId the ID of the collection to merge from
   * @param targetCollectionId the ID of the collection to merge into
   */
  void mergeCollections(@NonNull String sourceCollectionId, @NonNull String targetCollectionId);
}
