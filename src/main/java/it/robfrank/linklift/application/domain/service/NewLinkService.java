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
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewLinkService implements NewLinkUseCase {

  private static final Logger logger = LoggerFactory.getLogger(NewLinkService.class);

  private final LinkPersistenceAdapter linkPersistenceAdapter;
  private final DomainEventPublisher eventPublisher;

  public NewLinkService(@NonNull LinkPersistenceAdapter linkPersistenceAdapter, @NonNull DomainEventPublisher eventPublisher) {
    this.linkPersistenceAdapter = linkPersistenceAdapter;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public @NonNull Link newLink(@NonNull NewLinkCommand newLinkCommand) {
    // Validate the link URL
    validateLinkUrl(newLinkCommand.url());

    // Validate userId is provided (required for user-owned links)
    if (newLinkCommand.userId() == null || newLinkCommand.userId().isBlank()) {
      ValidationException validationException = new ValidationException("Invalid link data");
      validationException.addFieldError("userId", "User ID is required");
      throw validationException;
    }

    // Check if link already exists for this user
    if (linkPersistenceAdapter.findLinkByUrl(newLinkCommand.url()).isPresent()) {
      throw new LinkAlreadyExistsException(newLinkCommand.url());
    }

    var id = UUID.randomUUID().toString();

    var link = new Link(id, newLinkCommand.url(), newLinkCommand.title(), newLinkCommand.description(), LocalDateTime.now(), "text/html");

    var savedLink = linkPersistenceAdapter.saveLinkForUser(link, newLinkCommand.userId());

    logger.debug("savedLink = {}", savedLink);

    // Trigger async content download
    //    downloadContentUseCase.downloadContentAsync(new DownloadContentCommand(savedLink.id(), savedLink.url()));

    eventPublisher.publish(new LinkCreatedEvent(savedLink, newLinkCommand.userId()));

    return savedLink;
  }

  private void validateLinkUrl(@NonNull String url) {
    ValidationException validationException = new ValidationException("Invalid link data");

    if (url == null || url.isBlank()) {
      validationException.addFieldError("url", "URL cannot be empty");
      throw validationException;
    }

    try {
      new URI(url).toURL();
    } catch (MalformedURLException | URISyntaxException e) {
      validationException.addFieldError("url", "Invalid URL format");
      throw validationException;
    }
  }
}
