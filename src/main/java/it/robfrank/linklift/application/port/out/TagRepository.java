package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.Tag;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public interface TagRepository {
  Tag save(@NonNull Tag tag);

  void delete(@NonNull String tagId);

  Optional<Tag> findById(@NonNull String tagId);

  Optional<Tag> findByNameAndUserId(@NonNull String name, @NonNull String userId);

  List<Tag> findByUserId(@NonNull String userId);

  List<Tag> findTagsForLink(@NonNull String linkId);

  /** Tags on a link that belong to the given user (filtered in the query, not in memory). */
  List<Tag> findTagsForLink(@NonNull String linkId, @NonNull String userId);

  /** All tags attached to any of the given links, in a single query (avoids N+1). */
  List<Tag> findTagsForLinks(@NonNull List<String> linkIds);

  void addTagToLink(@NonNull String linkId, @NonNull String tagId);

  void removeTagFromLink(@NonNull String linkId, @NonNull String tagId);

  List<String> findLinkIdsByTagId(@NonNull String tagId);
}
