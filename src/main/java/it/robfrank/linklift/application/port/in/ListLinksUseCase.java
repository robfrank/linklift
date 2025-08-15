package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.LinkPage;

public interface ListLinksUseCase {
  LinkPage listLinks(ListLinksQuery query);
}
