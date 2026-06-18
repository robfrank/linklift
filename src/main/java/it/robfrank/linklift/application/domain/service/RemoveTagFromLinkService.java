package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.domain.exception.TagNotFoundException;
import it.robfrank.linklift.application.port.in.RemoveTagFromLinkUseCase;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import it.robfrank.linklift.application.port.out.TagRepository;
import org.jspecify.annotations.NonNull;

public class RemoveTagFromLinkService implements RemoveTagFromLinkUseCase {

  private final TagRepository tagRepository;
  private final LoadLinksPort loadLinksPort;

  public RemoveTagFromLinkService(TagRepository tagRepository, LoadLinksPort loadLinksPort) {
    this.tagRepository = tagRepository;
    this.loadLinksPort = loadLinksPort;
  }

  @Override
  public void removeTagFromLink(@NonNull String linkId, @NonNull String tagId, @NonNull String userId) {
    // The caller must own both the link and the tag.
    if (!loadLinksPort.userOwnsLink(userId, linkId)) {
      throw new LinkNotFoundException("Link not found or not owned by user");
    }

    var tag = tagRepository.findById(tagId).orElseThrow(() -> new TagNotFoundException(tagId));

    if (!tag.userId().equals(userId)) {
      throw AuthenticationException.unauthorizedAccess();
    }

    tagRepository.removeTagFromLink(linkId, tagId);
  }
}
