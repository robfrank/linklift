package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.Link;

public interface SaveLinkPort {
  Link saveLink(Link link);

  Link save(Link link, String userId);

  void syncLinkConnections(Link link);
}
