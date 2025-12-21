package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import it.robfrank.linklift.adapter.in.web.security.SecurityContext;
import it.robfrank.linklift.application.domain.model.GraphData;
import it.robfrank.linklift.application.domain.model.LinkPage;
import it.robfrank.linklift.application.port.in.GetGraphUseCase;
import it.robfrank.linklift.application.port.in.ListLinksQuery;
import it.robfrank.linklift.application.port.in.ListLinksUseCase;

public class ListLinksController {

  private final ListLinksUseCase listLinksUseCase;
  private final GetGraphUseCase getGraphUseCase;

  public ListLinksController(ListLinksUseCase listLinksUseCase, GetGraphUseCase getGraphUseCase) {
    this.listLinksUseCase = listLinksUseCase;
    this.getGraphUseCase = getGraphUseCase;
  }

  public void listLinks(Context ctx) {
    // Get current user from security context
    String currentUserId = SecurityContext.getCurrentUserId(ctx);

    // Extract query parameters
    Integer page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(null);
    Integer size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(null);
    String sortBy = ctx.queryParam("sortBy");
    String sortDirection = ctx.queryParam("sortDirection");

    // Create query with user context for authorization
    ListLinksQuery query = ListLinksQuery.forUser(page, size, sortBy, sortDirection, currentUserId);
    LinkPage result = listLinksUseCase.listLinks(query);

    ctx.status(200).json(new LinkPageResponse(result, "Links retrieved successfully"));
  }

  public void getGraph(Context ctx) {
    String currentUserId = SecurityContext.getCurrentUserId(ctx);
    GraphData graphData = getGraphUseCase.getGraphData(currentUserId);
    ctx.status(200).json(new GraphResponse(graphData, "Graph data retrieved successfully"));
  }

  public record LinkPageResponse(LinkPage data, String message) {}

  public record GraphResponse(GraphData data, String message) {}

  public static class Builder {

    private ListLinksUseCase listLinksUseCase;
    private GetGraphUseCase getGraphUseCase;

    public Builder withListLinksUseCase(ListLinksUseCase listLinksUseCase) {
      this.listLinksUseCase = listLinksUseCase;
      return this;
    }

    public Builder withGetGraphUseCase(GetGraphUseCase getGraphUseCase) {
      this.getGraphUseCase = getGraphUseCase;
      return this;
    }

    public ListLinksController build() {
      return new ListLinksController(listLinksUseCase, getGraphUseCase);
    }
  }
}
