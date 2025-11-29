package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.Collection;
import org.jspecify.annotations.NonNull;

public interface CreateCollectionUseCase {
  @NonNull
  Collection createCollection(@NonNull CreateCollectionCommand command);
}
