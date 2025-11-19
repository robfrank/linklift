package it.robfrank.linklift.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.robfrank.linklift.application.domain.event.LinksQueryEvent;
import it.robfrank.linklift.application.domain.exception.ValidationException;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.LinkPage;
import it.robfrank.linklift.application.port.in.ListLinksQuery;
import it.robfrank.linklift.application.port.out.DomainEventPublisher;
import it.robfrank.linklift.application.port.out.LoadLinksPort;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListLinksServiceTest {

  @Mock
  private LoadLinksPort loadLinksPort;

  @Mock
  private DomainEventPublisher eventPublisher;

  private ListLinksService listLinksService;

  @BeforeEach
  void setUp() {
    listLinksService = new ListLinksService(loadLinksPort, eventPublisher);
  }

  @Test
  void listLinks_shouldReturnLinkPage_whenValidQuery() {
    // Given
    ListLinksQuery query = new ListLinksQuery(0, 20, "extractedAt", "DESC", "user-123");
    List<Link> links = List.of(new Link("1", "https://example.com", "Example", "Description", LocalDateTime.now(), "text/html", null, null, null));
    LinkPage expectedPage = new LinkPage(links, 0, 20, 1, 1, false, false);

    when(loadLinksPort.loadLinks(query)).thenReturn(expectedPage);

    // When
    LinkPage result = listLinksService.listLinks(query);

    // Then
    assertThat(result).isEqualTo(expectedPage);
    verify(loadLinksPort).loadLinks(query);
  }

  @Test
  void listLinks_shouldPublishEvent_whenSuccessful() {
    // Given
    ListLinksQuery query = new ListLinksQuery(0, 20, "extractedAt", "DESC", "user-123");
    LinkPage linkPage = new LinkPage(List.of(), 0, 20, 0, 0, false, false);

    when(loadLinksPort.loadLinks(query)).thenReturn(linkPage);

    // When
    listLinksService.listLinks(query);

    // Then
    ArgumentCaptor<LinksQueryEvent> eventCaptor = ArgumentCaptor.forClass(LinksQueryEvent.class);
    verify(eventPublisher).publish(eventCaptor.capture());

    LinksQueryEvent event = eventCaptor.getValue();
    assertThat(event.getQuery()).isEqualTo(query);
    assertThat(event.getResultCount()).isEqualTo(0L);
  }

  @Test
  void listLinks_shouldThrowValidationException_whenPageIsNegative() {
    // Given
    ListLinksQuery query = new ListLinksQuery(-1, 20, "extractedAt", "DESC", "user-123");

    // When & Then
    assertThatThrownBy(() -> listLinksService.listLinks(query))
      .isInstanceOf(ValidationException.class)
      .hasMessage("Invalid query parameters")
      .satisfies(ex -> {
        ValidationException validationEx = (ValidationException) ex;
        assertThat(validationEx.getFieldErrors()).containsKey("page");
        assertThat(validationEx.getFieldErrors().get("page")).isEqualTo("Page must be >= 0");
      });
  }

  @Test
  void listLinks_shouldThrowValidationException_whenSizeIsInvalid() {
    // Given
    ListLinksQuery query1 = new ListLinksQuery(0, 0, "extractedAt", "DESC", "user-123");
    ListLinksQuery query2 = new ListLinksQuery(0, 101, "extractedAt", "DESC", "user-123");

    // When & Then
    assertThatThrownBy(() -> listLinksService.listLinks(query1))
      .isInstanceOf(ValidationException.class)
      .satisfies(ex -> {
        ValidationException validationEx = (ValidationException) ex;
        assertThat(validationEx.getFieldErrors()).containsKey("size");
        assertThat(validationEx.getFieldErrors().get("size")).isEqualTo("Size must be between 1 and 100");
      });

    assertThatThrownBy(() -> listLinksService.listLinks(query2))
      .isInstanceOf(ValidationException.class)
      .satisfies(ex -> {
        ValidationException validationEx = (ValidationException) ex;
        assertThat(validationEx.getFieldErrors()).containsKey("size");
      });
  }

  @Test
  void listLinks_shouldThrowValidationException_whenInvalidSortField() {
    // Given
    ListLinksQuery query = new ListLinksQuery(0, 20, "invalidField", "DESC", "user-123");

    // When & Then
    assertThatThrownBy(() -> listLinksService.listLinks(query))
      .isInstanceOf(ValidationException.class)
      .satisfies(ex -> {
        ValidationException validationEx = (ValidationException) ex;
        assertThat(validationEx.getFieldErrors()).containsKey("sortBy");
        assertThat(validationEx.getFieldErrors().get("sortBy")).contains("Invalid sort field");
      });
  }

  @Test
  void listLinks_shouldThrowValidationException_whenInvalidSortDirection() {
    // Given
    ListLinksQuery query = new ListLinksQuery(0, 20, "extractedAt", "INVALID", "user-123");

    // When & Then
    assertThatThrownBy(() -> listLinksService.listLinks(query))
      .isInstanceOf(ValidationException.class)
      .satisfies(ex -> {
        ValidationException validationEx = (ValidationException) ex;
        assertThat(validationEx.getFieldErrors()).containsKey("sortDirection");
        assertThat(validationEx.getFieldErrors().get("sortDirection")).contains("Sort direction must be ASC or DESC");
      });
  }

  @Test
  void listLinks_shouldAcceptValidSortFields() {
    // Given
    String[] validFields = { "id", "url", "title", "description", "extractedAt", "contentType" };
    LinkPage linkPage = new LinkPage(List.of(), 0, 20, 0, 0, false, false);

    when(loadLinksPort.loadLinks(any(ListLinksQuery.class))).thenReturn(linkPage);

    // When & Then
    for (String field : validFields) {
      ListLinksQuery query = new ListLinksQuery(0, 20, field, "ASC", "user-123");
      // Should not throw exception
      listLinksService.listLinks(query);
    }

    verify(loadLinksPort, org.mockito.Mockito.times(validFields.length)).loadLinks(any(ListLinksQuery.class));
  }

  @Test
  void listLinks_shouldAcceptValidSortDirections() {
    // Given
    String[] validDirections = { "ASC", "DESC", "asc", "desc" };
    LinkPage linkPage = new LinkPage(List.of(), 0, 20, 0, 0, false, false);

    when(loadLinksPort.loadLinks(any(ListLinksQuery.class))).thenReturn(linkPage);

    // When & Then
    for (String direction : validDirections) {
      ListLinksQuery query = new ListLinksQuery(0, 20, "extractedAt", direction, "user-123");
      // Should not throw exception
      listLinksService.listLinks(query);
    }

    verify(loadLinksPort, org.mockito.Mockito.times(validDirections.length)).loadLinks(any(ListLinksQuery.class));
  }
}
