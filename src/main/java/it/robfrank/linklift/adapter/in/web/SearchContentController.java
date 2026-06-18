package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import it.robfrank.linklift.adapter.in.web.security.SecurityContext;
import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.port.in.SearchContentUseCase;
import java.util.List;
import org.jspecify.annotations.NonNull;

public class SearchContentController {

  private final SearchContentUseCase searchContentUseCase;

  public SearchContentController(@NonNull SearchContentUseCase searchContentUseCase) {
    this.searchContentUseCase = searchContentUseCase;
  }

  public void search(@NonNull Context ctx) {
    String query = ctx.queryParam("q");
    if (query == null || query.isBlank()) {
      ctx.status(HttpStatus.BAD_REQUEST);
      ctx.result("Search query cannot be empty");
      return;
    }

    String userId = SecurityContext.getCurrentUserId(ctx);
    if (userId == null) {
      throw AuthenticationException.unauthorizedAccess();
    }

    int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(10);

    List<Content> results = searchContentUseCase.search(query, limit, userId);
    ctx.json(results);
  }
}
