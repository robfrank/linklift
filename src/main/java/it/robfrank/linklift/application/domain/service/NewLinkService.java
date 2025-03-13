package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.adapter.out.persitence.LinkPersistenceAdapter;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.port.in.NewLinkCommand;
import it.robfrank.linklift.application.port.in.NewLinkUseCase;

import java.time.LocalDateTime;
import java.util.UUID;

public class NewLinkService implements NewLinkUseCase {

  private final LinkPersistenceAdapter linkPersistenceAdapter;

  public NewLinkService(LinkPersistenceAdapter linkPersistenceAdapter) {
    this.linkPersistenceAdapter = linkPersistenceAdapter;
  }

  @Override
  public boolean newLink(NewLinkCommand newLinkCommand) {
    var id = UUID.randomUUID().toString();

    var savedLink = linkPersistenceAdapter.saveLink(
      new Link(id, newLinkCommand.url(), newLinkCommand.title(), newLinkCommand.description(), LocalDateTime.now(), "text/html")
    );

    return savedLink.id().equals(id);
  }
}
