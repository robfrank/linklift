package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.Note;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.GetNotesForLinkUseCase;
import it.robfrank.linklift.application.port.out.NoteRepository;
import java.util.List;
import org.jspecify.annotations.NonNull;

public class GetNotesForLinkService implements GetNotesForLinkUseCase {

  private final NoteRepository noteRepository;

  public GetNotesForLinkService(NoteRepository noteRepository) {
    this.noteRepository = noteRepository;
  }

  @Override
  public List<Note> getNotesForLink(@NonNull String linkId, @NonNull String userId) {
    ValidationUtils.requireNotEmpty(linkId, "linkId");
    ValidationUtils.requireNotEmpty(userId, "userId");

    return noteRepository.findByLinkIdAndUserId(linkId, userId);
  }
}
