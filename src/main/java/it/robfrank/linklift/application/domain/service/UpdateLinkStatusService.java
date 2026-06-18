package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.UpdateLinkStatusCommand;
import it.robfrank.linklift.application.port.in.UpdateLinkStatusUseCase;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import it.robfrank.linklift.application.port.out.UpdateLinkPort;
import org.jspecify.annotations.NonNull;

public class UpdateLinkStatusService implements UpdateLinkStatusUseCase {

  private final LoadLinksPort loadLinksPort;
  private final UpdateLinkPort updateLinkPort;

  public UpdateLinkStatusService(LoadLinksPort loadLinksPort, UpdateLinkPort updateLinkPort) {
    this.loadLinksPort = loadLinksPort;
    this.updateLinkPort = updateLinkPort;
  }

  @Override
  public @NonNull Link updateLinkStatus(@NonNull UpdateLinkStatusCommand command) {
    ValidationUtils.requireNotEmpty(command.id(), "id");
    ValidationUtils.requireNotEmpty(command.userId(), "userId");

    // Single query that combines existence + ownership.
    Link existingLink = loadLinksPort
      .findLinkByIdAndUserId(command.id(), command.userId())
      .orElseThrow(() -> new LinkNotFoundException("Link not found or not owned by user"));

    Link updatedLink = new Link(
      existingLink.id(),
      existingLink.url(),
      existingLink.title(),
      existingLink.description(),
      existingLink.extractedAt(),
      existingLink.contentType(),
      existingLink.extractedUrls(),
      command.readStatus() != null ? command.readStatus() : existingLink.readStatus(),
      command.archived() != null ? command.archived() : existingLink.archived(),
      command.favorited() != null ? command.favorited() : existingLink.favorited()
    );

    return updateLinkPort.updateLink(updatedLink);
  }
}
