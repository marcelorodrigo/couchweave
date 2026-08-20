package io.github.marcelorodrigo.couchweave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import io.github.marcelorodrigo.couchweave.client.CouchOptimisticLockingFailureException;
import io.github.marcelorodrigo.couchweave.mapping.CouchDocument;
import io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext;
import io.github.marcelorodrigo.couchweave.mapping.CouchWeaveCustomConversions;
import io.github.marcelorodrigo.couchweave.mapping.MappingCouchWeaveConverter;
import io.github.marcelorodrigo.couchweave.mapping.Revision;
import io.github.marcelorodrigo.couchweave.testsupport.couchdb.CouchDbIntegrationTest;
import io.github.marcelorodrigo.couchweave.testsupport.couchdb.CouchDbTestDatabase;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import tools.jackson.databind.ObjectMapper;

@CouchDbIntegrationTest
class CouchWeaveTemplateIT {

    private static final String OVERRIDE_DATABASE = "couchweave-template-override";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void emptyDatabase(CouchDbTestDatabase database) {
        var template = template(database, MutableBook.class, ImmutableBook.class);
        for (var book : template.findAll(MutableBook.class)) {
            template.delete(book);
        }
        for (var book : template.findAll(ImmutableBook.class)) {
            template.delete(book);
        }
    }

    @Test
    @DisplayName("should round trip generated identifiers and revisions against CouchDB")
    void shouldRoundTripGeneratedIdentifiersAndRevisionsAgainstCouchDb(CouchDbTestDatabase database) {
        // given
        var template = template(database, MutableBook.class, ImmutableBook.class, OverrideBook.class);
        var source = new MutableBook(null, null, "CouchWeave");

        // when
        var saved = template.save(source);
        var found = template.findById(saved.id, MutableBook.class);

        // then
        assertThat(source.id).isNull();
        assertThat(saved.id).isNotBlank();
        assertThat(saved.revision).isNotBlank();
        assertThat(found).isPresent().get().satisfies(value -> {
            assertThat(value.id).isEqualTo(saved.id);
            assertThat(value.revision).isEqualTo(saved.revision);
            assertThat(value.title).isEqualTo(saved.title);
        });
    }

    @Test
    @DisplayName("should reconstruct an immutable entity with an assigned identifier")
    void shouldReconstructAnImmutableEntityWithAnAssignedIdentifier(CouchDbTestDatabase database) {
        // given
        var template = template(database, MutableBook.class, ImmutableBook.class, OverrideBook.class);
        var source = new ImmutableBook("immutable-" + UUID.randomUUID(), null, "Kindred");

        // when
        var saved = template.save(source);

        // then
        assertThat(saved.id()).isEqualTo(source.id());
        assertThat(saved.revision()).isNotBlank();
        assertThat(saved.title()).isEqualTo(source.title());
    }

    @Test
    @DisplayName("should support existence and revision-aware deletion against CouchDB")
    void shouldSupportExistenceAndRevisionAwareDeletionAgainstCouchDb(CouchDbTestDatabase database) {
        // given
        var template = template(database, MutableBook.class, ImmutableBook.class, OverrideBook.class);
        var saved = template.save(new MutableBook("delete-" + UUID.randomUUID(), null, "Delete me"));

        // when
        var existsBeforeDelete = template.existsById(saved.id, MutableBook.class);
        template.delete(saved);
        var existsAfterDelete = template.existsById(saved.id, MutableBook.class);

        // then
        assertThat(existsBeforeDelete).isTrue();
        assertThat(existsAfterDelete).isFalse();
    }

    @Test
    @DisplayName("should make missing document operations predictable against CouchDB")
    void shouldMakeMissingDocumentOperationsPredictableAgainstCouchDb(CouchDbTestDatabase database) {
        // given
        var template = template(database, MutableBook.class, ImmutableBook.class, OverrideBook.class);
        var missingId = "missing-" + UUID.randomUUID();

        // when
        var found = template.findById(missingId, MutableBook.class);
        var exists = template.existsById(missingId, MutableBook.class);
        template.deleteById(missingId, MutableBook.class);

        // then
        assertThat(found).isEmpty();
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("should find all filtering unrelated discriminator types")
    void shouldFindAllFilteringUnrelatedDiscriminatorTypes(CouchDbTestDatabase database) {
        // given
        var template = template(database, MutableBook.class, ImmutableBook.class);
        var book = template.save(new MutableBook("filter-book-" + UUID.randomUUID(), null, "Book"));
        template.save(new ImmutableBook("filter-immutable-" + UUID.randomUUID(), null, "Immutable"));

        // when
        var books = template.findAll(MutableBook.class);

        // then
        assertThat(books).containsExactly(book);
        assertThat(template.count(MutableBook.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("should count only matching documents")
    void shouldCountOnlyMatchingDocuments(CouchDbTestDatabase database) {
        // given
        var template = template(database, MutableBook.class, ImmutableBook.class);
        template.save(new MutableBook("count-book-" + UUID.randomUUID(), null, "First"));
        template.save(new MutableBook("count-book-" + UUID.randomUUID(), null, "Second"));
        template.save(new ImmutableBook("count-immutable-" + UUID.randomUUID(), null, "Other"));

        // when
        var count = template.count(MutableBook.class);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("should route an annotated entity to its override database")
    void shouldRouteAnAnnotatedEntityToItsOverrideDatabase(CouchDbTestDatabase database) throws Exception {
        // given
        createDatabase(database, OVERRIDE_DATABASE);
        try {
            var template = template(database, MutableBook.class, ImmutableBook.class, OverrideBook.class);
            var source = new OverrideBook("override-" + UUID.randomUUID(), null, "Archive me");

            // when
            var saved = template.save(source);
            var found = template.findById(saved.id, OverrideBook.class);

            // then
            assertThat(found).contains(saved);
        } finally {
            deleteDatabase(database, OVERRIDE_DATABASE);
        }
    }

    @Test
    @DisplayName("should fail a stale save with an optimistic-locking conflict and leave the winning update intact")
    void shouldFailAStaleSaveWithAnOptimisticLockingConflict(CouchDbTestDatabase database) {
        // given
        var template = template(database, MutableBook.class, ImmutableBook.class, OverrideBook.class);
        var created = template.save(new MutableBook("stale-" + UUID.randomUUID(), null, "original"));
        var winner = template.findById(created.id, MutableBook.class).orElseThrow();
        var stale = template.findById(created.id, MutableBook.class).orElseThrow();

        // when
        winner.title = "updated";
        template.save(winner);

        // then
        assertThatThrownBy(() -> template.save(stale))
                .isInstanceOfSatisfying(CouchOptimisticLockingFailureException.class, failure -> {
                    assertThat(failure.entityType()).isEqualTo(MutableBook.class);
                    assertThat(failure.documentId()).isEqualTo(created.id);
                    assertThat(failure.revision()).isEqualTo(stale.revision);
                });
        var reloaded = template.findById(created.id, MutableBook.class).orElseThrow();
        assertThat(reloaded.title).isEqualTo("updated");
    }

    @Test
    @DisplayName("should fail a stale entity delete with an optimistic-locking conflict and leave the document intact")
    void shouldFailAStaleEntityDeleteWithAnOptimisticLockingConflict(CouchDbTestDatabase database) {
        // given
        var template = template(database, MutableBook.class, ImmutableBook.class, OverrideBook.class);
        var created = template.save(new MutableBook("delete-" + UUID.randomUUID(), null, "original"));
        var winner = template.findById(created.id, MutableBook.class).orElseThrow();
        var stale = template.findById(created.id, MutableBook.class).orElseThrow();

        // when
        winner.title = "changed";
        template.save(winner);

        // then
        assertThatThrownBy(() -> template.delete(stale))
                .isInstanceOfSatisfying(CouchOptimisticLockingFailureException.class, failure -> {
                    assertThat(failure.entityType()).isEqualTo(MutableBook.class);
                    assertThat(failure.documentId()).isEqualTo(created.id);
                    assertThat(failure.revision()).isEqualTo(stale.revision);
                });
        assertThat(template.existsById(created.id, MutableBook.class)).isTrue();
        var reloaded = template.findById(created.id, MutableBook.class).orElseThrow();
        assertThat(reloaded.title).isEqualTo("changed");
    }

    private CouchWeaveTemplate template(CouchDbTestDatabase database, Class<?>... entityTypes) {
        var context = new CouchMappingContext(database.databaseName());
        context.setInitialEntitySet(Set.of(entityTypes));
        context.initialize();
        var converter =
                new MappingCouchWeaveConverter(context, objectMapper, new CouchWeaveCustomConversions(List.of()));
        var settings = new CouchDbClientSettings(
                database.serverUri(),
                database.databaseName(),
                database.username(),
                database.password(),
                Duration.ofSeconds(5),
                Duration.ofSeconds(5));
        return new CouchWeaveTemplate(settings, converter);
    }

    private void createDatabase(CouchDbTestDatabase database, String databaseName)
            throws IOException, InterruptedException {
        var response = send(database, databaseName, HttpRequest.BodyPublishers.noBody(), "PUT");
        assertThat(response.statusCode()).isIn(201, 412);
    }

    private void deleteDatabase(CouchDbTestDatabase database, String databaseName)
            throws IOException, InterruptedException {
        var response = send(database, databaseName, HttpRequest.BodyPublishers.noBody(), "DELETE");
        assertThat(response.statusCode()).isIn(200, 202, 404);
    }

    private HttpResponse<String> send(
            CouchDbTestDatabase database, String databaseName, HttpRequest.BodyPublisher body, String method)
            throws IOException, InterruptedException {
        var uri = database.serverUri().resolve("/" + databaseName);
        var request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", basicAuthentication(database))
                .method(method, body)
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String basicAuthentication(CouchDbTestDatabase database) {
        var credentials = database.username() + ":" + database.password();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    @CouchDocument(type = "it-book")
    static class MutableBook {
        @Id
        String id;

        @Revision
        String revision;

        String title;

        MutableBook() {}

        MutableBook(String id, String revision, String title) {
            this.id = id;
            this.revision = revision;
            this.title = title;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof MutableBook book
                    && Objects.equals(id, book.id)
                    && Objects.equals(revision, book.revision)
                    && Objects.equals(title, book.title);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, revision, title);
        }
    }

    @CouchDocument(type = "it-immutable-book")
    record ImmutableBook(@Id String id, @Revision String revision, String title) {}

    @CouchDocument(type = "it-override-book", database = OVERRIDE_DATABASE)
    record OverrideBook(@Id String id, @Revision String revision, String title) {}
}
