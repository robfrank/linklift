package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.Tag;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface CreateTagUseCase {
  Tag createTag(@NonNull CreateTagCommand command);
}
