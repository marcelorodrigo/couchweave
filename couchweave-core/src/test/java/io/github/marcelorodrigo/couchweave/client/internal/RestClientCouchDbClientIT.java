package io.github.marcelorodrigo.couchweave.client.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import io.github.marcelorodrigo.couchweave.client.CouchOptimisticLockingFailureException;
import io.github.marcelorodrigo.couchweave.testsupport.couchdb.CouchDbIntegrationTest;
import io.github.marcelorodrigo.couchweave.testsupport.couchdb.CouchDbTestDatabase;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@CouchDbIntegrationTest
class RestClientCouchDbClientIT {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should complete the document CRUD lifecycle against CouchDB")
    void shouldCompleteTheDocumentCrudLifecycleAgainstCouchDb(CouchDbTestDatabase database) throws JacksonException {
        // given
        var client = client(database);
        var documentId = "book / café";
        var newDocument = objectMapper.readTree("""
                {"_id":"book / café","title":"CouchWeave"}
                """);

        // when
        var created = client.putDocument(database.databaseName(), documentId, newDocument);
        var foundAfterCreate = client.getDocument(database.databaseName(), documentId);
        var existsAfterCreate = client.documentExists(database.databaseName(), documentId);
        var updatedDocument = objectMapper.readTree("""
                {"_id":"book / café","_rev":"%s","title":"CouchWeave Updated"}
                """.formatted(created.revision()));
        var updated = client.putDocument(database.databaseName(), documentId, updatedDocument);
        var deleted = client.deleteDocument(database.databaseName(), documentId, updated.revision());
        var existsAfterDelete = client.documentExists(database.databaseName(), documentId);
        var foundAfterDelete = client.getDocument(database.databaseName(), documentId);

        // then
        assertThat(created.documentId()).isEqualTo(documentId);
        assertThat(foundAfterCreate).isPresent().get().satisfies(document -> {
            assertThat(document.get("_rev").stringValue()).isEqualTo(created.revision());
            assertThat(document.get("title").stringValue()).isEqualTo("CouchWeave");
        });
        assertThat(existsAfterCreate).isTrue();
        assertThat(updated.revision()).isNotEqualTo(created.revision());
        assertThat(deleted.documentId()).isEqualTo(documentId);
        assertThat(existsAfterDelete).isFalse();
        assertThat(foundAfterDelete).isEmpty();
    }

    @Test
    @DisplayName("should translate a stale document update against CouchDB")
    void shouldTranslateAStaleDocumentUpdateAgainstCouchDb(CouchDbTestDatabase database) throws JacksonException {
        // given
        var client = client(database);
        var documentId = "stale-book";
        var created = client.putDocument(
                database.databaseName(),
                documentId,
                objectMapper.readTree("{\"_id\":\"stale-book\",\"title\":\"First\"}"));
        var staleDocument = objectMapper.readTree("""
                {"_id":"stale-book","_rev":"%s","title":"Stale"}
                """.formatted(created.revision()));
        var currentDocument = objectMapper.readTree("""
                {"_id":"stale-book","_rev":"%s","title":"Current"}
                """.formatted(created.revision()));
        client.putDocument(database.databaseName(), documentId, currentDocument);
        var databaseName = database.databaseName();

        // when / then
        assertThatThrownBy(() -> client.putDocument(databaseName, documentId, staleDocument))
                .isInstanceOf(CouchOptimisticLockingFailureException.class)
                .hasMessageContaining(documentId)
                .hasMessageContaining(created.revision());
    }

    @Test
    @DisplayName("should fetch document bodies from all documents against CouchDB")
    void shouldFetchDocumentBodiesFromAllDocumentsAgainstCouchDb(CouchDbTestDatabase database) throws JacksonException {
        // given
        var client = client(database);
        client.putDocument(
                database.databaseName(), "book-1", objectMapper.readTree("{\"_id\":\"book-1\",\"title\":\"First\"}"));
        client.putDocument(
                database.databaseName(), "book-2", objectMapper.readTree("{\"_id\":\"book-2\",\"title\":\"Second\"}"));

        // when
        var documents = client.getAllDocuments(database.databaseName());

        // then
        assertThat(documents)
                .extracting(document -> document.get("title").stringValue())
                .containsExactly("First", "Second");
    }

    private RestClientCouchDbClient client(CouchDbTestDatabase database) {
        return new RestClientCouchDbClient(new CouchDbClientSettings(
                database.serverUri(),
                database.databaseName(),
                database.username(),
                database.password(),
                Duration.ofSeconds(5),
                Duration.ofSeconds(5)));
    }
}
