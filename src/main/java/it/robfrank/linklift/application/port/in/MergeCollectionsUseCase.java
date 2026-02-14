package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

public interface MergeCollectionsUseCase {
  void mergeCollections(@NonNull MergeCollectionsCommand command);
}
