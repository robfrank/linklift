package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.Link;

public interface NewLinkUseCase {
  Link newLink(NewLinkCommand newLinkCommand);
}
