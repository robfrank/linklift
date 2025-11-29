package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.Content;
import org.jspecify.annotations.NonNull;

public interface SaveContentPort {
  @NonNull
  Content saveContent(@NonNull Content content);

  void createHasContentEdge(@NonNull String linkId, @NonNull String contentId);
}
