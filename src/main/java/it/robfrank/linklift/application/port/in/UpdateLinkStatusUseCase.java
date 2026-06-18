package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.Link;
import org.jspecify.annotations.NonNull;

public interface UpdateLinkStatusUseCase {
  @NonNull
  Link updateLinkStatus(@NonNull UpdateLinkStatusCommand command);
}
