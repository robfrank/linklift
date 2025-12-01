package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import it.robfrank.linklift.adapter.in.web.error.ErrorResponse;
import it.robfrank.linklift.adapter.in.web.security.SecurityContext;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.domain.model.CollectionWithLinks;
import it.robfrank.linklift.application.port.in.*;
import java.util.List;
import org.jspecify.annotations.NonNull;

public class CollectionController {

  private final CreateCollectionUseCase createCollectionUseCase;
  private final ListCollectionsUseCase listCollectionsUseCase;
  private final GetCollectionUseCase getCollectionUseCase;
  private final AddLinkToCollectionUseCase addLinkToCollectionUseCase;
  private final RemoveLinkFromCollectionUseCase removeLinkFromCollectionUseCase;
  private final DeleteCollectionUseCase deleteCollectionUseCase;

  public CollectionController(
    CreateCollectionUseCase createCollectionUseCase,
    ListCollectionsUseCase listCollectionsUseCase,
    GetCollectionUseCase getCollectionUseCase,
    AddLinkToCollectionUseCase addLinkToCollectionUseCase,
    RemoveLinkFromCollectionUseCase removeLinkFromCollectionUseCase,
    DeleteCollectionUseCase deleteCollectionUseCase
  ) {
    this.createCollectionUseCase = createCollectionUseCase;
    this.listCollectionsUseCase = listCollectionsUseCase;
    this.getCollectionUseCase = getCollectionUseCase;
    this.addLinkToCollectionUseCase = addLinkToCollectionUseCase;
    this.removeLinkFromCollectionUseCase = removeLinkFromCollectionUseCase;
    this.deleteCollectionUseCase = deleteCollectionUseCase;
  }

  public void createCollection(@NonNull Context ctx) {
    var request = ctx.bodyAsClass(CreateCollectionRequest.class);
    var userId = SecurityContext.getCurrentUserId(ctx);

    if (userId == null) {
      ctx.status(401).json(ErrorResponse.builder().status(401).message("Unauthorized").build());
      return;
    }

    var command = new CreateCollectionCommand(request.name(), request.description(), userId.toString(), request.query());

    var collection = createCollectionUseCase.createCollection(command);
    ctx.status(201).json(new CollectionResponse(collection));
  }

  public void listCollections(@NonNull Context ctx) {
    var userId = SecurityContext.getCurrentUserId(ctx);

    if (userId == null) {
      ctx.status(401).json(ErrorResponse.builder().status(401).message("Unauthorized").build());
      return;
    }

    List<Collection> collections = listCollectionsUseCase.listCollections(userId.toString());
    ctx.status(200).json(new CollectionsResponse(collections));
  }

  public void getCollection(@NonNull Context ctx) {
    var userId = SecurityContext.getCurrentUserId(ctx);
    var collectionId = ctx.pathParam("id");

    if (userId == null) {
      ctx.status(401).json(ErrorResponse.builder().status(401).message("Unauthorized").build());
      return;
    }

    CollectionWithLinks collectionWithLinks = getCollectionUseCase.getCollection(collectionId, userId.toString());
    ctx.status(200).json(new CollectionWithLinksResponse(collectionWithLinks));
  }

  public void addLinkToCollection(@NonNull Context ctx) {
    var userId = SecurityContext.getCurrentUserId(ctx);
    var collectionId = ctx.pathParam("id");
    var request = ctx.bodyAsClass(AddLinkRequest.class);

    if (userId == null) {
      ctx.status(401).json(ErrorResponse.builder().status(401).message("Unauthorized").build());
      return;
    }

    var command = new AddLinkToCollectionCommand(collectionId, request.linkId(), userId.toString());
    addLinkToCollectionUseCase.addLinkToCollection(command);
    ctx.status(204);
  }

  public void removeLinkFromCollection(@NonNull Context ctx) {
    var userId = SecurityContext.getCurrentUserId(ctx);
    var collectionId = ctx.pathParam("id");
    var linkId = ctx.pathParam("linkId");

    if (userId == null) {
      ctx.status(401).json(ErrorResponse.builder().status(401).message("Unauthorized").build());
      return;
    }

    var command = new RemoveLinkFromCollectionCommand(collectionId, linkId, userId.toString());
    removeLinkFromCollectionUseCase.removeLinkFromCollection(command);
    ctx.status(204);
  }

  public void deleteCollection(@NonNull Context ctx) {
    var userId = SecurityContext.getCurrentUserId(ctx);
    var collectionId = ctx.pathParam("id");

    if (userId == null) {
      ctx.status(401).json(ErrorResponse.builder().status(401).message("Unauthorized").build());
      return;
    }

    deleteCollectionUseCase.deleteCollection(collectionId, userId.toString());
    ctx.status(204);
  }

  public record CreateCollectionRequest(String name, String description, String query) {}

  public record CollectionResponse(Collection data) {}

  public record CollectionsResponse(List<Collection> data) {}

  public record CollectionWithLinksResponse(CollectionWithLinks data) {}

  public record AddLinkRequest(String linkId) {}
}
