package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.adapter.out.persitence.LinkPersistenceAdapter;
import it.robfrank.linklift.application.domain.event.LinkCreatedEvent;
import it.robfrank.linklift.application.domain.exception.LinkAlreadyExistsException;
import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.in.NewLinkCommand;
import it.robfrank.linklift.application.port.in.NewLinkUseCase;
import it.robfrank.linklift.application.port.out.DomainEventPublisher;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.UUID;

public class NewLinkService implements NewLinkUseCase {

  private final LinkPersistenceAdapter linkPersistenceAdapter;
  private final DomainEventPublisher eventPublisher;

  public NewLinkService(LinkPersistenceAdapter linkPersistenceAdapter, DomainEventPublisher eventPublisher) {
    this.linkPersistenceAdapter = linkPersistenceAdapter;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Link newLink(NewLinkCommand newLinkCommand) {
    // Validate the link URL
    validateLinkUrl(newLinkCommand.url());

    // Check if link already exists
    if (linkPersistenceAdapter.findLinkByUrl(newLinkCommand.url()).isPresent()) {
      throw new LinkAlreadyExistsException(newLinkCommand.url());
    }

    var id = UUID.randomUUID().toString();

    var link = new Link(id, newLinkCommand.url(), newLinkCommand.title(), newLinkCommand.description(), LocalDateTime.now(), "text/html");

    var savedLink = linkPersistenceAdapter.saveLink(link);

    eventPublisher.publish(new LinkCreatedEvent(savedLink));

    return savedLink;
  }

  private void validateLinkUrl(String url) {
    ValidationException validationException = new ValidationException("Invalid link data");

    if (url == null || url.isBlank()) {
      validationException.addFieldError("url", "URL cannot be empty");
      throw validationException;
    }

    try {
      new URL(url);
    } catch (MalformedURLException e) {
      validationException.addFieldError("url", "Invalid URL format");
      throw validationException;
    }
  }
}
