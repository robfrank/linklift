package it.robfrank.linklift.adapter.in.web;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import io.javalin.http.Context;
import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.in.NewLinkCommand;
import it.robfrank.linklift.application.port.in.NewLinkUseCase;
import java.util.HashMap;
import java.util.Map;

public class NewLinkController {

  private final NewLinkUseCase newLinkUseCase;

  public NewLinkController(NewLinkUseCase newLinkUseCase) {
    this.newLinkUseCase = newLinkUseCase;
  }

  public void processLink(Context ctx) {
    // Use our own validation instead of Javalin's
    NewLinkCommand command = ctx.bodyAsClass(NewLinkCommand.class);
    validateNewLinkCommand(command);

    Link saved = newLinkUseCase.newLink(command);

    ctx.status(201).json(new LinkResponse(saved, "Link received"));
  }

  private void validateNewLinkCommand(NewLinkCommand command) {
    Map<String, String> errors = new HashMap<>();

    if (command.url() == null || command.url().isBlank()) {
      errors.put("url", "URL cannot be empty");
    }

    if (command.title() == null || command.title().isBlank()) {
      errors.put("title", "Title cannot be empty");
    }

    if (command.description() == null || command.description().isBlank()) {
      errors.put("description", "Description cannot be empty");
    }

    if (!errors.isEmpty()) {
      throw new ValidationException("Invalid link data", errors);
    }
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
