package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.Link;
import org.jspecify.annotations.NonNull;

public interface UpdateLinkUseCase {
  @NonNull
  Link updateLink(@NonNull UpdateLinkCommand command);
}
