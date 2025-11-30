package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.Link;
import org.jspecify.annotations.NonNull;

public interface UpdateLinkPort {
  @NonNull
  Link updateLink(@NonNull Link link);
}
