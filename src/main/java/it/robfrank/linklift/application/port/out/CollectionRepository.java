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
}
