package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.UpdateLinkCommand;
import it.robfrank.linklift.application.port.in.UpdateLinkUseCase;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import it.robfrank.linklift.application.port.out.UpdateLinkPort;
import org.jspecify.annotations.NonNull;

public class UpdateLinkService implements UpdateLinkUseCase {

  private final LoadLinksPort loadLinksPort;
  private final UpdateLinkPort updateLinkPort;

  public UpdateLinkService(LoadLinksPort loadLinksPort, UpdateLinkPort updateLinkPort) {
    this.loadLinksPort = loadLinksPort;
    this.updateLinkPort = updateLinkPort;
  }

  @Override
  public @NonNull Link updateLink(@NonNull UpdateLinkCommand command) {
    ValidationUtils.requireNotEmpty(command.id(), "id");
    ValidationUtils.requireNotEmpty(command.userId(), "userId");

    // Check existence
    Link existingLink = loadLinksPort.getLinkById(command.id());

    // Check ownership
    if (!loadLinksPort.userOwnsLink(command.userId(), command.id())) {
      throw new LinkNotFoundException("Link not found or not owned by user");
    }

    // Update fields
    String newTitle = command.title() != null ? command.title() : existingLink.title();
    String newDescription = command.description() != null ? command.description() : existingLink.description();

    Link updatedLink = new Link(
      existingLink.id(),
      existingLink.url(),
      newTitle,
      newDescription,
      existingLink.extractedAt(),
      existingLink.contentType(),
      existingLink.extractedUrls()
    );

    return updateLinkPort.updateLink(updatedLink);
  }
}
