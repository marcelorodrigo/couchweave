package io.github.marcelorodrigo.couchweave.repository.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.marcelorodrigo.couchweave.mapping.CouchDocument;
import io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext;
import io.github.marcelorodrigo.couchweave.mapping.CouchPersistentEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

class CouchWeaveEntityInformationTest {

    private final CouchWeaveEntityInformation<Person, String> information = information();

    @Test
    @DisplayName("should expose domain type")
    void shouldExposeDomainType() {
        // given
        // when
        var result = information.getJavaType();
        // then
        assertThat(result).isEqualTo(Person.class);
    }

    @Test
    @DisplayName("should expose string ID type")
    void shouldExposeStringIdType() {
        // given
        // when
        var result = information.getIdType();
        // then
        assertThat(result).isEqualTo(String.class);
    }

    @Test
    @DisplayName("should read mapped ID")
    void shouldReadMappedId() {
        // given
        var person = new Person("p1");
        // when
        var result = information.getId(person);
        // then
        assertThat(result).isEqualTo("p1");
    }

    @Test
    @DisplayName("should return null ID for new entity without ID")
    void shouldReturnNullIdForNewEntityWithoutId() {
        // given
        // when
        var result = information.getId(new Person(null));
        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should delegate newness to persistent metadata")
    void shouldDelegateNewnessToPersistentMetadata() {
        // given
        // when
        var result = information.isNew(new Person(null));
        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should reject null entity metadata")
    void shouldRejectNullEntityMetadata() {
        // given
        // when
        // then
        assertThatThrownBy(() -> new CouchWeaveEntityInformation<Person, String>(null))
                .isInstanceOf(NullPointerException.class);
    }

    private static CouchWeaveEntityInformation<Person, String> information() {
        var context = new CouchMappingContext("default-db");
        context.setInitialEntitySet(java.util.Set.of(Person.class));
        context.initialize();
        return new CouchWeaveEntityInformation<>(entity(context));
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
