package io.github.marcelorodrigo.couchweave.repository.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.marcelorodrigo.couchweave.CouchWeaveOperations;
import io.github.marcelorodrigo.couchweave.mapping.CouchDocument;
import io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext;
import io.github.marcelorodrigo.couchweave.repository.CouchWeaveRepository;
import io.github.marcelorodrigo.couchweave.repository.CouchWeaveRepositoryConfigurationException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

class CouchWeaveRepositoryFactoryTest {
    @Test
    @DisplayName("should return simple CouchWeave repository as base class")
    void shouldReturnSimpleCouchWeaveRepositoryAsBaseClass() {
        // given
        var factory = factory();
        // when
        var result = factoryBase(factory, ValidRepo.class);
        // then
        assertThat(result).isEqualTo(SimpleCouchWeaveRepository.class);
    }

    @Test
    @DisplayName("should reject non-string ID repository")
    void shouldRejectNonStringIdRepository() {
        // given
        var factory = factory();
        // when
        // then
        assertThatThrownBy(() -> factory.getRepository(LongRepo.class))
                .isInstanceOf(CouchWeaveRepositoryConfigurationException.class)
                .hasMessageContaining("LongRepo", "Person", "java.lang.String");
    }

    @Test
    @DisplayName("should reject unmapped domain type")
    void shouldRejectUnmappedDomainType() {
        // given
        var factory = factory();
        // when
        // then
        assertThatThrownBy(() -> factory.getRepository(UnmappedRepo.class))
                .isInstanceOf(CouchWeaveRepositoryConfigurationException.class);
    }

    @Test
    @DisplayName("should create repository proxy for valid interface")
    void shouldCreateRepositoryProxyForValidInterface() {
        // given
        var operations = mock(CouchWeaveOperations.class);
        var person = new Person("x");
        when(operations.findById("x", Person.class)).thenReturn(Optional.of(person));
        var factory = factory(operations);
        // when
        var result = factory.getRepository(ValidRepo.class);
        result.findById("x");
        // then
        assertThat(java.lang.reflect.Proxy.isProxyClass(result.getClass())).isTrue();
        verify(operations).findById("x", Person.class);
    }

    private static CouchWeaveRepositoryFactory factory() {
        return factory(mock(CouchWeaveOperations.class));
    }

    private static CouchWeaveRepositoryFactory factory(CouchWeaveOperations operations) {
        var context = new CouchMappingContext("db");
        context.setInitialEntitySet(java.util.Set.of(Person.class));
        context.initialize();
        return new CouchWeaveRepositoryFactory(operations, context);
    }

    private static Class<?> factoryBase(CouchWeaveRepositoryFactory factory, Class<?> type) {
        return base(factory, new DefaultRepositoryMetadata(type));
    }

    private static Class<?> base(
            CouchWeaveRepositoryFactory factory, org.springframework.data.repository.core.RepositoryMetadata metadata) {
        class Exposed extends CouchWeaveRepositoryFactory {
            Exposed() {
                super(mock(CouchWeaveOperations.class), new CouchMappingContext("db"));
            }

            Class<?> base() {
                return getRepositoryBaseClass(metadata);
            }
        }
        return new Exposed().base();
    }

    @CouchDocument
    static class Person {
        @Id
        String id;

        Person(String id) {
            this.id = id;
        }
    }

    static class Unmapped {}

    interface ValidRepo extends CouchWeaveRepository<Person, String> {}

    interface LongRepo extends CouchWeaveRepository<Person, Long> {}

    interface UnmappedRepo extends CouchWeaveRepository<Unmapped, String> {}
}
