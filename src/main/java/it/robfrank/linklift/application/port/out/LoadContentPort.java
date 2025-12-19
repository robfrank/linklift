package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.Content;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public interface LoadContentPort {
  @NonNull
  Optional<Content> findContentByLinkId(@NonNull String linkId);

  @NonNull
  Optional<Content> findContentById(@NonNull String contentId);

  @NonNull
  java.util.List<Content> findSimilar(@NonNull java.util.List<Float> queryVector, int limit);

  @NonNull
  java.util.List<Content> findContentsWithoutEmbeddings(int limit);
}
