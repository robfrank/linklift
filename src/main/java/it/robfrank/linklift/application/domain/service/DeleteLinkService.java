package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.port.in.DeleteLinkUseCase;
import it.robfrank.linklift.application.port.out.DeleteLinkPort;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import org.jspecify.annotations.NonNull;

public class DeleteLinkService implements DeleteLinkUseCase {

  private final LoadLinksPort loadLinksPort;
  private final DeleteLinkPort deleteLinkPort;

  public DeleteLinkService(LoadLinksPort loadLinksPort, DeleteLinkPort deleteLinkPort) {
    this.loadLinksPort = loadLinksPort;
    this.deleteLinkPort = deleteLinkPort;
  }

  @Override
  public void deleteLink(@NonNull String id, @NonNull String userId) {
    // Check existence and ownership
    if (!loadLinksPort.userOwnsLink(userId, id)) {
      throw new LinkNotFoundException("Link not found or not owned by user");
    }

    deleteLinkPort.deleteLink(id);
  }
}
