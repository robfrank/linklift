package it.robfrank.linklift.application.port.out;

import org.jspecify.annotations.NonNull;

public interface DeleteLinkPort {
  void deleteLink(@NonNull String id);
}
