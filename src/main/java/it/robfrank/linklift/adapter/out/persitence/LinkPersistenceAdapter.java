package it.robfrank.linklift.adapter.out.persitence;

import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.out.SaveLinkPort;

public class LinkPersistenceAdapter implements SaveLinkPort {

  private final ArcadeLinkRepository linkRepository;

  public LinkPersistenceAdapter(ArcadeLinkRepository linkRepository) {
    this.linkRepository = linkRepository;
  }

  @Override
  public Link saveLink(Link link) {
    return linkRepository.findLinkByUrl(link.url()).orElseGet(() -> linkRepository.saveLink(link));
  }
}
