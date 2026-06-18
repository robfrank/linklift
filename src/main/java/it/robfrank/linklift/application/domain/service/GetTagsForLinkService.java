package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.Tag;
import it.robfrank.linklift.application.port.in.GetTagsForLinkUseCase;
import it.robfrank.linklift.application.port.out.TagRepository;
import java.util.List;
import org.jspecify.annotations.NonNull;

public class GetTagsForLinkService implements GetTagsForLinkUseCase {

  private final TagRepository tagRepository;

  public GetTagsForLinkService(TagRepository tagRepository) {
    this.tagRepository = tagRepository;
  }

  @Override
  public List<Tag> getTagsForLink(@NonNull String linkId, @NonNull String userId) {
    // Scope to the requesting user's own tags (filtered in the query) so one user can't enumerate another's tags on a link.
    return tagRepository.findTagsForLink(linkId, userId);
  }
}
