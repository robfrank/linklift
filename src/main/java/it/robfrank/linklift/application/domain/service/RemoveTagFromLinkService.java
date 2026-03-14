package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.exception.TagNotFoundException;
import it.robfrank.linklift.application.port.in.RemoveTagFromLinkUseCase;
import it.robfrank.linklift.application.port.out.TagRepository;
import org.jspecify.annotations.NonNull;

public class RemoveTagFromLinkService implements RemoveTagFromLinkUseCase {

  private final TagRepository tagRepository;

  public RemoveTagFromLinkService(TagRepository tagRepository) {
    this.tagRepository = tagRepository;
  }

  @Override
  public void removeTagFromLink(@NonNull String linkId, @NonNull String tagId, @NonNull String userId) {
    var tag = tagRepository.findById(tagId).orElseThrow(() -> new TagNotFoundException(tagId));

    if (!tag.userId().equals(userId)) {
      throw AuthenticationException.unauthorizedAccess();
    }

    tagRepository.removeTagFromLink(linkId, tagId);
  }
}
