package it.robfrank.linklift.adapter.out.persitence;

import com.arcadedb.Constants;
import com.arcadedb.remote.RemoteDatabase;
import it.robfrank.linklift.application.domain.model.Link;
import it.robfrank.linklift.application.domain.model.User;
import it.robfrank.linklift.config.DatabaseInitializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class to demonstrate ArcadeDB graph operations for the LinkLift project.
 * This test shows the improved performance and functionality of using graph relationships
 * instead of denormalized userId fields.
 */
@Testcontainers
class GraphOperationsTest {

    @Container
    private static final GenericContainer<?> arcadeDBContainer = new GenericContainer<>("arcadedata/arcadedb:" + Constants.getRawVersion())
            .withExposedPorts(2480)
            .withStartupTimeout(Duration.ofSeconds(90))
            .withEnv("JAVA_OPTS", """
                    -Darcadedb.dateImplementation=java.time.LocalDate
                    -Darcadedb.dateTimeImplementation=java.time.LocalDateTime
                    -Darcadedb.server.rootPassword=playwithdata
                    -Darcadedb.server.plugins=Postgres:com.arcadedb.postgres.PostgresProtocolPlugin
                    """
            )
            .waitingFor(Wait.forHttp("/api/v1/ready").forPort(2480).forStatusCode(204));

    private RemoteDatabase database;
    private ArcadeLinkRepository linkRepository;
    private ArcadeUserRepository userRepository;

    @BeforeAll
    static void setup() {
        new DatabaseInitializer(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "root",
                "playwithdata").initializeDatabase();
    }

    @BeforeEach
    void setUp() {
        database = new RemoteDatabase(arcadeDBContainer.getHost(), arcadeDBContainer.getMappedPort(2480), "linklift", "root",
                "playwithdata");
        linkRepository = new ArcadeLinkRepository(database, new LinkMapper());
        userRepository = new ArcadeUserRepository(database, new UserMapper());

        // Clean up any existing test data
        cleanupTestData();
    }

    @Test
    void shouldCreateLinkWithGraphRelationship() {
        // Given: Create a test user
        String userId = UUID.randomUUID().toString();
        User testUser = new User(userId, "testuser", "test@example.com", "hashedPassword", "salt",
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), null, true, "Test", "User", null);
        userRepository.save(testUser);

        // And: Create a test link
        String linkId = UUID.randomUUID().toString();
        Link testLink = new Link(linkId, "https://graph-example.com", "Graph Test", "Testing graph relationships",
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), "text/html");

        // When: Save the link with user relationship using graph approach
        Link savedLink = linkRepository.saveLinkForUser(testLink, userId);

        // Then: The link should be saved correctly
        assertThat(savedLink).isNotNull();
        assertThat(savedLink.id()).isEqualTo(linkId);
        assertThat(savedLink.url()).isEqualTo("https://graph-example.com");

        // And: The ownership relationship should exist
        assertThat(linkRepository.userOwnsLink(userId, linkId)).isTrue();

        // And: We should be able to find the owner
        assertThat(linkRepository.getLinkOwner(linkId)).hasValue(userId);
    }

    @Test
    void shouldFindLinksUsingGraphTraversal() {
        // Given: Create a test user with multiple links
        String userId = UUID.randomUUID().toString();
        User testUser = new User(userId, "graphuser", "graph@example.com", "hashedPassword", "salt",
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), null, true, "Graph", "User", null);
        userRepository.save(testUser);

        // Create multiple links for the user
        List<Link> testLinks = List.of(
                new Link(UUID.randomUUID().toString(), "https://link1.com", "Link 1", "First link",
                        LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), "text/html"),
                new Link(UUID.randomUUID().toString(), "https://link2.com", "Link 2", "Second link",
                        LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), "text/html"),
                new Link(UUID.randomUUID().toString(), "https://link3.com", "Link 3", "Third link",
                        LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), "text/html")
        );

        // Save links with graph relationships
        testLinks.forEach(link -> linkRepository.saveLinkForUser(link, userId));

        // When: Find links using graph traversal
        List<Link> foundLinks = linkRepository.findLinksByUserId(userId);

        // Then: All user's links should be found
        assertThat(foundLinks).hasSize(3);
        assertThat(foundLinks.stream().map(Link::url))
                .containsExactlyInAnyOrder("https://link1.com", "https://link2.com", "https://link3.com");
    }

    @Test
    @Disabled
    void shouldTransferLinkOwnership() {
        // Given: Create two users
        String user1Id = UUID.randomUUID().toString();
        String user2Id = UUID.randomUUID().toString();

        User user1 = new User(user1Id, "user1", "user1@example.com", "hash1", "salt1",
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), null, true, "User", "One", null);
        User user2 = new User(user2Id, "user2", "user2@example.com", "hash2", "salt2",
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), null, true, "User", "Two", null);

        userRepository.save(user1);
        userRepository.save(user2);

        // And: Create a link owned by user1
        String linkId = UUID.randomUUID().toString();
        Link testLink = new Link(linkId, "https://transfer-test.com", "Transfer Test", "Testing ownership transfer",
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), "text/html");
        linkRepository.saveLinkForUser(testLink, user1Id);

        // Verify initial ownership
        assertThat(linkRepository.userOwnsLink(user1Id, linkId)).isTrue();
        assertThat(linkRepository.userOwnsLink(user2Id, linkId)).isFalse();

        // When: Transfer ownership to user2
        linkRepository.transferLinkOwnership(linkId, user1Id, user2Id);

        // Then: Ownership should be transferred
        assertThat(linkRepository.userOwnsLink(user1Id, linkId)).isFalse();
        assertThat(linkRepository.userOwnsLink(user2Id, linkId)).isTrue();
        assertThat(linkRepository.getLinkOwner(linkId)).hasValue(user2Id);
    }

    @Test
    void shouldDeleteLinkAndRelationships() {
        // Given: Create a user and link with relationship
        String userId = UUID.randomUUID().toString();
        User testUser = new User(userId, "deleteuser", "delete@example.com", "hashedPassword", "salt",
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), null, true, "Delete", "User", null);
        userRepository.save(testUser);

        String linkId = UUID.randomUUID().toString();
        Link testLink = new Link(linkId, "https://delete-test.com", "Delete Test", "Testing deletion",
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), "text/html");
        linkRepository.saveLinkForUser(testLink, userId);

        // Verify link exists and relationship exists
        assertThat(linkRepository.findLinkById(linkId)).isPresent();
        assertThat(linkRepository.userOwnsLink(userId, linkId)).isTrue();

        // When: Delete the link
        linkRepository.deleteLink(linkId);

        // Then: Link should be deleted and relationship should be gone
        assertThat(linkRepository.findLinkById(linkId)).isEmpty();
        assertThat(linkRepository.userOwnsLink(userId, linkId)).isFalse();
    }

    private void cleanupTestData() {
        try {
            // Clean up any existing test data
            database.command("sql", "DELETE FROM OwnsLink");
            database.command("sql", "DELETE FROM Link WHERE url LIKE 'https://%example.com%' OR url LIKE 'https://link%.com%' OR url LIKE 'https://transfer-test.com%' OR url LIKE 'https://delete-test.com%'");
            database.command("sql", "DELETE FROM User WHERE username LIKE '%testuser%' OR username LIKE '%graphuser%' OR username LIKE 'user1' OR username LIKE 'user2' OR username LIKE 'deleteuser'");
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}
