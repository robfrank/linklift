package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.Content;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public interface LoadContentPort {
  @NonNull
  Optional<Content> findContentByLinkId(@NonNull String linkId);

  @NonNull
  Optional<Content> findContentById(@NonNull String contentId);

  /**
   * Finds content most similar to the query vector, restricted to links owned by the
   * given user. Cross-user content is never returned.
   */
  @NonNull
  List<Content> findSimilar(@NonNull List<Float> queryVector, int limit, @NonNull String userId);

  @NonNull
  List<Content> findContentsWithoutEmbeddings(int limit);
}
