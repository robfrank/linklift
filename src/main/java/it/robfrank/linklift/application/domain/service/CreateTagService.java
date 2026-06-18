package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.Tag;
import it.robfrank.linklift.application.domain.validation.ValidationUtils;
import it.robfrank.linklift.application.port.in.CreateTagCommand;
import it.robfrank.linklift.application.port.in.CreateTagUseCase;
import it.robfrank.linklift.application.port.out.TagRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.jspecify.annotations.NonNull;

public class CreateTagService implements CreateTagUseCase {

  private static final int MAX_TAG_NAME_LENGTH = 100;

  private final TagRepository tagRepository;

  public CreateTagService(TagRepository tagRepository) {
    this.tagRepository = tagRepository;
  }

  @Override
  public Tag createTag(@NonNull CreateTagCommand command) {
    ValidationUtils.requireNotEmpty(command.name(), "name");
    ValidationUtils.requireMaxLength(command.name(), MAX_TAG_NAME_LENGTH, "name");
    ValidationUtils.requireNotEmpty(command.userId(), "userId");

    String normalizedName = command.name().toLowerCase().strip();

    // Return existing tag if it already exists for this user
    return tagRepository
      .findByNameAndUserId(normalizedName, command.userId())
      .orElseGet(() -> {
        Tag tag = new Tag(UUID.randomUUID().toString(), normalizedName, command.userId(), LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return tagRepository.save(tag);
      });
  }
}
