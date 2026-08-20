package io.github.marcelorodrigo.couchweave.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.marcelorodrigo.couchweave.CouchWeaveTemplate;
import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import io.github.marcelorodrigo.couchweave.client.CouchOptimisticLockingFailureException;
import io.github.marcelorodrigo.couchweave.mapping.CouchDocument;
import io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext;
import io.github.marcelorodrigo.couchweave.mapping.CouchWeaveCustomConversions;
import io.github.marcelorodrigo.couchweave.mapping.MappingCouchWeaveConverter;
import io.github.marcelorodrigo.couchweave.mapping.Revision;
import io.github.marcelorodrigo.couchweave.repository.support.CouchWeaveRepositoryFactory;
import io.github.marcelorodrigo.couchweave.testsupport.couchdb.CouchDbIntegrationTest;
import io.github.marcelorodrigo.couchweave.testsupport.couchdb.CouchDbTestDatabase;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.annotation.Id;
import tools.jackson.databind.ObjectMapper;

@CouchDbIntegrationTest
class CouchWeaveRepositoryIT {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should save generated ID and find by ID")
    void shouldSaveGeneratedIdAndFindById(CouchDbTestDatabase database) {
        // given
        var repository = repository(database);
        var source = new MutableBook(null, null, "Generated");

        // when
        var saved = repository.save(source);
        var found = repository.findById(saved.id);

        // then
        assertThat(saved.id).isNotBlank();
        assertThat(saved.revision).isNotBlank();
        assertThat(found).contains(saved);
    }

    @Test
    @DisplayName("should create with assigned ID")
    void shouldCreateWithAssignedId(CouchDbTestDatabase database) {
        // given
        var repository = repository(database);
        var source = new MutableBook("assigned-" + UUID.randomUUID(), null, "Assigned");

        // when
        var saved = repository.save(source);

        // then
        assertThat(repository.findById(source.id)).contains(saved);
        assertThat(saved.revision).isNotBlank();
    }

    @Test
    @DisplayName("should reject replacing an existing entity without a revision")
    void shouldRejectReplacingExistingEntityWithoutRevision(CouchDbTestDatabase database) {
        // given
        var repository = repository(database);
        var id = "replace-" + UUID.randomUUID();
        repository.save(new MutableBook(id, null, "Original"));

        // when / then
        assertThatThrownBy(() -> repository.save(new MutableBook(id, null, "Replacement")))
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
    }

    @Test
    @DisplayName("should support exists and delete by ID")
    void shouldSupportExistsAndDeleteById(CouchDbTestDatabase database) {
        // given
        var repository = repository(database);
        var saved = repository.save(new MutableBook("delete-id-" + UUID.randomUUID(), null, "Delete"));

        // when
        repository.deleteById(saved.id);

        // then
        assertThat(repository.existsById(saved.id)).isFalse();
    }

    @Test
    @DisplayName("should delete an entity using its revision")
    void shouldDeleteEntityUsingItsRevision(CouchDbTestDatabase database) {
        // given
        var repository = repository(database);
        var saved = repository.save(new MutableBook("delete-entity-" + UUID.randomUUID(), null, "Delete"));

        // when
        repository.delete(saved);

        // then
        assertThat(repository.existsById(saved.id)).isFalse();
    }

    @Test
    @DisplayName("should find all returning only matching discriminator")
    void shouldFindAllReturningOnlyMatchingDiscriminator(CouchDbTestDatabase database) {
        // given
        var repository = repository(database);
        var first = repository.save(new MutableBook("all-book-" + UUID.randomUUID(), null, "First"));
        var second = repository.save(new MutableBook("all-book-" + UUID.randomUUID(), null, "Second"));
        repositoryOperations(database).save(new Note("all-note-" + UUID.randomUUID(), null, "Note"));

        // when
        var books = repository.findAll();

        // then
        assertThat(books).containsExactly(first, second);
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("should find all by ID preserving order and skipping missing")
    void shouldFindAllByIdPreservingOrderAndSkippingMissing(CouchDbTestDatabase database) {
        // given
        var repository = repository(database);
        var a = repository.save(new MutableBook("order-a-" + UUID.randomUUID(), null, "A"));
        var b = repository.save(new MutableBook("order-b-" + UUID.randomUUID(), null, "B"));

        // when
        var result = repository.findAllById(List.of(b.id, "missing-" + UUID.randomUUID(), a.id, b.id));

        // then
        assertThat(result).containsExactly(b, a, b);
    }

    @Test
    @DisplayName("should save all sequentially in order")
    void shouldSaveAllSequentiallyInOrder(CouchDbTestDatabase database) {
        // given
        var repository = repository(database);
        var books = List.of(
                new MutableBook(null, null, "First"),
                new MutableBook(null, null, "Second"),
                new MutableBook(null, null, "Third"));

        // when
        var saved = repository.saveAll(books);

        // then
        assertThat(saved).extracting(book -> book.title).containsExactly("First", "Second", "Third");
        assertThat(saved).allSatisfy(book -> {
            assertThat(book.id).isNotBlank();
            assertThat(book.revision).isNotBlank();
            assertThat(repository.existsById(book.id)).isTrue();
        });
    }

    @Test
    @DisplayName("should leave earlier save all writes when a later one fails")
    void shouldLeaveEarlierSaveAllWritesWhenLaterOneFails(CouchDbTestDatabase database) {
        // given
        var repository = repository(database);
        var blockerId = "save-all-blocker-" + UUID.randomUUID();
        repository.save(new MutableBook(blockerId, null, "Blocker"));
        var first = new MutableBook("save-all-first-" + UUID.randomUUID(), null, "First");
        var second = new MutableBook("save-all-second-" + UUID.randomUUID(), null, "Second");
        var failing = new MutableBook(blockerId, null, "Failing");

        // when / then
        assertThatThrownBy(() -> repository.saveAll(List.of(first, second, failing)))
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
        assertThat(repository.findById(first.id)).isPresent();
        assertThat(repository.findById(second.id)).isPresent();
    }

    @Test
    @DisplayName("should fail stale save with an enriched optimistic locking conflict")
    void shouldFailStaleSaveWithEnrichedOptimisticLockingConflict(CouchDbTestDatabase database) {
        // given
        var repository = repository(database);
        var created = repository.save(new MutableBook("stale-save-" + UUID.randomUUID(), null, "Original"));
        var winner = repository.findById(created.id).orElseThrow();
        var stale = repository.findById(created.id).orElseThrow();
        winner.title = "Winner";
        repository.save(winner);

        // when / then
        assertThatThrownBy(() -> repository.save(stale))
                .isInstanceOfSatisfying(CouchOptimisticLockingFailureException.class, failure -> {
                    assertThat(failure.entityType()).isEqualTo(MutableBook.class);
                    assertThat(failure.documentId()).isEqualTo(created.id);
                });
        assertThat(repository.findById(created.id).orElseThrow().title).isEqualTo("Winner");
    }

    @Test
    @DisplayName("should fail stale entity delete and leave the document intact")
    void shouldFailStaleEntityDeleteAndLeaveDocumentIntact(CouchDbTestDatabase database) {
        // given
        var repository = repository(database);
        var created = repository.save(new MutableBook("stale-delete-" + UUID.randomUUID(), null, "Original"));
        var winner = repository.findById(created.id).orElseThrow();
        var stale = repository.findById(created.id).orElseThrow();
        winner.title = "Winner";
        repository.save(winner);

        // when / then
        assertThatThrownBy(() -> repository.delete(stale))
                .isInstanceOfSatisfying(CouchOptimisticLockingFailureException.class, failure -> {
                    assertThat(failure.entityType()).isEqualTo(MutableBook.class);
                    assertThat(failure.documentId()).isEqualTo(created.id);
                });
        assertThat(repository.existsById(created.id)).isTrue();
        assertThat(repository.findById(created.id).orElseThrow().title).isEqualTo("Winner");
    }

    @Test
    @DisplayName("should delete by ID using the current revision")
    void shouldDeleteByIdUsingCurrentRevision(CouchDbTestDatabase database) {
        // given
        var repository = repository(database);
        var created = repository.save(new MutableBook("current-delete-" + UUID.randomUUID(), null, "Original"));
        var stale = repository.findById(created.id).orElseThrow();
        var current = repository.findById(created.id).orElseThrow();
        current.title = "Updated";
        repository.save(current);

        // when
        repository.deleteById(stale.id);

        // then
        assertThat(repository.existsById(created.id)).isFalse();
    }

    @Test
    @DisplayName("should delete all sequentially")
    void shouldDeleteAllSequentially(CouchDbTestDatabase database) {
        // given
        var repository = repository(database);
        repository.saveAll(List.of(
                new MutableBook("delete-all-" + UUID.randomUUID(), null, "First"),
                new MutableBook("delete-all-" + UUID.randomUUID(), null, "Second"),
                new MutableBook("delete-all-" + UUID.randomUUID(), null, "Third")));

        // when
        repository.deleteAll();

        // then
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("should stop collection delete on the first failure")
    void shouldStopCollectionDeleteOnFirstFailure(CouchDbTestDatabase database) {
        // given
        var repository = repository(database);
        var valid = repository.save(new MutableBook("delete-first-" + UUID.randomUUID(), null, "Valid"));
        var missing = new MutableBook("delete-missing-" + UUID.randomUUID(), "1-missing", "Missing");

        // when / then
        assertThatThrownBy(() -> repository.deleteAll(List.of(valid, missing)))
                .isInstanceOf(CouchOptimisticLockingFailureException.class);
        assertThat(repository.existsById(valid.id)).isFalse();
    }

    private CouchWeaveRepository<MutableBook, String> repository(CouchDbTestDatabase database) {
        return new CouchWeaveRepositoryFactory(repositoryOperations(database), mappingContext(database))
                .getRepository(BookRepository.class);
    }

    private CouchWeaveTemplate repositoryOperations(CouchDbTestDatabase database) {
        var context = mappingContext(database);
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

    private CouchMappingContext mappingContext(CouchDbTestDatabase database) {
        var context = new CouchMappingContext(database.databaseName());
        context.setInitialEntitySet(Set.of(MutableBook.class, Note.class));
        context.initialize();
        return context;
    }

    interface BookRepository extends CouchWeaveRepository<MutableBook, String> {}

    @CouchDocument(type = "it-repository-book")
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
    }

    @CouchDocument(type = "it-note")
    record Note(@Id String id, @Revision String revision, String body) {}
}
