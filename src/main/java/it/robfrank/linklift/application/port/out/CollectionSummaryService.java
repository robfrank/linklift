package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.domain.model.Link;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Port for generating summaries for a collection of links using AI.
 */
public interface CollectionSummaryService {
  /**
   * Generates a summary for the given collection and its links.
   *
   * @param collection the collection to summarize
   * @param links the links contained in the collection
   * @return a concise AI-generated summary
   */
  @NonNull
  String generateCollectionSummary(@NonNull Collection collection, @NonNull List<Link> links);
}
