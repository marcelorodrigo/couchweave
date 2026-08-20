package io.github.marcelorodrigo.couchweave.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.marcelorodrigo.couchweave.CouchWeaveOperations;
import io.github.marcelorodrigo.couchweave.mapping.CouchDocument;
import io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext;
import io.github.marcelorodrigo.couchweave.repository.support.CouchWeaveRepositoryFactoryBean;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.annotation.Id;

class CouchWeaveRepositoryContextTest {
    @Test
    @DisplayName("should create proxied repository")
    void shouldCreateProxiedRepository() {
        // given
        var operations = mock(CouchWeaveOperations.class);
        var person = new Person("p1");
        when(operations.findById("p1", Person.class)).thenReturn(Optional.of(person));
        try (var context = context(operations, PersonRepository.class)) {
            context.refresh();
            // when
            var repository = context.getBean(PersonRepository.class);
            repository.findById("p1");
            // then
            assertThat(java.lang.reflect.Proxy.isProxyClass(repository.getClass()))
                    .isTrue();
            verify(operations).findById("p1", Person.class);
        }
    }

    @Test
    @DisplayName("should fail startup for non-string ID repository")
    void shouldFailStartupForNonStringIdRepository() {
        // given
        var operations = mock(CouchWeaveOperations.class);
        var context = context(operations, LongIdRepository.class);
        // when
        // then
        org.assertj.core.api.Assertions.assertThatThrownBy(context::refresh)
                .hasCauseInstanceOf(CouchWeaveRepositoryConfigurationException.class);
        context.close();
    }

    private static AnnotationConfigApplicationContext context(
            CouchWeaveOperations operations, Class<?> repositoryType) {
        var context = new AnnotationConfigApplicationContext();
        var mappingContext = new CouchMappingContext("db");
        mappingContext.setInitialEntitySet(java.util.Set.of(Person.class));
        mappingContext.initialize();
        context.registerBean(CouchMappingContext.class, () -> mappingContext);
        context.registerBean(CouchWeaveOperations.class, () -> operations);
        context.registerBean(
                "repositoryFactory",
                TestFactoryBean.class,
                () -> new TestFactoryBean(repositoryType, operations, mappingContext));
        return context;
    }

    static class TestFactoryBean
            extends CouchWeaveRepositoryFactoryBean<
                    org.springframework.data.repository.Repository<Object, Object>, Object, Object> {
        TestFactoryBean(Class<?> repositoryType, CouchWeaveOperations operations, CouchMappingContext mappingContext) {
            super(repositoryInterface(repositoryType));
            setOperations(operations);
            setMappingContext(mappingContext);
        }

        @SuppressWarnings("unchecked")
        private static Class<? extends org.springframework.data.repository.Repository<Object, Object>>
                repositoryInterface(Class<?> repositoryType) {
            // The test factory uses erased repository generics for both fixture interfaces.
            return (Class<? extends org.springframework.data.repository.Repository<Object, Object>>)
                    repositoryType.asSubclass(org.springframework.data.repository.Repository.class);
        }
    }

    @CouchDocument
    static class Person {
        @Id
        String id;

        Person(String id) {
            this.id = id;
        }
    }

    interface PersonRepository extends CouchWeaveRepository<Person, String> {}

    interface LongIdRepository extends CouchWeaveRepository<Person, Long> {}
}
