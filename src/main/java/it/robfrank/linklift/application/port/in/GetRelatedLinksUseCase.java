package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.Link;
import java.util.List;
import org.jspecify.annotations.NonNull;

public interface GetRelatedLinksUseCase {
  @NonNull
  List<Link> getRelatedLinks(@NonNull String linkId, @NonNull String userId);
}
