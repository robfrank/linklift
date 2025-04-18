package it.robfrank.linklift.adapter.out.persitence;

import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.out.SaveLinkPort;
import java.util.Optional;

public class LinkPersistenceAdapter implements SaveLinkPort {

  private final ArcadeLinkRepository linkRepository;

  public LinkPersistenceAdapter(ArcadeLinkRepository linkRepository) {
    this.linkRepository = linkRepository;
  }

  @Override
  public Link saveLink(Link link) {
    return linkRepository.saveLink(link);
  }

  public Optional<Link> findLinkByUrl(String url) {
    return linkRepository.findLinkByUrl(url);
  }

  public Link getLinkByUrl(String url) {
    return linkRepository.findLinkByUrl(url).orElseThrow(() -> new LinkNotFoundException("No link found with URL: " + url));
  }

  public Link getLinkById(String id) {
    return linkRepository.findLinkById(id).orElseThrow(() -> new LinkNotFoundException(id));
  }
}
