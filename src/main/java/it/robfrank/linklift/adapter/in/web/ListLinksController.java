package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import it.robfrank.linklift.adapter.in.web.error.ErrorResponse;
import it.robfrank.linklift.adapter.in.web.security.SecurityContext;
import it.robfrank.linklift.application.domain.model.GraphData;
import it.robfrank.linklift.application.domain.model.LinkPage;
import it.robfrank.linklift.application.domain.model.ReadStatus;
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
    Integer page = ctx.queryParamAsClass("page", Integer.class).getOrNull();
    Integer size = ctx.queryParamAsClass("size", Integer.class).getOrNull();
    String sortBy = ctx.queryParam("sortBy");
    String sortDirection = ctx.queryParam("sortDirection");

    // Extract status filter parameters
    String readStatusParam = ctx.queryParam("readStatus");
    String archivedParam = ctx.queryParam("archived");
    String favoritedParam = ctx.queryParam("favorited");
    String tagId = ctx.queryParam("tagId");

    ReadStatus readStatus = null;
    if (readStatusParam != null && !readStatusParam.isBlank()) {
      try {
        readStatus = ReadStatus.valueOf(readStatusParam.toUpperCase());
      } catch (IllegalArgumentException ignored) {
        // Invalid value - ignore filter
      }
    }
    Boolean archived = archivedParam != null ? Boolean.parseBoolean(archivedParam) : null;
    Boolean favorited = favoritedParam != null ? Boolean.parseBoolean(favoritedParam) : null;

    // Create query with user context and optional filters
    ListLinksQuery query = ListLinksQuery.forUserWithFiltersAndTag(page, size, sortBy, sortDirection, currentUserId, readStatus, archived, favorited, tagId);
    LinkPage result = listLinksUseCase.listLinks(query);

    ctx.status(200).json(new LinkPageResponse(result, "Links retrieved successfully"));
  }

  public void getGraph(Context ctx) {
    String currentUserId = SecurityContext.getCurrentUserId(ctx);

    if (currentUserId == null) {
      ctx.status(401).json(ErrorResponse.builder().status(401).message("Unauthorized").build());
      return;
    }

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
