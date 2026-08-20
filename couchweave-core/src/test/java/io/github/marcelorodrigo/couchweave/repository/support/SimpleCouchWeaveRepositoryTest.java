package io.github.marcelorodrigo.couchweave.repository.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.marcelorodrigo.couchweave.CouchWeaveOperations;
import io.github.marcelorodrigo.couchweave.client.CouchOptimisticLockingFailureException;
import io.github.marcelorodrigo.couchweave.mapping.CouchDocument;
import io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext;
import io.github.marcelorodrigo.couchweave.mapping.CouchPersistentEntity;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.annotation.Id;

@ExtendWith(MockitoExtension.class)
class SimpleCouchWeaveRepositoryTest {
    @Mock
    CouchWeaveOperations operations;

    private SimpleCouchWeaveRepository<Person, String> repository;

    @BeforeEach
    void setUp() {
        var context = new CouchMappingContext("default-db");
        context.setInitialEntitySet(java.util.Set.of(Person.class));
        context.initialize();
        repository = new SimpleCouchWeaveRepository<>(operations, new CouchWeaveEntityInformation<>(entity(context)));
    }

    @Test
    @DisplayName("should delegate save to operations")
    void shouldDelegateSaveToOperations() {
        // given
        var person = new Person("p1");
        when(operations.save(person)).thenReturn(person);
        // when
        var result = repository.save(person);
        // then
        assertThat(result).isSameAs(person);
        verify(operations).save(person);
    }

    @Test
    @DisplayName("should save all in input order")
    void shouldSaveAllInInputOrder() {
        // given
        var first = new Person("a");
        var second = new Person("b");
        when(operations.save(first)).thenReturn(first);
        when(operations.save(second)).thenReturn(second);
        // when
        var result = repository.saveAll(List.of(first, second));
        // then
        assertThat(result).containsExactly(first, second);
    }

    @Test
    @DisplayName("should stop save all on first failure")
    void shouldStopSaveAllOnFirstFailure() {
        // given
        var first = new Person("a");
        var second = new Person("b");
        var failure = new IllegalStateException();
        when(operations.save(first)).thenReturn(first);
        when(operations.save(second)).thenThrow(failure);
        // when
        // then
        assertThatThrownBy(() -> repository.saveAll(List.of(first, second, new Person("c"))))
                .isSameAs(failure);
        verify(operations).save(first);
        verify(operations).save(second);
    }

    @Test
    @DisplayName("should find by ID delegating with string ID")
    void shouldFindByIdDelegatingWithStringId() {
        // given
        var person = new Person("x");
        when(operations.findById("x", Person.class)).thenReturn(Optional.of(person));
        // when
        var result = repository.findById("x");
        // then
        assertThat(result).contains(person);
    }

    @Test
    @DisplayName("should return empty when find by ID misses")
    void shouldReturnEmptyWhenFindByIdMisses() {
        // given
        when(operations.findById("x", Person.class)).thenReturn(Optional.empty());
        // when
        var result = repository.findById("x");
        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should check existence delegating with string ID")
    void shouldCheckExistenceDelegatingWithStringId() {
        // given
        when(operations.existsById("x", Person.class)).thenReturn(true);
        // when
        var result = repository.existsById("x");
        // then
        assertThat(result).isTrue();
        verify(operations).existsById("x", Person.class);
    }

    @Test
    @DisplayName("should find all delegating with entity class")
    void shouldFindAllDelegatingWithEntityClass() {
        // given
        var a = new Person("a");
        var b = new Person("b");
        when(operations.findAll(Person.class)).thenReturn(List.of(a, b));
        // when
        var result = repository.findAll();
        // then
        assertThat(result).containsExactly(a, b);
        verify(operations).findAll(Person.class);
    }

    @Test
    @DisplayName("should find all by ID preserving order duplicates and skipping missing")
    void shouldFindAllByIdPreservingOrderAndDuplicatesAndSkippingMissing() {
        // given
        var a = new Person("a");
        var b = new Person("b");
        when(operations.findById("a", Person.class)).thenReturn(Optional.of(a));
        when(operations.findById("b", Person.class)).thenReturn(Optional.of(b));
        when(operations.findById("missing", Person.class)).thenReturn(Optional.empty());
        // when
        var result = repository.findAllById(List.of("a", "b", "a", "missing"));
        // then
        assertThat(result).containsExactly(a, b, a);
    }

    @Test
    @DisplayName("should count delegating to operations")
    void shouldCountDelegatingToOperations() {
        // given
        when(operations.count(Person.class)).thenReturn(2L);
        // when
        var result = repository.count();
        // then
        assertThat(result).isEqualTo(2L);
    }

    @Test
    @DisplayName("should delete by ID delegating with string ID")
    void shouldDeleteByIdDelegatingWithStringId() {
        // given
        // when
        repository.deleteById("x");
        // then
        verify(operations).deleteById("x", Person.class);
    }

    @Test
    @DisplayName("should delete entity delegating")
    void shouldDeleteEntityDelegating() {
        // given
        var person = new Person("x");
        // when
        repository.delete(person);
        // then
        verify(operations).delete(person);
    }

    @Test
    @DisplayName("should delete all by ID in order")
    void shouldDeleteAllByIdInOrder() {
        // given
        var ordered = inOrder(operations);
        // when
        repository.deleteAllById(List.of("a", "b"));
        // then
        ordered.verify(operations).deleteById("a", Person.class);
        ordered.verify(operations).deleteById("b", Person.class);
    }

    @Test
    @DisplayName("should delete all entities in order")
    void shouldDeleteAllEntitiesInOrder() {
        // given
        var a = new Person("a");
        var b = new Person("b");
        var ordered = inOrder(operations);
        // when
        repository.deleteAll(List.of(a, b));
        // then
        ordered.verify(operations).delete(a);
        ordered.verify(operations).delete(b);
    }

    @Test
    @DisplayName("should delete all by listing then deleting")
    void shouldDeleteAllByListingThenDeleting() {
        // given
        var a = new Person("a");
        when(operations.findAll(Person.class)).thenReturn(List.of(a));
        // when
        repository.deleteAll();
        // then
        verify(operations).delete(a);
    }

    @Test
    @DisplayName("should reject blank ID")
    void shouldRejectBlankId() {
        assertThatThrownBy(() -> repository.findById(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should reject null ID")
    void shouldRejectNullId() {
        assertThatThrownBy(() -> repository.findById(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should reject null save all input")
    void shouldRejectNullSaveAllInput() {
        assertThatThrownBy(() -> repository.saveAll(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should reject null save all element")
    void shouldRejectNullSaveAllElement() {
        var list = Arrays.asList(new Person("a"), null);
        assertThatThrownBy(() -> repository.saveAll(list))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should reject null find all by ID input")
    void shouldRejectNullFindAllByIdInput() {
        assertThatThrownBy(() -> repository.findAllById(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should reject null find all by ID element")
    void shouldRejectNullFindAllByIdElement() {
        var ids = Arrays.asList("a", null);
        assertThatThrownBy(() -> repository.findAllById(ids))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should reject null delete all by ID input")
    void shouldRejectNullDeleteAllByIdInput() {
        assertThatThrownBy(() -> repository.deleteAllById(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should reject null delete all by ID element")
    void shouldRejectNullDeleteAllByIdElement() {
        var ids = Arrays.asList("a", null);
        assertThatThrownBy(() -> repository.deleteAllById(ids))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should reject null delete all input")
    void shouldRejectNullDeleteAllInput() {
        assertThatThrownBy(() -> repository.deleteAll(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should reject null delete all element")
    void shouldRejectNullDeleteAllElement() {
        var list = Arrays.asList(new Person("a"), null);
        assertThatThrownBy(() -> repository.deleteAll(list))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should propagate stale exception unchanged")
    void shouldPropagateStaleExceptionUnchanged() {
        // given
        var person = new Person("x");
        var failure = new CouchOptimisticLockingFailureException("stale", "db", "x", "1-a", null);
        doThrow(failure).when(operations).delete(person);
        // when
        // then
        assertThatThrownBy(() -> repository.delete(person)).isSameAs(failure);
    }

    @SuppressWarnings("unchecked")
    private static CouchPersistentEntity<Person> entity(CouchMappingContext context) {
        // The requested domain class determines the wildcarded mapping result.
        return (CouchPersistentEntity<Person>) context.getRequiredPersistentEntity(Person.class);
    }

    @CouchDocument
    static class Person {
        @Id
        String id;

        Person(String id) {
            this.id = id;
        }
    }
}
