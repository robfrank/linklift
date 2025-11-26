package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.Collection;
import org.jspecify.annotations.NonNull;

public interface CollectionRepository {
  @NonNull
  Collection save(@NonNull Collection collection);
}
