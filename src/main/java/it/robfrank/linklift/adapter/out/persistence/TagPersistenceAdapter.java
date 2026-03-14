package it.robfrank.linklift.adapter.out.persistence;

import it.robfrank.linklift.application.domain.model.Tag;
import it.robfrank.linklift.application.port.out.TagRepository;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public class TagPersistenceAdapter implements TagRepository {

  private final ArcadeTagRepository tagRepository;

  public TagPersistenceAdapter(ArcadeTagRepository tagRepository) {
    this.tagRepository = tagRepository;
  }

  @Override
  public Tag save(@NonNull Tag tag) {
    return tagRepository.save(tag);
  }

  @Override
  public void delete(@NonNull String tagId) {
    tagRepository.delete(tagId);
  }

  @Override
  public Optional<Tag> findById(@NonNull String tagId) {
    return tagRepository.findById(tagId);
  }

  @Override
  public Optional<Tag> findByNameAndUserId(@NonNull String name, @NonNull String userId) {
    return tagRepository.findByNameAndUserId(name, userId);
  }

  @Override
  public List<Tag> findByUserId(@NonNull String userId) {
    return tagRepository.findByUserId(userId);
  }

  @Override
  public List<Tag> findTagsForLink(@NonNull String linkId) {
    return tagRepository.findTagsForLink(linkId);
  }

  @Override
  public void addTagToLink(@NonNull String linkId, @NonNull String tagId) {
    tagRepository.addTagToLink(linkId, tagId);
  }

  @Override
  public void removeTagFromLink(@NonNull String linkId, @NonNull String tagId) {
    tagRepository.removeTagFromLink(linkId, tagId);
  }

  @Override
  public List<String> findLinkIdsByTagId(@NonNull String tagId) {
    return tagRepository.findLinkIdsByTagId(tagId);
  }
}
