package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.Tag;
import it.robfrank.linklift.application.port.in.ListTagsUseCase;
import it.robfrank.linklift.application.port.out.TagRepository;
import java.util.List;
import org.jspecify.annotations.NonNull;

public class ListTagsService implements ListTagsUseCase {

  private final TagRepository tagRepository;

  public ListTagsService(TagRepository tagRepository) {
    this.tagRepository = tagRepository;
  }

  @Override
  public List<Tag> listTags(@NonNull String userId) {
    return tagRepository.findByUserId(userId);
  }
}
