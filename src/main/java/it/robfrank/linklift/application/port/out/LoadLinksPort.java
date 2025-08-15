package it.robfrank.linklift.application.port.out;

import it.robfrank.linklift.application.domain.model.LinkPage;
import it.robfrank.linklift.application.port.in.ListLinksQuery;

public interface LoadLinksPort {
  LinkPage loadLinks(ListLinksQuery query);
}
