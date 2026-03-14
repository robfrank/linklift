package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import it.robfrank.linklift.adapter.in.web.security.SecurityContext;
import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.ReadStatus;
import it.robfrank.linklift.application.port.in.DeleteLinkUseCase;
import it.robfrank.linklift.application.port.in.UpdateLinkCommand;
import it.robfrank.linklift.application.port.in.UpdateLinkStatusCommand;
import it.robfrank.linklift.application.port.in.UpdateLinkStatusUseCase;
import it.robfrank.linklift.application.port.in.UpdateLinkUseCase;
import java.util.Objects;

public class LinkController {

  private final UpdateLinkUseCase updateLinkUseCase;
  private final DeleteLinkUseCase deleteLinkUseCase;
  private final UpdateLinkStatusUseCase updateLinkStatusUseCase;

  public LinkController(UpdateLinkUseCase updateLinkUseCase, DeleteLinkUseCase deleteLinkUseCase, UpdateLinkStatusUseCase updateLinkStatusUseCase) {
    this.updateLinkUseCase = updateLinkUseCase;
    this.deleteLinkUseCase = deleteLinkUseCase;
    this.updateLinkStatusUseCase = updateLinkStatusUseCase;
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

  public void updateLinkStatus(Context ctx) {
    String id = Objects.requireNonNull(ctx.pathParam("id"));
    String currentUserId = SecurityContext.getCurrentUserId(ctx);

    if (currentUserId == null) {
      throw AuthenticationException.unauthorizedAccess();
    }

    UpdateLinkStatusRequest request = ctx.bodyAsClass(UpdateLinkStatusRequest.class);

    ReadStatus readStatus = null;
    if (request.readStatus() != null) {
      readStatus = ReadStatus.valueOf(request.readStatus().toUpperCase());
    }

    UpdateLinkStatusCommand command = new UpdateLinkStatusCommand(id, readStatus, request.archived(), request.favorited(), currentUserId);

    Link updatedLink = updateLinkStatusUseCase.updateLinkStatus(command);

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

  public record UpdateLinkStatusRequest(String readStatus, Boolean archived, Boolean favorited) {}

  public record UpdateLinkResponse(Link data) {}
}
