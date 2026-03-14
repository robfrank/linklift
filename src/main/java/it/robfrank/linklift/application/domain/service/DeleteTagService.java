package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.exception.AuthenticationException;
import it.robfrank.linklift.application.domain.exception.TagNotFoundException;
import it.robfrank.linklift.application.port.in.DeleteTagUseCase;
import it.robfrank.linklift.application.port.out.TagRepository;
import org.jspecify.annotations.NonNull;

public class DeleteTagService implements DeleteTagUseCase {

  private final TagRepository tagRepository;

  public DeleteTagService(TagRepository tagRepository) {
    this.tagRepository = tagRepository;
  }

  @Override
  public void deleteTag(@NonNull String tagId, @NonNull String userId) {
    var tag = tagRepository.findById(tagId).orElseThrow(() -> new TagNotFoundException(tagId));

    if (!tag.userId().equals(userId)) {
      throw AuthenticationException.unauthorizedAccess();
    }

    tagRepository.delete(tagId);
  }
}
