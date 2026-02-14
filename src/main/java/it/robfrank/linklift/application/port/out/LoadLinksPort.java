package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.GraphData;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.LinkPage;
import it.robfrank.linklift.application.port.in.ListLinksQuery;
import java.util.List;

public interface LoadLinksPort {
  LinkPage loadLinks(ListLinksQuery query);

  List<Link> getRelatedLinks(String linkId, String userId);

  List<Link> findLinksByIds(List<String> ids);

  Link getLinkById(String id);

  GraphData getGraphData(String userId);

  boolean userOwnsLink(String userId, String linkId);
}
