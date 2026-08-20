package io.github.marcelorodrigo.couchweave.repository.enablement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.marcelorodrigo.couchweave.CouchWeaveOperations;
import io.github.marcelorodrigo.couchweave.CouchWeaveTemplate;
import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import io.github.marcelorodrigo.couchweave.mapping.CouchDocument;
import io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext;
import io.github.marcelorodrigo.couchweave.mapping.CouchWeaveCustomConversions;
import io.github.marcelorodrigo.couchweave.mapping.MappingCouchWeaveConverter;
import io.github.marcelorodrigo.couchweave.repository.CouchWeaveRepository;
import io.github.marcelorodrigo.couchweave.repository.config.EnableCouchWeaveRepositories;
import java.lang.reflect.Proxy;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.client.RestClient;

class CouchWeaveRepositoryEnablementTest {

    @Test
    @DisplayName("should create default infrastructure and a proxied repository")
    void shouldCreateDefaultInfrastructure() {
        // given
        try (var context = context(DefaultStackConfig.class)) {
            // when / then
            assertThat(context.getBean(CouchMappingContext.class)).isNotNull();
            assertThat(context.getBean(CouchWeaveCustomConversions.class)).isNotNull();
            assertThat(context.getBean(MappingCouchWeaveConverter.class)).isNotNull();
            assertThat(context.getBean(RestClient.Builder.class)).isNotNull();
            assertThat(context.getBean(CouchWeaveOperations.class)).isInstanceOf(CouchWeaveTemplate.class);

            var repository = context.getBean(PersonRepository.class);
            assertThat(Proxy.isProxyClass(repository.getClass())).isTrue();
        }
    }

    @Test
    @DisplayName("should delegate to a custom operations bean without creating the default")
    void shouldUseCustomOperations() {
        // given
        try (var context = context(WithCustomOperationsConfig.class)) {
            // when / then
            assertThat(context.getBean(CouchWeaveOperations.class)).isSameAs(CustomOperationsHolder.operations);
            var repository = context.getBean(PersonRepository.class);
            var person = new Person("p1");
            when(CustomOperationsHolder.operations.findById("p1", Person.class)).thenReturn(Optional.of(person));

            var found = repository.findById("p1");

            assertThat(found).contains(person);
            verify(CustomOperationsHolder.operations).findById("p1", Person.class);
        }
    }

    @Test
    @DisplayName("should honor a custom REST client builder override")
    void shouldUseCustomRestClientBuilder() {
        // given
        try (var context = context(WithCustomBuilderConfig.class)) {
            // when / then
            assertThat(context.getBean(RestClient.Builder.class)).isSameAs(CustomBuilderHolder.builder);
        }
    }

    @Test
    @DisplayName("should honor a custom mapping context override")
    void shouldUseCustomMappingContext() {
        // given
        try (var context = context(WithCustomMappingConfig.class)) {
            // when / then
            assertThat(context.getBean(CouchMappingContext.class)).isSameAs(CustomMappingHolder.mappingContext);
        }
    }

    @Test
    @DisplayName("should honor a custom conversions override")
    void shouldUseCustomConversions() {
        // given
        try (var context = context(WithCustomConversionsConfig.class)) {
            // when / then
            assertThat(context.getBean(CouchWeaveCustomConversions.class))
                    .isSameAs(CustomConversionsHolder.conversions);
        }
    }

    @Test
    @DisplayName("should fail startup when required settings are missing")
    void shouldFailWithoutSettings() {
        // given
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(MissingSettingsConfig.class);
            // when / then
            assertThatThrownBy(context::refresh).hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    @Test
    @DisplayName("should start without settings when operations and mapping context are supplied")
    void shouldStartWithoutSettingsWhenInfrastructureProvided() {
        // given
        try (var context = context(NoSettingsConfig.class)) {
            // when / then
            var repository = context.getBean(PersonRepository.class);
            assertThat(Proxy.isProxyClass(repository.getClass())).isTrue();
        }
    }

    @Test
    @DisplayName("should discover repositories through base package classes")
    void shouldDiscoverByBasePackageClasses() {
        // given
        try (var context = context(ScannedConfig.class)) {
            // when / then
            assertThat(context.getBean(PersonRepository.class)).isNotNull();
            assertThat(context.getBean(ExcludedRepository.class)).isNotNull();
        }
    }

    @Test
    @DisplayName("should respect include filters")
    void shouldRespectIncludeFilters() {
        // given
        try (var context = context(IncludeFilteredConfig.class)) {
            // when / then
            assertThat(context.getBean(PersonRepository.class)).isNotNull();
            assertThatThrownBy(() -> context.getBean(ExcludedRepository.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    @Test
    @DisplayName("should respect exclude filters")
    void shouldRespectExcludeFilters() {
        // given
        try (var context = context(ExcludeFilteredConfig.class)) {
            // when / then
            assertThatThrownBy(() -> context.getBean(ExcludedRepository.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
            assertThat(context.getBean(PersonRepository.class)).isNotNull();
        }
    }

    @Test
    @DisplayName("should ignore repositories that are not CouchWeave repositories")
    void shouldIgnoreUnrelatedRepositories() {
        // given
        try (var context = context(ScannedConfig.class)) {
            // when / then
            assertThatThrownBy(() -> context.getBean(UnrelatedRepository.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    @Test
    @DisplayName("should not consider nested repositories by default")
    void shouldExcludeNestedRepositoriesByDefault() {
        // given
        try (var context = context(NestedExcludedByDefaultConfig.class)) {
            // when / then
            assertThatThrownBy(() -> context.getBean(NestedHolder.NestedRepository.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    @Test
    @DisplayName("should consider nested repositories when enabled")
    void shouldIncludeNestedRepositoriesWhenEnabled() {
        // given
        try (var context = context(NestedEnabledConfig.class)) {
            // when / then
            assertThat(context.getBean(NestedHolder.NestedRepository.class)).isNotNull();
        }
    }

    @Test
    @DisplayName("should fail when multiple operations candidates are not disambiguated")
    void shouldFailWithAmbiguousOperations() {
        // given
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(AmbiguousOperationsConfig.class);
            // when / then
            assertThatThrownBy(context::refresh).hasRootCauseInstanceOf(NoUniqueBeanDefinitionException.class);
        }
    }

    @Test
    @DisplayName("should resolve ambiguous operations when one is primary")
    void shouldResolvePrimaryOperations() {
        // given
        try (var context = context(PrimaryOperationsConfig.class)) {
            // when / then
            assertThat(context.getBean(PersonRepository.class)).isNotNull();
        }
    }

    private static AnnotationConfigApplicationContext context(Class<?> configuration) {
        var context = new AnnotationConfigApplicationContext();
        context.register(configuration);
        context.refresh();
        return context;
    }

    static CouchDbClientSettings settings() {
        return new CouchDbClientSettings(
                java.net.URI.create("http://localhost:5984"),
                "couchweave_test",
                null,
                null,
                java.time.Duration.ofSeconds(5),
                java.time.Duration.ofSeconds(30));
    }

    static CouchMappingContext nonStrictMapping() {
        return new CouchMappingContext("couchweave_test", false);
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true)
    static class DefaultStackConfig {
        @Bean
        CouchDbClientSettings couchDbClientSettings() {
            return settings();
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true)
    static class WithCustomOperationsConfig {
        @Bean
        CouchMappingContext couchMappingContext() {
            return nonStrictMapping();
        }

        @Bean
        CouchWeaveOperations couchWeaveOperations() {
            return CustomOperationsHolder.operations;
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true)
    static class WithCustomBuilderConfig {
        @Bean
        CouchDbClientSettings couchDbClientSettings() {
            return settings();
        }

        @Bean
        RestClient.Builder couchWeaveRestClientBuilder() {
            return CustomBuilderHolder.builder;
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true)
    static class WithCustomMappingConfig {
        @Bean
        CouchDbClientSettings couchDbClientSettings() {
            return settings();
        }

        @Bean
        CouchMappingContext couchMappingContext() {
            return CustomMappingHolder.mappingContext;
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true)
    static class WithCustomConversionsConfig {
        @Bean
        CouchDbClientSettings couchDbClientSettings() {
            return settings();
        }

        @Bean
        CouchWeaveCustomConversions couchWeaveCustomConversions() {
            return CustomConversionsHolder.conversions;
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true)
    static class MissingSettingsConfig {}

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true)
    static class NoSettingsConfig {
        @Bean
        CouchMappingContext couchMappingContext() {
            return nonStrictMapping();
        }

        @Bean
        CouchWeaveOperations couchWeaveOperations() {
            return mock(CouchWeaveOperations.class);
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true)
    static class ScannedConfig {
        @Bean
        CouchDbClientSettings couchDbClientSettings() {
            return settings();
        }

        @Bean
        CouchMappingContext couchMappingContext() {
            return nonStrictMapping();
        }

        @Bean
        CouchWeaveOperations couchWeaveOperations() {
            return mock(CouchWeaveOperations.class);
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true,
            includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PersonRepository.class))
    static class IncludeFilteredConfig {
        @Bean
        CouchDbClientSettings couchDbClientSettings() {
            return settings();
        }

        @Bean
        CouchMappingContext couchMappingContext() {
            return nonStrictMapping();
        }

        @Bean
        CouchWeaveOperations couchWeaveOperations() {
            return mock(CouchWeaveOperations.class);
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true,
            excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ExcludedRepository.class))
    static class ExcludeFilteredConfig {
        @Bean
        CouchDbClientSettings couchDbClientSettings() {
            return settings();
        }

        @Bean
        CouchMappingContext couchMappingContext() {
            return nonStrictMapping();
        }

        @Bean
        CouchWeaveOperations couchWeaveOperations() {
            return mock(CouchWeaveOperations.class);
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true)
    static class NestedEnabledConfig {
        @Bean
        CouchDbClientSettings couchDbClientSettings() {
            return settings();
        }

        @Bean
        CouchMappingContext couchMappingContext() {
            return nonStrictMapping();
        }

        @Bean
        CouchWeaveOperations couchWeaveOperations() {
            return mock(CouchWeaveOperations.class);
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = false)
    static class NestedExcludedByDefaultConfig {
        @Bean
        CouchDbClientSettings couchDbClientSettings() {
            return settings();
        }

        @Bean
        CouchMappingContext couchMappingContext() {
            return nonStrictMapping();
        }

        @Bean
        CouchWeaveOperations couchWeaveOperations() {
            return mock(CouchWeaveOperations.class);
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true)
    static class AmbiguousOperationsConfig {
        @Bean
        CouchMappingContext couchMappingContext() {
            return nonStrictMapping();
        }

        @Bean
        CouchWeaveOperations firstOperations() {
            return mock(CouchWeaveOperations.class);
        }

        @Bean
        CouchWeaveOperations secondOperations() {
            return mock(CouchWeaveOperations.class);
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true)
    static class PrimaryOperationsConfig {
        @Bean
        CouchMappingContext couchMappingContext() {
            return nonStrictMapping();
        }

        @Bean
        @org.springframework.context.annotation.Primary
        CouchWeaveOperations primaryOperations() {
            return mock(CouchWeaveOperations.class);
        }

        @Bean
        CouchWeaveOperations secondaryOperations() {
            return mock(CouchWeaveOperations.class);
        }
    }

    static final class CustomOperationsHolder {
        static final CouchWeaveOperations operations = mock(CouchWeaveOperations.class);
    }

    static final class CustomBuilderHolder {
        static final RestClient.Builder builder = RestClient.builder();
    }

    static final class CustomMappingHolder {
        static final CouchMappingContext mappingContext = new CouchMappingContext("custom_db", false);
    }

    static final class CustomConversionsHolder {
        static final CouchWeaveCustomConversions conversions = new CouchWeaveCustomConversions(java.util.List.of());
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

    interface ExcludedRepository extends CouchWeaveRepository<Person, String> {}

    interface UnrelatedRepository extends CrudRepository<Person, String> {}

    static class NestedHolder {
        interface NestedRepository extends CouchWeaveRepository<Person, String> {}
    }
}
