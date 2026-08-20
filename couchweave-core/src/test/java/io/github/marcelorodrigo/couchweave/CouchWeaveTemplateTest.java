package io.github.marcelorodrigo.couchweave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.marcelorodrigo.couchweave.client.CouchOptimisticLockingFailureException;
import io.github.marcelorodrigo.couchweave.client.internal.CouchDbClient;
import io.github.marcelorodrigo.couchweave.client.internal.CouchDbWriteResult;
import io.github.marcelorodrigo.couchweave.mapping.CouchDocument;
import io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext;
import io.github.marcelorodrigo.couchweave.mapping.CouchWeaveConverter;
import io.github.marcelorodrigo.couchweave.mapping.Revision;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.MappingException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
class CouchWeaveTemplateTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CouchDbClient client;

    @Mock
    private CouchWeaveConverter converter;

    private CouchWeaveTemplate template;

    @BeforeEach
    void setUp() {
        var context = new CouchMappingContext("default-db");
        context.setInitialEntitySet(Set.of(Book.class, NoRevisionBook.class));
        context.initialize();
        // The blank-ID validation test returns before reading mapping metadata.
        lenient().when(converter.getMappingContext()).thenReturn(context);
        template = new CouchWeaveTemplate(client, converter);
    }

    @Test
    @DisplayName("should save an entity using its mapped database and server metadata")
    void shouldSaveAnEntityUsingItsMappedDatabaseAndServerMetadata() {
        // given
        var source = new Book(null, null, "CouchWeave");
        var document = document("generated-id", null);
        var saved = new Book("generated-id", "1-created", "CouchWeave");
        when(converter.write(source)).thenReturn(document);
        when(client.putDocument("archive", "generated-id", document))
                .thenReturn(new CouchDbWriteResult("generated-id", "1-created"));
        when(converter.read(Book.class, document)).thenReturn(saved);

        // when
        var result = template.save(source);

        // then
        assertThat(result).isEqualTo(saved);
        assertThat(document.get("_rev").stringValue()).isEqualTo("1-created");
        verify(converter).read(Book.class, document);
    }

    @Test
    @DisplayName("should patch a server-assigned identifier before reading the saved entity")
    void shouldPatchAServerAssignedIdentifierBeforeReadingTheSavedEntity() {
        // given
        var source = new Book("client-id", "1-current", "CouchWeave");
        var document = document("client-id", "1-current");
        var saved = new Book("server-id", "2-updated", "CouchWeave");
        when(converter.write(source)).thenReturn(document);
        when(client.putDocument("archive", "client-id", document))
                .thenReturn(new CouchDbWriteResult("server-id", "2-updated"));
        when(converter.read(Book.class, document)).thenReturn(saved);

        // when
        template.save(source);

        // then
        assertThat(document.get("_id").stringValue()).isEqualTo("server-id");
        assertThat(document.get("_rev").stringValue()).isEqualTo("2-updated");
    }

    @Test
    @DisplayName("should find a document in the mapped database")
    void shouldFindADocumentInTheMappedDatabase() {
        // given
        var document = document("book-1", "1-created");
        var expected = new Book("book-1", "1-created", "CouchWeave");
        when(client.getDocument("archive", "book-1")).thenReturn(Optional.of(document));
        when(converter.read(Book.class, document)).thenReturn(expected);

        // when
        var result = template.findById("book-1", Book.class);

        // then
        assertThat(result).contains(expected);
    }

    @Test
    @DisplayName("should find only documents with the exact entity discriminator")
    void shouldFindOnlyDocumentsWithTheExactEntityDiscriminator() {
        // given
        var matching = document("book-1", null);
        var otherType = document("novel-1", null);
        otherType.put("couchweave_type", "novel");
        var design = document("_design/books", null);
        when(client.getAllDocuments("archive")).thenReturn(List.of(matching, otherType, design));
        var expected = new Book("book-1", null, "CouchWeave");
        when(converter.read(Book.class, matching)).thenReturn(expected);

        // when
        var result = template.findAll(Book.class);

        // then
        assertThat(result).containsExactly(expected);
        verify(converter).read(Book.class, matching);
    }

    @Test
    @DisplayName("should preserve the all documents response order")
    void shouldPreserveTheAllDocumentsResponseOrder() {
        // given
        var first = document("book-1", null);
        var second = document("book-2", null);
        when(client.getAllDocuments("archive")).thenReturn(List.of(first, second));
        var firstEntity = new Book("book-1", null, "First");
        var secondEntity = new Book("book-2", null, "Second");
        when(converter.read(Book.class, first)).thenReturn(firstEntity);
        when(converter.read(Book.class, second)).thenReturn(secondEntity);

        // when
        var result = template.findAll(Book.class);

        // then
        assertThat(result).containsExactly(firstEntity, secondEntity);
    }

    @Test
    @DisplayName("should propagate a malformed matching document failure")
    void shouldPropagateAMalformedMatchingDocumentFailure() {
        // given
        var matching = document("book-1", null);
        var failure = new IllegalStateException("malformed document");
        when(client.getAllDocuments("archive")).thenReturn(List.of(matching));
        when(converter.read(Book.class, matching)).thenThrow(failure);

        // when
        var action = (org.assertj.core.api.ThrowableAssert.ThrowingCallable) () -> template.findAll(Book.class);

        // then
        assertThatThrownBy(action).isSameAs(failure);
    }

    @Test
    @DisplayName("should reject an unmapped entity type when finding all")
    void shouldRejectAnUnmappedEntityTypeWhenFindingAll() {
        // given

        // when
        var action = (org.assertj.core.api.ThrowableAssert.ThrowingCallable) () -> template.findAll(Unmapped.class);

        // then
        assertThatThrownBy(action).isInstanceOf(MappingException.class);
    }

    @Test
    @DisplayName("should count only documents with the exact entity discriminator")
    void shouldCountOnlyDocumentsWithTheExactEntityDiscriminator() {
        // given
        var matching = document("book-1", null);
        var otherType = document("novel-1", null);
        otherType.put("couchweave_type", "novel");
        var design = document("_design/books", null);
        when(client.getAllDocuments("archive")).thenReturn(List.of(matching, otherType, design));
        when(converter.read(Book.class, matching)).thenReturn(new Book("book-1", null, "CouchWeave"));

        // when
        var result = template.count(Book.class);

        // then
        assertThat(result).isEqualTo(1L);
    }

    @Test
    @DisplayName("should propagate a malformed matching document count failure")
    void shouldPropagateAMalformedMatchingDocumentCountFailure() {
        // given
        var matching = document("book-1", null);
        var failure = new IllegalStateException("malformed document");
        when(client.getAllDocuments("archive")).thenReturn(List.of(matching));
        when(converter.read(Book.class, matching)).thenThrow(failure);

        // when
        var action = (org.assertj.core.api.ThrowableAssert.ThrowingCallable) () -> template.count(Book.class);

        // then
        assertThatThrownBy(action).isSameAs(failure);
    }

    @Test
    @DisplayName("should route find all to the entity database")
    void shouldRouteFindAllToTheEntityDatabase() {
        // given
        when(client.getAllDocuments("archive")).thenReturn(List.of());

        // when
        template.findAll(Book.class);

        // then
        verify(client).getAllDocuments("archive");
    }

    @Test
    @DisplayName("should route count to the entity database")
    void shouldRouteCountToTheEntityDatabase() {
        // given
        when(client.getAllDocuments("archive")).thenReturn(List.of());

        // when
        template.count(Book.class);

        // then
        verify(client).getAllDocuments("archive");
    }

    @Test
    @DisplayName("should return an empty result when a document is missing")
    void shouldReturnAnEmptyResultWhenADocumentIsMissing() {
        // given
        when(client.getDocument("archive", "missing")).thenReturn(Optional.empty());

        // when
        var result = template.findById("missing", Book.class);

        // then
        assertThat(result).isEmpty();
        verify(converter, never()).read(any(), any());
    }

    @Test
    @DisplayName("should check existence in the mapped database")
    void shouldCheckExistenceInTheMappedDatabase() {
        // given
        when(client.documentExists("archive", "book-1")).thenReturn(true);

        // when
        var result = template.existsById("book-1", Book.class);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should report that a missing document does not exist")
    void shouldReportThatAMissingDocumentDoesNotExist() {
        // given
        when(client.documentExists("archive", "missing")).thenReturn(false);

        // when
        var result = template.existsById("missing", Book.class);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should delete an entity using its mapped revision")
    void shouldDeleteAnEntityUsingItsMappedRevision() {
        // given
        var entity = new Book("book-1", "2-current", "CouchWeave");

        // when
        template.delete(entity);

        // then
        verify(client).deleteDocument("archive", "book-1", "2-current");
    }

    @Test
    @DisplayName("should reject entity deletion without a revision")
    void shouldRejectEntityDeletionWithoutARevision() {
        // given
        var entity = new Book("book-1", null, "CouchWeave");

        // when / then
        assertThatThrownBy(() -> template.delete(entity))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("revision");
        verify(client, never()).deleteDocument(any(), any(), any());
    }

    @Test
    @DisplayName("should reject entity deletion without an identifier")
    void shouldRejectEntityDeletionWithoutAnIdentifier() {
        // given
        var entity = new Book(null, "2-current", "CouchWeave");

        // when / then
        assertThatThrownBy(() -> template.delete(entity))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("ID");
        verify(client, never()).deleteDocument(any(), any(), any());
    }

    @Test
    @DisplayName("should reject entity deletion for a type without a revision property")
    void shouldRejectEntityDeletionForATypeWithoutARevisionProperty() {
        // given
        var entity = new NoRevisionBook("book-1", "CouchWeave");

        // when / then
        assertThatThrownBy(() -> template.delete(entity))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("revision");
        verify(client, never()).deleteDocument(any(), any(), any());
    }

    @Test
    @DisplayName("should fetch the current revision before deleting by identifier")
    void shouldFetchTheCurrentRevisionBeforeDeletingByIdentifier() {
        // given
        var document = document("book-1", "3-current");
        when(client.getDocument("archive", "book-1")).thenReturn(Optional.of(document));

        // when
        template.deleteById("book-1", Book.class);

        // then
        verify(client).deleteDocument("archive", "book-1", "3-current");
    }

    @Test
    @DisplayName("should make deleting a missing identifier a no-op")
    void shouldMakeDeletingAMissingIdentifierANoOp() {
        // given
        when(client.getDocument("archive", "missing")).thenReturn(Optional.empty());

        // when
        template.deleteById("missing", Book.class);

        // then
        verify(client, never()).deleteDocument(any(), any(), any());
    }

    @Test
    @DisplayName("should reject a blank identifier")
    void shouldRejectABlankIdentifier() {
        // given

        // when / then
        assertThatThrownBy(() -> template.findById(" ", Book.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }

    @Test
    @DisplayName("should create an assigned-identifier document when it does not yet exist")
    void shouldCreateAnAssignedIdentifierDocumentWhenItDoesNotExist() {
        // given
        var source = new Book("book-new", null, "CouchWeave");
        var document = document("book-new", null);
        var saved = new Book("book-new", "1-created", "CouchWeave");
        when(client.getDocument("archive", "book-new")).thenReturn(Optional.empty());
        when(converter.write(source)).thenReturn(document);
        when(client.putDocument("archive", "book-new", document))
                .thenReturn(new CouchDbWriteResult("book-new", "1-created"));
        when(converter.read(Book.class, document)).thenReturn(saved);

        // when
        var result = template.save(source);

        // then
        assertThat(result).isEqualTo(saved);
        verify(client).getDocument("archive", "book-new");
        verify(client).putDocument("archive", "book-new", document);
    }

    @Test
    @DisplayName("should reject saving an existing document without a revision")
    void shouldRejectSavingAnExistingDocumentWithoutARevision() {
        // given
        var source = new Book("book-exists", null, "CouchWeave");
        when(client.getDocument("archive", "book-exists"))
                .thenReturn(Optional.of(document("book-exists", "1-current")));

        // when / then
        assertThatThrownBy(() -> template.save(source))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("revision");
        verify(client, never()).putDocument(any(), any(), any());
    }

    @Test
    @DisplayName("should reject saving a revisionless type that already exists")
    void shouldRejectSavingARevisionlessTypeThatAlreadyExists() {
        // given
        var source = new NoRevisionBook("book-exists", "CouchWeave");
        when(client.getDocument("default-db", "book-exists"))
                .thenReturn(Optional.of(document("book-exists", "1-current")));

        // when / then
        assertThatThrownBy(() -> template.save(source))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("revision");
        verify(client, never()).putDocument(any(), any(), any());
    }

    @Test
    @DisplayName("should reject saving a revision without a usable identifier")
    void shouldRejectSavingARevisionWithoutAUsableIdentifier() {
        // given
        var source = new Book(null, "2-current", "CouchWeave");

        // when / then
        assertThatThrownBy(() -> template.save(source))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("ID");
        verify(client, never()).getDocument(any(), any());
        verify(client, never()).putDocument(any(), any(), any());
    }

    @Test
    @DisplayName("should expose the entity type on a stale save conflict")
    void shouldExposeTheEntityTypeOnAStaleSaveConflict() {
        // given
        var source = new Book("book-1", "2-current", "CouchWeave");
        var document = document("book-1", "2-current");
        var conflict = new CouchOptimisticLockingFailureException("conflict", "archive", "book-1", "2-current", null);
        when(converter.write(source)).thenReturn(document);
        when(client.putDocument("archive", "book-1", document)).thenThrow(conflict);

        // when / then
        assertThatThrownBy(() -> template.save(source))
                .isInstanceOfSatisfying(CouchOptimisticLockingFailureException.class, failure -> {
                    assertThat(failure.entityType()).isEqualTo(Book.class);
                    assertThat(failure.database()).isEqualTo("archive");
                    assertThat(failure.documentId()).isEqualTo("book-1");
                    assertThat(failure.revision()).isEqualTo("2-current");
                });
        verify(client, times(1)).putDocument("archive", "book-1", document);
    }

    @Test
    @DisplayName("should expose the entity type on a stale entity delete conflict")
    void shouldExposeTheEntityTypeOnAStaleEntityDeleteConflict() {
        // given
        var entity = new Book("book-1", "2-current", "CouchWeave");
        var conflict = new CouchOptimisticLockingFailureException("conflict", "archive", "book-1", "2-current", null);
        when(client.deleteDocument("archive", "book-1", "2-current")).thenThrow(conflict);

        // when / then
        assertThatThrownBy(() -> template.delete(entity))
                .isInstanceOfSatisfying(CouchOptimisticLockingFailureException.class, failure -> {
                    assertThat(failure.entityType()).isEqualTo(Book.class);
                    assertThat(failure.database()).isEqualTo("archive");
                    assertThat(failure.documentId()).isEqualTo("book-1");
                    assertThat(failure.revision()).isEqualTo("2-current");
                });
        verify(client, times(1)).deleteDocument("archive", "book-1", "2-current");
    }

    private ObjectNode document(String id, String revision) {
        var document = objectMapper.createObjectNode();
        document.put("_id", id);
        if (revision != null) {
            document.put("_rev", revision);
        }
        document.put("couchweave_type", "book");
        return document;
    }

    @CouchDocument(type = "book", database = "archive")
    static class Book {
        @Id
        String id;

        @Revision
        String revision;

        String title;

        Book(String id, String revision, String title) {
            this.id = id;
            this.revision = revision;
            this.title = title;
        }
    }

    static class Unmapped {}

    @CouchDocument(type = "no-revision")
    record NoRevisionBook(@Id String id, String title) {}
}
