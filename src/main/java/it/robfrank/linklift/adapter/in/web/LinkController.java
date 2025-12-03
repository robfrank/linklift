package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import it.robfrank.linklift.adapter.in.web.security.SecurityContext;
import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.in.DeleteLinkUseCase;
import it.robfrank.linklift.application.port.in.UpdateLinkCommand;
import it.robfrank.linklift.application.port.in.UpdateLinkUseCase;
import java.util.Objects;

public class LinkController {

  private final UpdateLinkUseCase updateLinkUseCase;
  private final DeleteLinkUseCase deleteLinkUseCase;

  public LinkController(UpdateLinkUseCase updateLinkUseCase, DeleteLinkUseCase deleteLinkUseCase) {
    this.updateLinkUseCase = updateLinkUseCase;
    this.deleteLinkUseCase = deleteLinkUseCase;
  }

  public void updateLink(Context ctx) {
    String id = Objects.requireNonNull(ctx.pathParam("id"));
    String currentUserId = SecurityContext.getCurrentUserId(ctx);

    if (currentUserId == null) {
      throw AuthenticationException.unauthorizedAccess();
    }

    UpdateLinkRequest request = ctx.bodyAsClass(UpdateLinkRequest.class);

    UpdateLinkCommand command = new UpdateLinkCommand(id, request.title(), request.description(), currentUserId);

    Link updatedLink = updateLinkUseCase.updateLink(command);

    ctx.json(new UpdateLinkResponse(updatedLink));
  }

  public void deleteLink(Context ctx) {
    String id = Objects.requireNonNull(ctx.pathParam("id"));
    String currentUserId = SecurityContext.getCurrentUserId(ctx);

    if (currentUserId == null) {
      throw AuthenticationException.unauthorizedAccess();
    }

    deleteLinkUseCase.deleteLink(id, currentUserId);

    ctx.status(204);
  }

  public record UpdateLinkRequest(String title, String description) {}

  public record UpdateLinkResponse(Link data) {}
}
