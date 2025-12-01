package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import it.robfrank.linklift.application.port.in.DeleteContentCommand;
import it.robfrank.linklift.application.port.in.DeleteContentUseCase;
import org.jspecify.annotations.NonNull;

public class DeleteContentController {

  private final DeleteContentUseCase deleteContentUseCase;

  public DeleteContentController(@NonNull DeleteContentUseCase deleteContentUseCase) {
    this.deleteContentUseCase = deleteContentUseCase;
  }

  public void deleteContent(@NonNull Context ctx) {
    String linkId = ctx.pathParam("linkId");
    DeleteContentCommand command = new DeleteContentCommand(linkId);
    deleteContentUseCase.deleteContent(command);
    ctx.status(204);
  }
}
