package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import it.robfrank.linklift.adapter.in.web.security.SecurityContext;
import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.model.Tag;
import it.robfrank.linklift.application.port.in.AddTagToLinkCommand;
import it.robfrank.linklift.application.port.in.AddTagToLinkUseCase;
import it.robfrank.linklift.application.port.in.CreateTagCommand;
import it.robfrank.linklift.application.port.in.CreateTagUseCase;
import it.robfrank.linklift.application.port.in.DeleteTagUseCase;
import it.robfrank.linklift.application.port.in.GetTagsForLinkUseCase;
import it.robfrank.linklift.application.port.in.ListTagsUseCase;
import it.robfrank.linklift.application.port.in.RemoveTagFromLinkUseCase;
import it.robfrank.linklift.application.port.in.SuggestTagsUseCase;
import java.util.List;
import java.util.Objects;

public class TagController {

  private final CreateTagUseCase createTagUseCase;
  private final DeleteTagUseCase deleteTagUseCase;
  private final ListTagsUseCase listTagsUseCase;
  private final GetTagsForLinkUseCase getTagsForLinkUseCase;
  private final AddTagToLinkUseCase addTagToLinkUseCase;
  private final RemoveTagFromLinkUseCase removeTagFromLinkUseCase;
  private final SuggestTagsUseCase suggestTagsUseCase;

  public TagController(
    CreateTagUseCase createTagUseCase,
    DeleteTagUseCase deleteTagUseCase,
    ListTagsUseCase listTagsUseCase,
    GetTagsForLinkUseCase getTagsForLinkUseCase,
    AddTagToLinkUseCase addTagToLinkUseCase,
    RemoveTagFromLinkUseCase removeTagFromLinkUseCase,
    SuggestTagsUseCase suggestTagsUseCase
  ) {
    this.createTagUseCase = createTagUseCase;
    this.deleteTagUseCase = deleteTagUseCase;
    this.listTagsUseCase = listTagsUseCase;
    this.getTagsForLinkUseCase = getTagsForLinkUseCase;
    this.addTagToLinkUseCase = addTagToLinkUseCase;
    this.removeTagFromLinkUseCase = removeTagFromLinkUseCase;
    this.suggestTagsUseCase = suggestTagsUseCase;
  }

  /** GET /api/v1/tags - list all tags for the current user */
  public void listTags(Context ctx) {
    String userId = requireUserId(ctx);
    List<Tag> tags = listTagsUseCase.listTags(userId);
    ctx.json(tags);
  }

  /** POST /api/v1/tags - create a new tag */
  public void createTag(Context ctx) {
    String userId = requireUserId(ctx);
    CreateTagRequest request = ctx.bodyAsClass(CreateTagRequest.class);
    CreateTagCommand command = new CreateTagCommand(request.name(), userId);
    Tag tag = createTagUseCase.createTag(command);
    ctx.status(201).json(tag);
  }

  /** DELETE /api/v1/tags/:tagId - delete a tag */
  public void deleteTag(Context ctx) {
    String tagId = Objects.requireNonNull(ctx.pathParam("tagId"));
    String userId = requireUserId(ctx);
    deleteTagUseCase.deleteTag(tagId, userId);
    ctx.status(204);
  }

  /** GET /api/v1/links/:linkId/tags - get tags for a specific link */
  public void getTagsForLink(Context ctx) {
    String linkId = Objects.requireNonNull(ctx.pathParam("linkId"));
    String userId = requireUserId(ctx);
    List<Tag> tags = getTagsForLinkUseCase.getTagsForLink(linkId, userId);
    ctx.json(tags);
  }

  /** POST /api/v1/links/:linkId/tags - add a tag to a link */
  public void addTagToLink(Context ctx) {
    String linkId = Objects.requireNonNull(ctx.pathParam("linkId"));
    String userId = requireUserId(ctx);
    AddTagToLinkRequest request = ctx.bodyAsClass(AddTagToLinkRequest.class);
    AddTagToLinkCommand command = new AddTagToLinkCommand(linkId, request.tagId(), userId);
    addTagToLinkUseCase.addTagToLink(command);
    ctx.status(204);
  }

  /** DELETE /api/v1/links/:linkId/tags/:tagId - remove a tag from a link */
  public void removeTagFromLink(Context ctx) {
    String linkId = Objects.requireNonNull(ctx.pathParam("linkId"));
    String tagId = Objects.requireNonNull(ctx.pathParam("tagId"));
    String userId = requireUserId(ctx);
    removeTagFromLinkUseCase.removeTagFromLink(linkId, tagId, userId);
    ctx.status(204);
  }

  /** GET /api/v1/links/:linkId/tags/suggest - suggest tags for a link */
  public void suggestTags(Context ctx) {
    String linkId = Objects.requireNonNull(ctx.pathParam("linkId"));
    String userId = requireUserId(ctx);
    List<Tag> suggestions = suggestTagsUseCase.suggestTags(linkId, userId);
    ctx.json(suggestions);
  }

  private String requireUserId(Context ctx) {
    String userId = SecurityContext.getCurrentUserId(ctx);
    if (userId == null) {
      throw AuthenticationException.unauthorizedAccess();
    }
    return userId;
  }

  public record CreateTagRequest(String name) {}

  public record AddTagToLinkRequest(String tagId) {}
}
