package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.Content;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public interface GetContentUseCase {
  @NonNull
  Optional<Content> getContent(@NonNull GetContentQuery query);
}
