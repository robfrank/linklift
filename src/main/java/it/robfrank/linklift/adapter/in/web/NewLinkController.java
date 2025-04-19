package it.robfrank.linklift.adapter.in.web;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import io.javalin.http.Context;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.in.NewLinkCommand;
import it.robfrank.linklift.application.port.in.NewLinkUseCase;

public class NewLinkController {

  private final NewLinkUseCase newLinkUseCase;

  public NewLinkController(NewLinkUseCase newLinkUseCase) {
    this.newLinkUseCase = newLinkUseCase;
  }

  public void processLink(Context ctx) {
    NewLinkCommand linkCommand = ctx
      .bodyValidator(NewLinkCommand.class)
      .check(command -> isNotEmpty(command.url()), "Url cannot be empty")
      .check(command -> isNotEmpty(command.title()), "Title cannot be empty")
      .check(command -> isNotEmpty(command.description()), "Description cannot be empty")
      .get();

    Link saved = newLinkUseCase.newLink(linkCommand);

    ctx.status(201).json(new LinkResponse(saved, "Link received"));
  }

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
