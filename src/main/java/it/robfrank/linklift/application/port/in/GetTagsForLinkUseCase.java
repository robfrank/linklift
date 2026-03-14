package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.Tag;
import java.util.List;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface GetTagsForLinkUseCase {
  List<Tag> getTagsForLink(@NonNull String linkId);
}
