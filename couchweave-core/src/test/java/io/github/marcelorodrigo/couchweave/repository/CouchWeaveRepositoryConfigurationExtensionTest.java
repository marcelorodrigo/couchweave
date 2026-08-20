package io.github.marcelorodrigo.couchweave.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcelorodrigo.couchweave.mapping.CouchDocument;
import io.github.marcelorodrigo.couchweave.repository.config.CouchWeaveRepositoryConfigurationExtension;
import io.github.marcelorodrigo.couchweave.repository.support.CouchWeaveRepositoryFactoryBean;
import io.github.marcelorodrigo.couchweave.repository.support.SimpleCouchWeaveRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

class CouchWeaveRepositoryConfigurationExtensionTest {
    private final ExposedExtension extension = new ExposedExtension();

    @Test
    @DisplayName("should expose module name")
    void shouldExposeModuleName() {
        assertThat(extension.getModuleName()).isEqualTo("CouchWeave");
    }

    @Test
    @DisplayName("should expose module prefix")
    void shouldExposeModulePrefix() {
        assertThat(extension.modulePrefix()).isEqualTo("couchweave");
    }

    @Test
    @DisplayName("should identify CouchWeave repository type")
    void shouldIdentifyCouchWeaveRepositoryType() {
        assertThat(extension.identifyingTypes()).containsExactly(CouchWeaveRepository.class);
    }

    @Test
    @DisplayName("should expose factory bean class name")
    void shouldExposeFactoryBeanClassName() {
        assertThat(extension.getRepositoryFactoryBeanClassName())
                .isEqualTo(CouchWeaveRepositoryFactoryBean.class.getName());
    }

    @Test
    @DisplayName("should expose repository base class name")
    void shouldExposeRepositoryBaseClassName() {
        assertThat(extension.getRepositoryBaseClassName()).isEqualTo(SimpleCouchWeaveRepository.class.getName());
    }

    @Test
    @DisplayName("should have no identifying annotations")
    void shouldHaveNoIdentifyingAnnotations() {
        assertThat(extension.identifyingAnnotations()).isEmpty();
    }

    @Test
    @DisplayName("should not identify unrelated CrudRepository")
    void shouldNotIdentifyUnrelatedCrudRepository() {
        // given
        // when
        var result = extension.strict(new DefaultRepositoryMetadata(PlainRepo.class));
        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should wire the mapping context property reference")
    void shouldWireMappingContextPropertyReference() {
        // given
        var builder = BeanDefinitionBuilder.genericBeanDefinition(String.class);
        // when
        extension.postProcess(builder, (RepositoryConfigurationSource) null);
        // then
        var propertyValue = builder.getRawBeanDefinition().getPropertyValues().get("mappingContext");
        assertThat(propertyValue).isInstanceOf(RuntimeBeanReference.class);
        assertThat(((RuntimeBeanReference) propertyValue).getBeanName()).isEqualTo("couchMappingContext");
    }

    static class ExposedExtension extends CouchWeaveRepositoryConfigurationExtension {
        String modulePrefix() {
            return getModulePrefix();
        }

        java.util.Collection<Class<?>> identifyingTypes() {
            return getIdentifyingTypes();
        }

        java.util.Collection<Class<? extends java.lang.annotation.Annotation>> identifyingAnnotations() {
            return getIdentifyingAnnotations();
        }

        boolean strict(org.springframework.data.repository.core.RepositoryMetadata metadata) {
            return isStrictRepositoryCandidate(metadata);
        }
    }

    @CouchDocument
    static class Person {
        @Id
        String id;
    }

    interface CouchRepo extends CouchWeaveRepository<Person, String> {}

    interface PlainRepo extends CrudRepository<Person, String> {}
}
