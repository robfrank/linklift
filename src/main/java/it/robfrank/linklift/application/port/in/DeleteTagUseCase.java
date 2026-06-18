package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface DeleteTagUseCase {
  void deleteTag(@NonNull String tagId, @NonNull String userId);
}
