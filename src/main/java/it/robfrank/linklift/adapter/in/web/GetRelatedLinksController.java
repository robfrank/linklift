package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import it.robfrank.linklift.adapter.in.web.error.ErrorResponse;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.in.GetRelatedLinksUseCase;
import java.util.List;
import org.jspecify.annotations.NonNull;

public class GetRelatedLinksController {

  private final GetRelatedLinksUseCase getRelatedLinksUseCase;

  public GetRelatedLinksController(GetRelatedLinksUseCase getRelatedLinksUseCase) {
    this.getRelatedLinksUseCase = getRelatedLinksUseCase;
  }

  public void getRelatedLinks(@NonNull Context ctx) {
    var linkId = ctx.pathParam("linkId");
    var userId = it.robfrank.linklift.adapter.in.web.security.SecurityContext.getCurrentUserId(ctx);

    if (userId == null) {
      ctx.status(401).json(ErrorResponse.builder().status(401).message("Unauthorized").build());
      return;
    }

    var links = getRelatedLinksUseCase.getRelatedLinks(linkId, userId.toString());
    ctx.json(new RelatedLinksResponse(links));
  }

  public record RelatedLinksResponse(List<Link> data) {}
}
