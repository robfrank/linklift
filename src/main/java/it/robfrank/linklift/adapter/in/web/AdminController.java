package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import it.robfrank.linklift.application.port.in.BackfillEmbeddingsUseCase;
import java.util.concurrent.ExecutorService;
import org.jspecify.annotations.NonNull;

public class AdminController {

  private final BackfillEmbeddingsUseCase backfillEmbeddingsUseCase;
  private final ExecutorService executorService;

  public AdminController(@NonNull BackfillEmbeddingsUseCase backfillEmbeddingsUseCase, @NonNull ExecutorService executorService) {
    this.backfillEmbeddingsUseCase = backfillEmbeddingsUseCase;
    this.executorService = executorService;
  }

  public void backfillEmbeddings(Context ctx) {
    executorService.submit(backfillEmbeddingsUseCase::backfill);
    ctx.status(HttpStatus.ACCEPTED).result("Backfill process started");
  }
}
