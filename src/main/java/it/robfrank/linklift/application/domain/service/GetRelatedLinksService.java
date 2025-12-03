package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.GetRelatedLinksUseCase;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import java.util.List;
import org.jspecify.annotations.NonNull;

public class GetRelatedLinksService implements GetRelatedLinksUseCase {

  private final LoadLinksPort loadLinksPort;

  public GetRelatedLinksService(LoadLinksPort loadLinksPort) {
    this.loadLinksPort = loadLinksPort;
  }

  @Override
  public @NonNull List<Link> getRelatedLinks(@NonNull String linkId, @NonNull String userId) {
    ValidationUtils.requireNotEmpty(linkId, "linkId");
    ValidationUtils.requireNotEmpty(userId, "userId");
    return loadLinksPort.getRelatedLinks(linkId, userId);
  }
}
