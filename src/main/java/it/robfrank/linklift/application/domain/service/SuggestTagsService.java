package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.Tag;
import it.robfrank.linklift.application.port.in.SuggestTagsUseCase;
import it.robfrank.linklift.application.port.out.LoadContentPort;
import it.robfrank.linklift.application.port.out.TagRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Suggests tags for a link based on tags used on similar links.
 * Uses content similarity (via vector search on already-embedded content)
 * to find related links and returns their tags as suggestions.
 */
public class SuggestTagsService implements SuggestTagsUseCase {

  private static final Logger logger = LoggerFactory.getLogger(SuggestTagsService.class);
  private static final int MAX_SIMILAR_LINKS = 5;
  private static final int MAX_SUGGESTIONS = 10;

  private final TagRepository tagRepository;
  private final LoadContentPort loadContentPort;

  public SuggestTagsService(TagRepository tagRepository, LoadContentPort loadContentPort) {
    this.tagRepository = tagRepository;
    this.loadContentPort = loadContentPort;
  }

  @Override
  public List<Tag> suggestTags(@NonNull String linkId, @NonNull String userId) {
    try {
      Optional<it.robfrank.linklift.application.domain.model.Content> contentOpt = loadContentPort.findContentByLinkId(linkId);
      if (contentOpt.isEmpty() || contentOpt.get().embedding() == null || contentOpt.get().embedding().length == 0) {
        // No embedding available - fall back to returning user's existing tags
        return tagRepository.findByUserId(userId).stream().limit(MAX_SUGGESTIONS).toList();
      }

      float[] rawEmbedding = contentOpt.get().embedding();
      List<Float> embedding = new ArrayList<>(rawEmbedding.length);
      for (float v : rawEmbedding) embedding.add(v);
      var similarContents = loadContentPort.findSimilar(embedding, MAX_SIMILAR_LINKS + 1);

      List<String> currentLinkTagIds = tagRepository.findTagsForLink(linkId).stream().map(Tag::id).toList();

      List<Tag> suggestions = new ArrayList<>();
      for (var similar : similarContents) {
        if (similar.linkId().equals(linkId)) continue;
        List<Tag> tagsForSimilar = tagRepository.findTagsForLink(similar.linkId());
        for (Tag tag : tagsForSimilar) {
          if (tag.userId().equals(userId) && !currentLinkTagIds.contains(tag.id()) && !suggestions.contains(tag)) {
            suggestions.add(tag);
          }
          if (suggestions.size() >= MAX_SUGGESTIONS) break;
        }
        if (suggestions.size() >= MAX_SUGGESTIONS) break;
      }

      return suggestions;
    } catch (Exception e) {
      logger.warn("Failed to suggest tags for link {}, returning empty list", linkId, e);
      return List.of();
    }
  }
}
