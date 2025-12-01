package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.port.in.DeleteContentCommand;
import it.robfrank.linklift.application.port.in.DeleteContentUseCase;
import it.robfrank.linklift.application.port.out.SaveContentPort;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteContentService implements DeleteContentUseCase {

  private static final Logger logger = LoggerFactory.getLogger(DeleteContentService.class);

  private final SaveContentPort saveContentPort;

  public DeleteContentService(@NonNull SaveContentPort saveContentPort) {
    this.saveContentPort = saveContentPort;
  }

  @Override
  public void deleteContent(@NonNull DeleteContentCommand command) {
    logger.atInfo().addArgument(() -> command.linkId()).log("Deleting content for link: {}");
    saveContentPort.deleteContentByLinkId(command.linkId());
    logger.atInfo().addArgument(() -> command.linkId()).log("Content deleted for link: {}");
  }
}
