package it.robfrank.linklift.adapter.in.web;

import io.javalin.http.Context;
import it.robfrank.linklift.adapter.in.web.security.SecurityContext;
import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.model.Note;
import it.robfrank.linklift.application.port.in.*;
import java.util.List;
import java.util.Objects;

public class NoteController {

  private final CreateNoteUseCase createNoteUseCase;
  private final UpdateNoteUseCase updateNoteUseCase;
  private final DeleteNoteUseCase deleteNoteUseCase;
  private final GetNotesForLinkUseCase getNotesForLinkUseCase;

  public NoteController(
    CreateNoteUseCase createNoteUseCase,
    UpdateNoteUseCase updateNoteUseCase,
    DeleteNoteUseCase deleteNoteUseCase,
    GetNotesForLinkUseCase getNotesForLinkUseCase
  ) {
    this.createNoteUseCase = createNoteUseCase;
    this.updateNoteUseCase = updateNoteUseCase;
    this.deleteNoteUseCase = deleteNoteUseCase;
    this.getNotesForLinkUseCase = getNotesForLinkUseCase;
  }

  public void getNotes(Context ctx) {
    String linkId = Objects.requireNonNull(ctx.pathParam("linkId"));
    String userId = requireUserId(ctx);

    List<Note> notes = getNotesForLinkUseCase.getNotesForLink(linkId, userId);
    ctx.json(notes);
  }

  public void createNote(Context ctx) {
    String linkId = Objects.requireNonNull(ctx.pathParam("linkId"));
    String userId = requireUserId(ctx);

    CreateNoteRequest request = ctx.bodyAsClass(CreateNoteRequest.class);
    CreateNoteCommand command = new CreateNoteCommand(linkId, userId, request.content());

    Note note = createNoteUseCase.createNote(command);
    ctx.status(201).json(note);
  }

  public void updateNote(Context ctx) {
    String noteId = Objects.requireNonNull(ctx.pathParam("noteId"));
    String userId = requireUserId(ctx);

    UpdateNoteRequest request = ctx.bodyAsClass(UpdateNoteRequest.class);
    UpdateNoteCommand command = new UpdateNoteCommand(noteId, userId, request.content());

    Note note = updateNoteUseCase.updateNote(command);
    ctx.json(note);
  }

  public void deleteNote(Context ctx) {
    String noteId = Objects.requireNonNull(ctx.pathParam("noteId"));
    String userId = requireUserId(ctx);

    deleteNoteUseCase.deleteNote(noteId, userId);
    ctx.status(204);
  }

  private String requireUserId(Context ctx) {
    String userId = SecurityContext.getCurrentUserId(ctx);
    if (userId == null) {
      throw AuthenticationException.unauthorizedAccess();
    }
    return userId;
  }

  public record CreateNoteRequest(String content) {}

  public record UpdateNoteRequest(String content) {}
}
