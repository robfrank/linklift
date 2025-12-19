package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.Content;
import java.util.List;
import org.jspecify.annotations.NonNull;

public interface SearchContentUseCase {
  @NonNull
  List<Content> search(@NonNull String query, int limit);
}
