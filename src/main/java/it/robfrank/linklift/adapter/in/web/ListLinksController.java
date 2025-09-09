package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import it.robfrank.linklift.adapter.in.web.security.SecurityContext;
import it.robfrank.linklift.application.domain.model.LinkPage;
import it.robfrank.linklift.application.port.in.ListLinksQuery;
import it.robfrank.linklift.application.port.in.ListLinksUseCase;

public class ListLinksController {

    private final ListLinksUseCase listLinksUseCase;

    public ListLinksController(ListLinksUseCase listLinksUseCase) {
        this.listLinksUseCase = listLinksUseCase;
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
