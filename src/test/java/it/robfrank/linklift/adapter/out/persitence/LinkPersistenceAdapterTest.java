package it.robfrank.linklift.adapter.out.persitence;

import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.LinkPage;
import it.robfrank.linklift.application.port.in.ListLinksQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
class LinkPersistenceAdapterTest {

  @Mock
  private ArcadeLinkRepository linkRepository;

  private LinkPersistenceAdapter linkPersistenceAdapter;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    linkPersistenceAdapter = new LinkPersistenceAdapter(linkRepository);
  }

  @Test
  void loadLinks_shouldCallRepository_andReturnResult() {
    // Given
    ListLinksQuery query = new ListLinksQuery(0, 20, "extractedAt", "DESC", "user1");
    List<Link> links = List.of(
        new Link("1", "https://example.com", "Example", "Description", LocalDateTime.now(), "text/html", "user1")
    );
    LinkPage expectedPage = new LinkPage(links, 0, 20, 1, 1, false, false);

    when(linkRepository.findLinksWithPagination(query)).thenReturn(expectedPage);

    // When
    LinkPage result = linkPersistenceAdapter.loadLinks(query);

    // Then
    assertThat(result).isEqualTo(expectedPage);
    verify(linkRepository).findLinksWithPagination(query);
  }

  @Test
  void loadLinks_shouldHandleEmptyResults() {
    // Given
    ListLinksQuery query = new ListLinksQuery(0, 20, "extractedAt", "DESC", "user1");
    LinkPage emptyPage = new LinkPage(List.of(), 0, 20, 0, 0, false, false);

    when(linkRepository.findLinksWithPagination(query)).thenReturn(emptyPage);

    // When
    LinkPage result = linkPersistenceAdapter.loadLinks(query);

    // Then
    assertThat(result.content()).isEmpty();
    assertThat(result.totalElements()).isEqualTo(0L);
    assertThat(result.totalPages()).isEqualTo(0);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.hasPrevious()).isFalse();
  }

  @Test
  void loadLinks_shouldHandleMultiplePages() {
    // Given
    ListLinksQuery query = new ListLinksQuery(1, 10, "extractedAt", "DESC", "user1");
    List<Link> links = List.of(
        new Link("11", "https://example11.com", "Example 11", "Description", LocalDateTime.now(), "text/html", "user1"),
        new Link("12", "https://example12.com", "Example 12", "Description", LocalDateTime.now(), "text/html", "user1")
    );
    LinkPage page = new LinkPage(links, 1, 10, 25, 3, true, true);

    when(linkRepository.findLinksWithPagination(query)).thenReturn(page);

    // When
    LinkPage result = linkPersistenceAdapter.loadLinks(query);

    // Then
    assertThat(result.content()).hasSize(2);
    assertThat(result.page()).isEqualTo(1);
    assertThat(result.size()).isEqualTo(10);
    assertThat(result.totalElements()).isEqualTo(25L);
    assertThat(result.totalPages()).isEqualTo(3);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.hasPrevious()).isTrue();
  }
}
