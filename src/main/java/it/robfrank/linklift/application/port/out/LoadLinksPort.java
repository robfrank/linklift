package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.GraphData;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.LinkPage;
import it.robfrank.linklift.application.port.in.ListLinksQuery;
import java.util.List;
import java.util.Optional;

public interface LoadLinksPort {
  LinkPage loadLinks(ListLinksQuery query);

  List<Link> getRelatedLinks(String linkId, String userId);

  List<Link> findLinksByIds(List<String> ids);

  Link getLinkById(String id);

  /** Loads a link by id only if it is owned by the given user; combines existence + ownership in one query. */
  Optional<Link> findLinkByIdAndUserId(String id, String userId);

  GraphData getGraphData(String userId);

  boolean userOwnsLink(String userId, String linkId);
}
