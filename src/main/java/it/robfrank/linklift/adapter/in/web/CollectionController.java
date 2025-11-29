package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import it.robfrank.linklift.adapter.in.web.error.ErrorResponse;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.port.in.CreateCollectionCommand;
import it.robfrank.linklift.application.port.in.CreateCollectionUseCase;
import org.jspecify.annotations.NonNull;

public class CollectionController {

  private final CreateCollectionUseCase createCollectionUseCase;

  public CollectionController(CreateCollectionUseCase createCollectionUseCase) {
    this.createCollectionUseCase = createCollectionUseCase;
  }

  public void createCollection(@NonNull Context ctx) {
    var request = ctx.bodyAsClass(CreateCollectionRequest.class);
    var userId = ctx.attribute("userId");

    if (userId == null) {
      ctx.status(401).json(ErrorResponse.builder().status(401).message("Unauthorized").build());
      return;
    }

    var command = new CreateCollectionCommand(request.name(), request.description(), userId.toString(), request.query());

    var collection = createCollectionUseCase.createCollection(command);
    ctx.status(201).json(new CollectionResponse(collection));
  }

  public record CreateCollectionRequest(String name, String description, String query) {}

  public record CollectionResponse(Collection data) {}
}
