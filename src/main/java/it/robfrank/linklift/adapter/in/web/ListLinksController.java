package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import it.robfrank.linklift.application.domain.model.LinkPage;
import it.robfrank.linklift.application.port.in.ListLinksQuery;
import it.robfrank.linklift.application.port.in.ListLinksUseCase;

public class ListLinksController {

  private final ListLinksUseCase listLinksUseCase;

  public ListLinksController(ListLinksUseCase listLinksUseCase) {
    this.listLinksUseCase = listLinksUseCase;
  }

  public void listLinks(Context ctx) {
    // Extract query parameters
    Integer page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(null);
    Integer size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(null);
    String sortBy = ctx.queryParam("sortBy");
    String sortDirection = ctx.queryParam("sortDirection");

    ListLinksQuery query = ListLinksQuery.of(page, size, sortBy, sortDirection);
    LinkPage result = listLinksUseCase.listLinks(query);

    ctx.status(200).json(new LinkPageResponse(result, "Links retrieved successfully"));
  }

  public record LinkPageResponse(LinkPage data, String message) {}

  public static class Builder {

    private ListLinksUseCase listLinksUseCase;

    public Builder withListLinksUseCase(ListLinksUseCase listLinksUseCase) {
      this.listLinksUseCase = listLinksUseCase;
      return this;
    }

    public ListLinksController build() {
      return new ListLinksController(listLinksUseCase);
    }
  }
}
