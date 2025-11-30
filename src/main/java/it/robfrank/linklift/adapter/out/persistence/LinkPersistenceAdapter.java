package it.robfrank.linklift.adapter.out.persistence;

import it.robfrank.linklift.application.domain.exception.LinkNotFoundException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.LinkPage;
import it.robfrank.linklift.application.port.in.ListLinksQuery;
import it.robfrank.linklift.application.port.out.DeleteLinkPort;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import it.robfrank.linklift.application.port.out.SaveLinkPort;
import it.robfrank.linklift.application.port.out.UpdateLinkPort;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public class LinkPersistenceAdapter implements SaveLinkPort, LoadLinksPort, UpdateLinkPort, DeleteLinkPort {

  private final ArcadeLinkRepository linkRepository;

  public LinkPersistenceAdapter(ArcadeLinkRepository linkRepository) {
    this.linkRepository = linkRepository;
  }

  @Override
  public Link saveLink(Link link) {
    return linkRepository.saveLink(link);
  }

  /**
   * Save a link with user ownership using graph relationships.
   */
  public Link saveLinkForUser(Link link, String userId) {
    return linkRepository.saveLinkForUser(link, userId);
  }

  @Override
  public Link save(Link link, String userId) {
    return saveLinkForUser(link, userId);
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

  @Override
  public LinkPage loadLinks(ListLinksQuery query) {
    return linkRepository.findLinksWithPagination(query);
  }

  /**
   * Load links for a specific user using graph traversal.
   */
  public LinkPage loadLinksForUser(ListLinksQuery query, String userId) {
    return linkRepository.findLinksWithPaginationForUser(query, userId);
  }

  /**
   * Check if a user owns a specific link.
   */
  public boolean userOwnsLink(String userId, String linkId) {
    return linkRepository.userOwnsLink(userId, linkId);
  }

  /**
   * Get the owner of a specific link.
   */
  public Optional<String> getLinkOwner(String linkId) {
    return linkRepository.getLinkOwner(linkId);
  }

  /**
   * Delete a link and its relationships.
   */
  @Override
  public void deleteLink(@NonNull String linkId) {
    linkRepository.deleteLink(linkId);
  }

  /**
   * Transfer ownership of a link between users.
   */
  public void transferLinkOwnership(String linkId, String fromUserId, String toUserId) {
    linkRepository.transferLinkOwnership(linkId, fromUserId, toUserId);
  }

  @Override
  public List<Link> getRelatedLinks(String linkId, String userId) {
    return linkRepository.getRelatedLinks(linkId, userId);
  }

  @Override
  public @NonNull Link updateLink(@NonNull Link link) {
    return java.util.Objects.requireNonNull(linkRepository.updateLink(link));
  }
}
