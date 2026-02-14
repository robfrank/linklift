package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.GetRelatedLinksUseCase;
import it.robfrank.linklift.application.port.out.LoadContentPort;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import java.util.List;
import org.jspecify.annotations.NonNull;

public class GetRelatedLinksService implements GetRelatedLinksUseCase {

  private static final int MAX_SIMILAR_LINKS = 10;

  private final LoadLinksPort loadLinksPort;
  private final LoadContentPort loadContentPort;

  public GetRelatedLinksService(LoadLinksPort loadLinksPort, LoadContentPort loadContentPort) {
    this.loadLinksPort = loadLinksPort;
    this.loadContentPort = loadContentPort;
  }

  @Override
  public @NonNull List<Link> getRelatedLinks(@NonNull String linkId, @NonNull String userId) {
    ValidationUtils.requireNotEmpty(linkId, "linkId");
    ValidationUtils.requireNotEmpty(userId, "userId");

    // 1. Try vector search first
    var contentOpt = loadContentPort.findContentByLinkId(linkId);
    if (contentOpt.isPresent()) {
      var content = contentOpt.get();
      if (content.embedding() != null && content.embedding().length > 0) {
        float[] embedding = content.embedding();
        var embeddingList = new java.util.ArrayList<Float>(embedding.length);
        for (float f : embedding) {
          embeddingList.add(f);
        }

        var similarContents = loadContentPort.findSimilar(embeddingList, MAX_SIMILAR_LINKS);
        var linkIds = similarContents
          .stream()
          .map(it.robfrank.linklift.application.domain.model.Content::linkId)
          .filter(id -> !id.equals(linkId))
          .distinct()
          .toList();

        if (!linkIds.isEmpty()) {
          return loadLinksPort.findLinksByIds(linkIds);
        }
      }
    }

    // 2. Fallback to graph traversal
    return loadLinksPort.getRelatedLinks(linkId, userId);
  }
}
