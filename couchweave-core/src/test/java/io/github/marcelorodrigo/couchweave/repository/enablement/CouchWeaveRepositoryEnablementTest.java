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
import io.github.marcelorodrigo.couchweave.mapping.CouchWeaveConverter;
import io.github.marcelorodrigo.couchweave.mapping.CouchWeaveCustomConversions;
import io.github.marcelorodrigo.couchweave.mapping.MappingCouchWeaveConverter;
import io.github.marcelorodrigo.couchweave.repository.CouchWeaveRepository;
import io.github.marcelorodrigo.couchweave.repository.config.EnableCouchWeaveRepositories;
import java.lang.reflect.Proxy;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.FactoryBean;
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
            assertThat(context.getBean(CouchMappingContext.class).isStrict()).isFalse();
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
    @DisplayName("should honor a custom mapping context override and preserve its strictness")
    void shouldUseCustomMappingContext() {
        // given
        try (var context = context(WithCustomMappingConfig.class)) {
            // when / then
            assertThat(context.getBean(CouchMappingContext.class)).isSameAs(CustomMappingHolder.mappingContext);
            assertThat(context.getBean(CouchMappingContext.class).isStrict()).isTrue();
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
    @DisplayName("should honor a custom REST client builder supplied through a FactoryBean")
    void shouldUseCustomBuilderFromFactoryBean() {
        // given
        try (var context = context(WithBuilderFactoryBeanConfig.class)) {
            // when / then
            assertThat(context.getBean(RestClient.Builder.class)).isSameAs(BuilderFactoryBean.product);
        }
    }

    @Test
    @DisplayName("should honor a custom mapping context supplied through a FactoryBean")
    void shouldUseMappingContextFromFactoryBean() {
        // given
        try (var context = context(WithMappingFactoryBeanConfig.class)) {
            // when / then
            assertThat(context.getBean(CouchMappingContext.class)).isSameAs(MappingFactoryBean.product);
        }
    }

    @Test
    @DisplayName("should honor a custom conversions supplied through a FactoryBean")
    void shouldUseConversionsFromFactoryBean() {
        // given
        try (var context = context(WithConversionsFactoryBeanConfig.class)) {
            // when / then
            assertThat(context.getBean(CouchWeaveCustomConversions.class)).isSameAs(ConversionsFactoryBean.product);
        }
    }

    @Test
    @DisplayName("should honor a custom converter supplied through a FactoryBean")
    void shouldUseConverterFromFactoryBean() {
        // given
        try (var context = context(WithConverterFactoryBeanConfig.class)) {
            // when / then
            assertThat(context.getBean(CouchWeaveConverter.class)).isSameAs(ConverterFactoryBean.product);
        }
    }

    @Test
    @DisplayName("should honor a custom operations supplied through a FactoryBean")
    void shouldUseOperationsFromFactoryBean() {
        // given
        try (var context = context(WithOperationsFactoryBeanConfig.class)) {
            // when / then
            assertThat(context.getBean(CouchWeaveOperations.class)).isSameAs(OperationsFactoryBean.product);
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
    static class WithBuilderFactoryBeanConfig {
        @Bean
        CouchDbClientSettings couchDbClientSettings() {
            return settings();
        }

        @Bean
        FactoryBean<RestClient.Builder> couchWeaveRestClientBuilder() {
            return BuilderFactoryBean.factoryBean();
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true)
    static class WithMappingFactoryBeanConfig {
        @Bean
        CouchDbClientSettings couchDbClientSettings() {
            return settings();
        }

        @Bean
        FactoryBean<CouchMappingContext> couchMappingContext() {
            return MappingFactoryBean.factoryBean();
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true)
    static class WithConversionsFactoryBeanConfig {
        @Bean
        CouchDbClientSettings couchDbClientSettings() {
            return settings();
        }

        @Bean
        FactoryBean<CouchWeaveCustomConversions> couchWeaveCustomConversions() {
            return ConversionsFactoryBean.factoryBean();
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true)
    static class WithConverterFactoryBeanConfig {
        @Bean
        CouchDbClientSettings couchDbClientSettings() {
            return settings();
        }

        @Bean
        FactoryBean<CouchWeaveConverter> couchWeaveConverter(
                CouchMappingContext mappingContext, CouchWeaveCustomConversions customConversions) {
            return ConverterFactoryBean.factoryBean(mappingContext, customConversions);
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(
            basePackageClasses = CouchWeaveRepositoryEnablementTest.class,
            considerNestedRepositories = true)
    static class WithOperationsFactoryBeanConfig {
        @Bean
        CouchDbClientSettings couchDbClientSettings() {
            return settings();
        }

        @Bean
        FactoryBean<CouchWeaveOperations> couchWeaveOperations() {
            return OperationsFactoryBean.factoryBean();
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
        static final CouchMappingContext mappingContext = createStrictMapping();

        private static CouchMappingContext createStrictMapping() {
            var context = new CouchMappingContext("custom_db", true);
            context.setInitialEntitySet(java.util.Set.of(Person.class));
            context.initialize();
            return context;
        }
    }

    static final class CustomConversionsHolder {
        static final CouchWeaveCustomConversions conversions = new CouchWeaveCustomConversions(java.util.List.of());
    }

    static final class InstanceFactoryBean<T> implements FactoryBean<T> {
        private final T instance;
        private final Class<T> objectType;

        InstanceFactoryBean(T instance, Class<T> objectType) {
            this.instance = instance;
            this.objectType = objectType;
        }

        @Override
        public T getObject() {
            return instance;
        }

        @Override
        public Class<?> getObjectType() {
            return objectType;
        }

        @Override
        public boolean isSingleton() {
            return true;
        }
    }

    static final class BuilderFactoryBean {
        static final RestClient.Builder product = RestClient.builder();

        static FactoryBean<RestClient.Builder> factoryBean() {
            return new InstanceFactoryBean<>(product, RestClient.Builder.class);
        }
    }

    static final class MappingFactoryBean {
        static final CouchMappingContext product = new CouchMappingContext("factory_db", false);

        static FactoryBean<CouchMappingContext> factoryBean() {
            return new InstanceFactoryBean<>(product, CouchMappingContext.class);
        }
    }

    static final class ConversionsFactoryBean {
        static final CouchWeaveCustomConversions product = new CouchWeaveCustomConversions(java.util.List.of());

        static FactoryBean<CouchWeaveCustomConversions> factoryBean() {
            return new InstanceFactoryBean<>(product, CouchWeaveCustomConversions.class);
        }
    }

    static final class ConverterFactoryBean {
        static CouchWeaveConverter product;

        static FactoryBean<CouchWeaveConverter> factoryBean(
                CouchMappingContext mappingContext, CouchWeaveCustomConversions customConversions) {
            product = new MappingCouchWeaveConverter(mappingContext, customConversions);
            return new InstanceFactoryBean<>(product, CouchWeaveConverter.class);
        }
    }

    static final class OperationsFactoryBean {
        static final CouchWeaveOperations product = mock(CouchWeaveOperations.class);

        static FactoryBean<CouchWeaveOperations> factoryBean() {
            return new InstanceFactoryBean<>(product, CouchWeaveOperations.class);
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

    interface ExcludedRepository extends CouchWeaveRepository<Person, String> {}

    interface UnrelatedRepository extends CrudRepository<Person, String> {}

    static class NestedHolder {
        interface NestedRepository extends CouchWeaveRepository<Person, String> {}
    }
}
