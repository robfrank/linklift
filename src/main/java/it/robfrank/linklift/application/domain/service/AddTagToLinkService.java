package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.exception.TagNotFoundException;
import it.robfrank.linklift.application.port.in.AddTagToLinkCommand;
import it.robfrank.linklift.application.port.in.AddTagToLinkUseCase;
import it.robfrank.linklift.application.port.out.TagRepository;
import org.jspecify.annotations.NonNull;

public class AddTagToLinkService implements AddTagToLinkUseCase {

  private final TagRepository tagRepository;

  public AddTagToLinkService(TagRepository tagRepository) {
    this.tagRepository = tagRepository;
  }

  @Override
  public void addTagToLink(@NonNull AddTagToLinkCommand command) {
    var tag = tagRepository.findById(command.tagId()).orElseThrow(() -> new TagNotFoundException(command.tagId()));

    if (!tag.userId().equals(command.userId())) {
      throw AuthenticationException.unauthorizedAccess();
    }

    tagRepository.addTagToLink(command.linkId(), command.tagId());
  }
}
