package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.Tag;
import it.robfrank.linklift.application.port.in.CreateTagCommand;
import it.robfrank.linklift.application.port.out.TagRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateTagServiceTest {

  @Mock
  private TagRepository tagRepository;

  private CreateTagService service;

  @BeforeEach
  void setUp() {
    service = new CreateTagService(tagRepository);
  }

  @Test
  void shouldCreateNewTag() {
    Tag savedTag = new Tag(UUID.randomUUID().toString(), "java", "user-1", LocalDateTime.now());
    when(tagRepository.findByNameAndUserId("java", "user-1")).thenReturn(Optional.empty());
    when(tagRepository.save(any())).thenReturn(savedTag);

    Tag result = service.createTag(new CreateTagCommand("java", "user-1"));

    assertThat(result.name()).isEqualTo("java");
    verify(tagRepository).save(any());
  }

  @Test
  void shouldNormalizeTagNameToLowercase() {
    Tag savedTag = new Tag(UUID.randomUUID().toString(), "java", "user-1", LocalDateTime.now());
    when(tagRepository.findByNameAndUserId("java", "user-1")).thenReturn(Optional.empty());
    when(tagRepository.save(any())).thenReturn(savedTag);

    service.createTag(new CreateTagCommand("Java", "user-1"));

    verify(tagRepository).findByNameAndUserId("java", "user-1");
  }

  @Test
  void shouldReturnExistingTagIfAlreadyExists() {
    Tag existing = new Tag("existing-id", "java", "user-1", LocalDateTime.now());
    when(tagRepository.findByNameAndUserId("java", "user-1")).thenReturn(Optional.of(existing));

    Tag result = service.createTag(new CreateTagCommand("java", "user-1"));

    assertThat(result.id()).isEqualTo("existing-id");
    verify(tagRepository, never()).save(any());
  }

  @Test
  void shouldFailWithBlankName() {
    assertThatThrownBy(() -> service.createTag(new CreateTagCommand("", "user-1"))).isInstanceOf(ValidationException.class);
  }

  @Test
  void shouldFailWithBlankUserId() {
    assertThatThrownBy(() -> service.createTag(new CreateTagCommand("java", ""))).isInstanceOf(ValidationException.class);
  }
}
