package it.robfrank.linklift.adapter.in.web;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import it.robfrank.linklift.adapter.in.web.error.GlobalExceptionHandler;
import it.robfrank.linklift.application.domain.exception.ErrorCode;
import it.robfrank.linklift.application.domain.exception.LinkLiftException;
import it.robfrank.linklift.application.domain.model.Collection;
import it.robfrank.linklift.application.domain.model.CollectionWithLinks;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.SecurityContext;
import it.robfrank.linklift.application.port.in.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CollectionControllerTest {

  private CreateCollectionUseCase createCollectionUseCase;
  private ListCollectionsUseCase listCollectionsUseCase;
  private GetCollectionUseCase getCollectionUseCase;
  private AddLinkToCollectionUseCase addLinkToCollectionUseCase;
  private RemoveLinkFromCollectionUseCase removeLinkFromCollectionUseCase;
  private DeleteCollectionUseCase deleteCollectionUseCase;
  private CollectionController collectionController;

  @BeforeEach
  void setUp() {
    createCollectionUseCase = Mockito.mock(CreateCollectionUseCase.class);
    listCollectionsUseCase = Mockito.mock(ListCollectionsUseCase.class);
    getCollectionUseCase = Mockito.mock(GetCollectionUseCase.class);
    addLinkToCollectionUseCase = Mockito.mock(AddLinkToCollectionUseCase.class);
    removeLinkFromCollectionUseCase = Mockito.mock(RemoveLinkFromCollectionUseCase.class);
    deleteCollectionUseCase = Mockito.mock(DeleteCollectionUseCase.class);

    collectionController = new CollectionController(
      createCollectionUseCase,
      listCollectionsUseCase,
      getCollectionUseCase,
      addLinkToCollectionUseCase,
      removeLinkFromCollectionUseCase,
      deleteCollectionUseCase
    );
  }

  /**
   * Helper method to configure authentication for tests.
   * Sets up a SecurityContext with the given userId in the Javalin app's before filter.
   *
   * @param app the Javalin test app
   * @param userId the user ID to authenticate as
   */
  private void setupAuthentication(Javalin app, String userId) {
    app.before(ctx -> {
      var securityContext = new SecurityContext(
        userId,
        "testuser",
        "test@example.com",
        List.of(),
        true,
        LocalDateTime.now(),
        "127.0.0.1",
        "test-agent"
      );
      it.robfrank.linklift.adapter.in.web.security.SecurityContext.setSecurityContext(ctx, securityContext);
    });
  }

  @Test
  void createCollection_shouldReturn201_whenCollectionIsCreated() {
    // Given
    Collection collection = new Collection("col-123", "My Collection", "Description", "user-123", null);
    when(createCollectionUseCase.createCollection(any(CreateCollectionCommand.class))).thenReturn(collection);

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      setupAuthentication(app, "user-123");
      app.post("/collections", collectionController::createCollection);

      Response response = client.post(
        "/collections",
        """
        {
          "name": "My Collection",
          "description": "Description"
        }
        """
      );

      assertThat(response.code()).isEqualTo(201);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(
        json -> json.node("data").isObject(),
        json -> json.node("data.id").isEqualTo("col-123"),
        json -> json.node("data.name").isEqualTo("My Collection"),
        json -> json.node("data.description").isEqualTo("Description")
      );
    });
  }

  @Test
  void createCollection_shouldReturn401_whenUserNotAuthenticated() {
    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      // Don't set userId attribute
      app.post("/collections", collectionController::createCollection);

      Response response = client.post(
        "/collections",
        """
        {
          "name": "My Collection",
          "description": "Description"
        }
        """
      );

      assertThat(response.code()).isEqualTo(401);
      String responseBody = response.body().string();
      assertThatJson(responseBody).node("message").isEqualTo("Unauthorized");
    });
  }

  @Test
  void listCollections_shouldReturn200_withCollectionsList() {
    // Given
    List<Collection> collections = Arrays.asList(
      new Collection("col-1", "Collection 1", "Desc 1", "user-123", null),
      new Collection("col-2", "Collection 2", "Desc 2", "user-123", null)
    );
    when(listCollectionsUseCase.listCollections(eq("user-123"))).thenReturn(collections);

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      setupAuthentication(app, "user-123");
      app.get("/collections", collectionController::listCollections);

      Response response = client.get("/collections");

      assertThat(response.code()).isEqualTo(200);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(
        json -> json.node("data").isArray().hasSize(2),
        json -> json.node("data[0].id").isEqualTo("col-1"),
        json -> json.node("data[1].id").isEqualTo("col-2")
      );
    });
  }

  @Test
  void listCollections_shouldReturn200_withEmptyList_whenNoCollections() {
    // Given
    when(listCollectionsUseCase.listCollections(eq("user-123"))).thenReturn(List.of());

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      setupAuthentication(app, "user-123");
      app.get("/collections", collectionController::listCollections);

      Response response = client.get("/collections");

      assertThat(response.code()).isEqualTo(200);

      String responseBody = response.body().string();
      assertThatJson(responseBody).node("data").isArray().isEmpty();
    });
  }

  @Test
  void getCollection_shouldReturn200_withCollectionAndLinks() {
    // Given
    Collection collection = new Collection("col-123", "My Collection", "Description", "user-123", null);
    List<Link> links = Arrays.asList(
      new Link("link-1", "https://example.com/1", "Title 1", "Desc 1", LocalDateTime.now(), "text/html"),
      new Link("link-2", "https://example.com/2", "Title 2", "Desc 2", LocalDateTime.now(), "text/html")
    );
    CollectionWithLinks collectionWithLinks = new CollectionWithLinks(collection, links);

    when(getCollectionUseCase.getCollection(eq("col-123"), eq("user-123"))).thenReturn(collectionWithLinks);

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      setupAuthentication(app, "user-123");
      app.get("/collections/{id}", collectionController::getCollection);

      Response response = client.get("/collections/col-123");

      assertThat(response.code()).isEqualTo(200);

      String responseBody = response.body().string();
      assertThatJson(responseBody).and(
        json -> json.node("data.collection").isObject(),
        json -> json.node("data.collection.id").isEqualTo("col-123"),
        json -> json.node("data.links").isArray().hasSize(2)
      );
    });
  }

  @Test
  void getCollection_shouldReturn404_whenCollectionNotFound() {
    // Given
    when(getCollectionUseCase.getCollection(any(), any())).thenThrow(new LinkLiftException("Collection not found", ErrorCode.COLLECTION_NOT_FOUND));

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      setupAuthentication(app, "user-123");
      app.get("/collections/{id}", collectionController::getCollection);

      Response response = client.get("/collections/non-existent");

      assertThat(response.code()).isEqualTo(404);
      String responseBody = response.body().string();
      assertThatJson(responseBody).node("message").isString().contains("Collection not found");
    });
  }

  @Test
  void addLinkToCollection_shouldReturn204_whenLinkIsAdded() {
    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      setupAuthentication(app, "user-123");
      app.post("/collections/{id}/links", collectionController::addLinkToCollection);

      Response response = client.post(
        "/collections/col-123/links",
        """
        {
          "linkId": "link-456"
        }
        """
      );

      assertThat(response.code()).isEqualTo(204);
      verify(addLinkToCollectionUseCase).addLinkToCollection(any(AddLinkToCollectionCommand.class));
    });
  }

  @Test
  void addLinkToCollection_shouldReturn404_whenCollectionNotFound() {
    // Given
    doThrow(new LinkLiftException("Collection not found", ErrorCode.COLLECTION_NOT_FOUND)).when(addLinkToCollectionUseCase).addLinkToCollection(any());

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      setupAuthentication(app, "user-123");
      app.post("/collections/{id}/links", collectionController::addLinkToCollection);

      Response response = client.post(
        "/collections/non-existent/links",
        """
        {
          "linkId": "link-456"
        }
        """
      );

      assertThat(response.code()).isEqualTo(404);
    });
  }

  @Test
  void removeLinkFromCollection_shouldReturn204_whenLinkIsRemoved() {
    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      setupAuthentication(app, "user-123");
      app.delete("/collections/{id}/links/{linkId}", collectionController::removeLinkFromCollection);

      Response response = client.delete("/collections/col-123/links/link-456");

      assertThat(response.code()).isEqualTo(204);
      verify(removeLinkFromCollectionUseCase).removeLinkFromCollection(any(RemoveLinkFromCollectionCommand.class));
    });
  }

  @Test
  void deleteCollection_shouldReturn204_whenCollectionIsDeleted() {
    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      setupAuthentication(app, "user-123");
      app.delete("/collections/{id}", collectionController::deleteCollection);

      Response response = client.delete("/collections/col-123");

      assertThat(response.code()).isEqualTo(204);
      verify(deleteCollectionUseCase).deleteCollection(eq("col-123"), eq("user-123"));
    });
  }

  @Test
  void deleteCollection_shouldReturn404_whenCollectionNotFound() {
    // Given
    doThrow(new LinkLiftException("Collection not found", ErrorCode.COLLECTION_NOT_FOUND)).when(deleteCollectionUseCase).deleteCollection(any(), any());

    JavalinTest.test((app, client) -> {
      GlobalExceptionHandler.configure(app);
      setupAuthentication(app, "user-123");
      app.delete("/collections/{id}", collectionController::deleteCollection);

      Response response = client.delete("/collections/non-existent");

      assertThat(response.code()).isEqualTo(404);
    });
  }
}
