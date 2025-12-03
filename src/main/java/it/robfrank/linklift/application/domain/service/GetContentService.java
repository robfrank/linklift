package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.Content;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.GetContentQuery;
import it.robfrank.linklift.application.port.in.GetContentUseCase;
import it.robfrank.linklift.application.port.out.LoadContentPort;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public class GetContentService implements GetContentUseCase {

  private final LoadContentPort loadContentPort;

  public GetContentService(@NonNull LoadContentPort loadContentPort) {
    this.loadContentPort = loadContentPort;
  }

  @Override
  public @NonNull Optional<Content> getContent(@NonNull GetContentQuery query) {
    ValidationUtils.requireNotNull(query, "query");
    ValidationUtils.requireNotEmpty(query.linkId(), "linkId");
    return loadContentPort.findContentByLinkId(query.linkId());
  }
}
