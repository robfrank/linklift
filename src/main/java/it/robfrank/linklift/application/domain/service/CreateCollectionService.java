package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.CreateCollectionCommand;
import it.robfrank.linklift.application.port.in.CreateCollectionUseCase;
import it.robfrank.linklift.application.port.out.CollectionRepository;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateCollectionService implements CreateCollectionUseCase {

  private static final Logger logger = LoggerFactory.getLogger(CreateCollectionService.class);

  private final CollectionRepository collectionRepository;

  public CreateCollectionService(CollectionRepository collectionRepository) {
    this.collectionRepository = collectionRepository;
  }

  @Override
  public @NonNull Collection createCollection(@NonNull CreateCollectionCommand command) {
    ValidationUtils.requireNotEmpty(command.name(), "name");
    ValidationUtils.requireNotEmpty(command.userId(), "userId");

    var id = UUID.randomUUID().toString();
    var collection = new Collection(id, command.name(), command.description(), command.userId(), command.query(), null);

    logger.info("Creating collection: {}", collection);
    return collectionRepository.save(collection);
  }
}
