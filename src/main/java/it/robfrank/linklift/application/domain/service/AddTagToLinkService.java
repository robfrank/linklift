package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.domain.exception.TagNotFoundException;
import it.robfrank.linklift.application.port.in.AddTagToLinkCommand;
import it.robfrank.linklift.application.port.in.AddTagToLinkUseCase;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import it.robfrank.linklift.application.port.out.TagRepository;
import org.jspecify.annotations.NonNull;

public class AddTagToLinkService implements AddTagToLinkUseCase {

  private final TagRepository tagRepository;
  private final LoadLinksPort loadLinksPort;

  public AddTagToLinkService(TagRepository tagRepository, LoadLinksPort loadLinksPort) {
    this.tagRepository = tagRepository;
    this.loadLinksPort = loadLinksPort;
  }

  @Override
  public void addTagToLink(@NonNull AddTagToLinkCommand command) {
    // The caller must own both the link being tagged and the tag itself.
    if (!loadLinksPort.userOwnsLink(command.userId(), command.linkId())) {
      throw new LinkNotFoundException("Link not found or not owned by user");
    }

    var tag = tagRepository.findById(command.tagId()).orElseThrow(() -> new TagNotFoundException(command.tagId()));

    if (!tag.userId().equals(command.userId())) {
      throw AuthenticationException.unauthorizedAccess();
    }

    tagRepository.addTagToLink(command.linkId(), command.tagId());
  }
}
