package it.robfrank.linklift.adapter.in.web;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import io.javalin.http.Context;
import it.robfrank.linklift.adapter.in.web.security.SecurityContext;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.in.NewLinkCommand;
import it.robfrank.linklift.application.port.in.NewLinkUseCase;

public class NewLinkController {

  private final NewLinkUseCase newLinkUseCase;

  public NewLinkController(NewLinkUseCase newLinkUseCase) {
    this.newLinkUseCase = newLinkUseCase;
  }

  public void processLink(Context ctx) {
    // Get current user from security context
    String currentUserId = SecurityContext.getCurrentUserId(ctx);

    // Parse and validate the request body
    var requestBody = ctx.bodyValidator(LinkRequest.class).check(request -> isNotEmpty(request.url()), "Url cannot be empty").get();

    // Create command with user context
    NewLinkCommand linkCommand = new NewLinkCommand(requestBody.url(), requestBody.title(), requestBody.description(), currentUserId);

    Link saved = newLinkUseCase.newLink(linkCommand);

    ctx.status(201).json(new LinkResponse(saved, "Link received"));
  }

  public record LinkRequest(String url, String title, String description) {}

  public record LinkResponse(Link link, String status) {}

  public static class Builder {

    private NewLinkUseCase newLinkUseCase;

    public Builder withNewLinkUseCase(NewLinkUseCase newLinkUseCase) {
      this.newLinkUseCase = newLinkUseCase;
      return this;
    }

    public NewLinkController build() {
      return new NewLinkController(newLinkUseCase);
    }
  }
}
