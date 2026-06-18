package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.model.Tag;
import it.robfrank.linklift.application.port.in.SuggestTagsUseCase;
import it.robfrank.linklift.application.port.out.LoadContentPort;
import it.robfrank.linklift.application.port.out.TagRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
      Optional<Content> contentOpt = loadContentPort.findContentByLinkId(linkId);
      if (contentOpt.isEmpty() || contentOpt.get().embedding() == null || contentOpt.get().embedding().length == 0) {
        // No embedding available - fall back to returning user's existing tags
        return tagRepository.findByUserId(userId).stream().limit(MAX_SUGGESTIONS).toList();
      }

      float[] rawEmbedding = contentOpt.get().embedding();
      List<Float> embedding = new ArrayList<>(rawEmbedding.length);
      for (float v : rawEmbedding) embedding.add(v);
      var similarContents = loadContentPort.findSimilar(embedding, MAX_SIMILAR_LINKS + 1, userId);

      Set<String> excludedTagIds = tagRepository.findTagsForLink(linkId).stream().map(Tag::id).collect(Collectors.toSet());

      // Batch-load tags for all similar links in one query instead of one query per link.
      List<String> similarLinkIds = similarContents.stream().map(Content::linkId).filter(id -> !id.equals(linkId)).distinct().toList();

      List<Tag> suggestions = new ArrayList<>();
      Set<String> addedTagIds = new HashSet<>();
      for (Tag tag : tagRepository.findTagsForLinks(similarLinkIds)) {
        // O(1) membership checks: skip the current link's tags and any already-suggested tag.
        if (tag.userId().equals(userId) && !excludedTagIds.contains(tag.id()) && addedTagIds.add(tag.id())) {
          suggestions.add(tag);
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
