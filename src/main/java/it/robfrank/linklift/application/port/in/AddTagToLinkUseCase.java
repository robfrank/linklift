package it.robfrank.linklift.application.port.in;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface AddTagToLinkUseCase {
  void addTagToLink(@NonNull AddTagToLinkCommand command);
}
