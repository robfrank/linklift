package it.robfrank.linklift.application.domain.event;

import it.robfrank.linklift.application.port.in.ListLinksQuery;

public class LinksQueryEvent extends DomainEvent {

  private final ListLinksQuery query;
  private final long resultCount;

  public LinksQueryEvent(ListLinksQuery query, long resultCount) {
    super();
    this.query = query;
    this.resultCount = resultCount;
  }

  public ListLinksQuery getQuery() {
    return query;
  }

  public long getResultCount() {
    return resultCount;
  }
}
