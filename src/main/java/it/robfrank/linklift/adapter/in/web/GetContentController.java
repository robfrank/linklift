package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import it.robfrank.linklift.application.domain.exception.ContentNotFoundException;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.port.in.GetContentQuery;
import it.robfrank.linklift.application.port.in.GetContentUseCase;
import org.jspecify.annotations.NonNull;

public class GetContentController {

  private final GetContentUseCase getContentUseCase;

  public GetContentController(@NonNull GetContentUseCase getContentUseCase) {
    this.getContentUseCase = getContentUseCase;
  }

  public void getContent(@NonNull Context ctx) {
    String linkId = ctx.pathParam("linkId");

    Content content = getContentUseCase.getContent(new GetContentQuery(linkId)).orElseThrow(() -> new ContentNotFoundException(linkId));

    ctx.json(new ContentResponse(content, "Content retrieved successfully"));
  }

  public record ContentResponse(@NonNull Content data, @NonNull String message) {}
}
