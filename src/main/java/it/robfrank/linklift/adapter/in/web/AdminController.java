package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import it.robfrank.linklift.application.port.in.BackfillEmbeddingsUseCase;
import org.eclipse.jdt.annotation.NonNull;

public class AdminController {

  private final BackfillEmbeddingsUseCase backfillEmbeddingsUseCase;

  public AdminController(BackfillEmbeddingsUseCase backfillEmbeddingsUseCase) {
    this.backfillEmbeddingsUseCase = backfillEmbeddingsUseCase;
  }

  public void backfillEmbeddings(Context ctx) {
    // Run in a separate thread or just execute synchronously if it's not too many
    // For now, simple synchronous call (or we could use a dedicated executor)
    new Thread(backfillEmbeddingsUseCase::backfill).start();
    ctx.status(HttpStatus.ACCEPTED).result("Backfill process started");
  }
}
