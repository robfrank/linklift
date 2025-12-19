package it.robfrank.linklift.adapter.out.persistence;

import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.port.out.LoadContentPort;
import it.robfrank.linklift.application.port.out.SaveContentPort;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public class ContentPersistenceAdapter implements SaveContentPort, LoadContentPort {

  private final ArcadeContentRepository repository;

  public ContentPersistenceAdapter(@NonNull ArcadeContentRepository repository) {
    this.repository = repository;
  }

  @Override
  @NonNull
  public Content saveContent(@NonNull Content content) {
    return repository.save(content);
  }

  @Override
  public void createHasContentEdge(@NonNull String linkId, @NonNull String contentId) {
    repository.createHasContentEdge(linkId, contentId);
  }

  @Override
  @NonNull
  public Optional<Content> findContentByLinkId(@NonNull String linkId) {
    return repository.findByLinkId(linkId);
  }

  @Override
  @NonNull
  public Optional<Content> findContentById(@NonNull String contentId) {
    return repository.findById(contentId);
  }

  @Override
  public void deleteContentByLinkId(@NonNull String linkId) {
    repository.deleteByLinkId(linkId);
  }

  @Override
  @NonNull
  public List<Content> findSimilar(@NonNull List<Float> queryVector, int limit) {
    return repository.findSimilar(queryVector, limit);
  }

  @Override
  @NonNull
  public List<Content> findContentsWithoutEmbeddings(int limit) {
    return repository.findContentsWithoutEmbeddings(limit);
  }
}
