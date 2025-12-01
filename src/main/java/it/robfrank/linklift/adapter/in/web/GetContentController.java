package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import it.robfrank.linklift.application.domain.exception.ContentNotFoundException;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.port.in.DownloadContentUseCase;
import it.robfrank.linklift.application.port.in.GetContentQuery;
import it.robfrank.linklift.application.port.in.GetContentUseCase;
import org.jspecify.annotations.NonNull;

public class GetContentController {

  private final GetContentUseCase getContentUseCase;
  private final DownloadContentUseCase downloadContentUseCase;

  public GetContentController(@NonNull GetContentUseCase getContentUseCase, @NonNull DownloadContentUseCase downloadContentUseCase) {
    this.getContentUseCase = getContentUseCase;
    this.downloadContentUseCase = downloadContentUseCase;
  }

  public void getContent(@NonNull Context ctx) {
    String linkId = ctx.pathParam("linkId");

    Content content = getContentUseCase.getContent(new GetContentQuery(linkId)).orElseThrow(() -> new ContentNotFoundException(linkId));

    ctx.json(new ContentResponse(content, "Content retrieved successfully"));
  }

  public void refreshContent(@NonNull Context ctx) {
    String linkId = ctx.pathParam("linkId");
    downloadContentUseCase.refreshContent(linkId);
    ctx.status(202).json(new MessageResponse("Content refresh triggered"));
  }

  public record ContentResponse(@NonNull Content data, @NonNull String message) {}

  public record MessageResponse(@NonNull String message) {}
}
