package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.Link;

public interface NewLinkUseCase {

  boolean newLink(NewLinkCommand newLinkCommand);
}
