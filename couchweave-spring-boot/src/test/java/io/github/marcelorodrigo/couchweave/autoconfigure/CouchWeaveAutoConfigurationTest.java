package io.github.marcelorodrigo.couchweave.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcelorodrigo.couchweave.CouchWeaveOperations;
import io.github.marcelorodrigo.couchweave.CouchWeaveTemplate;
import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext;
import io.github.marcelorodrigo.couchweave.mapping.CouchWeaveConverter;
import io.github.marcelorodrigo.couchweave.mapping.CouchWeaveCustomConversions;
import io.github.marcelorodrigo.couchweave.mapping.MappingCouchWeaveConverter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

class CouchWeaveAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    CouchWeaveClientAutoConfiguration.class,
                    CouchWeaveMappingAutoConfiguration.class,
                    CouchWeaveOperationsAutoConfiguration.class))
            .withPropertyValues(
                    "spring.data.couchweave.server-uri=http://localhost:5984",
                    "spring.data.couchweave.database=couchweave_test");

    @Test
    @DisplayName("should create the complete default stack from valid properties")
    void shouldCreateDefaultStack() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(CouchDbClientSettings.class);
            assertThat(context).hasSingleBean(RestClient.Builder.class);
            assertThat(context).hasSingleBean(CouchMappingContext.class);
            assertThat(context.getBean(CouchMappingContext.class).isStrict()).isFalse();
            assertThat(context).hasSingleBean(CouchWeaveCustomConversions.class);
            assertThat(context).hasSingleBean(MappingCouchWeaveConverter.class);
            assertThat(context).hasSingleBean(CouchWeaveOperations.class);
            assertThat(context.getBean(CouchWeaveOperations.class)).isInstanceOf(CouchWeaveTemplate.class);
        });
    }

    @Test
    @DisplayName("should honor a custom CouchDbClientSettings bean")
    void shouldUseCustomSettings() {
        runner.withUserConfiguration(CustomSettingsConfig.class).run(context -> {
            assertThat(context.getBean(CouchDbClientSettings.class)).isSameAs(CustomSettingsConfig.settings);
            assertThat(context.getBean(CouchDbClientSettings.class).database()).isEqualTo("custom_db");
        });
    }

    @Test
    @DisplayName("should honor a custom RestClient.Builder bean")
    void shouldUseCustomBuilder() {
        runner.withUserConfiguration(CustomBuilderConfig.class)
                .run(context ->
                        assertThat(context.getBean(RestClient.Builder.class)).isSameAs(CustomBuilderConfig.builder));
    }

    @Test
    @DisplayName("should honor a custom CouchWeaveCustomConversions bean")
    void shouldUseCustomConversions() {
        runner.withUserConfiguration(CustomConversionsConfig.class)
                .run(context -> assertThat(context.getBean(CouchWeaveCustomConversions.class))
                        .isSameAs(CustomConversionsConfig.conversions));
    }

    @Test
    @DisplayName("should honor a custom CouchWeaveConverter bean")
    void shouldUseCustomConverter() {
        runner.withUserConfiguration(CustomConverterConfig.class)
                .run(context -> assertThat(context.getBean(CouchWeaveConverter.class))
                        .isSameAs(CustomConverterConfig.converter));
    }

    @Test
    @DisplayName("should honor a custom CouchWeaveOperations bean")
    void shouldUseCustomOperations() {
        runner.withUserConfiguration(CustomOperationsConfig.class)
                .run(context -> assertThat(context.getBean(CouchWeaveOperations.class))
                        .isSameAs(CustomOperationsConfig.operations));
    }

    @Test
    @DisplayName("should back off when the core client class is absent")
    void shouldBackOffWhenClientClassAbsent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CouchWeaveClientAutoConfiguration.class))
                .withPropertyValues(
                        "spring.data.couchweave.server-uri=http://localhost:5984",
                        "spring.data.couchweave.database=couchweave_test")
                .withClassLoader(new FilteredClassLoader(CouchDbClientSettings.class))
                .run(context -> assertThat(context).doesNotHaveBean(CouchDbClientSettings.class));
    }

    @Test
    @DisplayName("should back off when the core mapping classes are absent")
    void shouldBackOffWhenMappingClassAbsent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CouchWeaveMappingAutoConfiguration.class))
                .withClassLoader(new FilteredClassLoader(CouchMappingContext.class))
                .run(context -> assertThat(context).doesNotHaveBean(CouchMappingContext.class));
    }

    @Test
    @DisplayName("should back off when the core operations classes are absent")
    void shouldBackOffWhenOperationsClassAbsent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CouchWeaveOperationsAutoConfiguration.class))
                .withClassLoader(new FilteredClassLoader(CouchWeaveOperations.class))
                .run(context -> assertThat(context).doesNotHaveBean(CouchWeaveOperations.class));
    }

    @Test
    @DisplayName("should register all auto-configurations in the module imports file")
    void shouldRegisterAutoConfigurationsInImportsFile() throws java.io.IOException {
        var resource = new ClassPathResource(
                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
        assertThat(resource.exists()).isTrue();
        var content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        assertThat(content).contains(CouchWeaveClientAutoConfiguration.class.getName());
        assertThat(content).contains(CouchWeaveMappingAutoConfiguration.class.getName());
        assertThat(content).contains(CouchWeaveOperationsAutoConfiguration.class.getName());
        assertThat(content).contains(CouchWeaveRepositoriesAutoConfiguration.class.getName());
    }

    static class CustomSettingsConfig {
        static final CouchDbClientSettings settings =
                new CouchDbClientSettings(java.net.URI.create("http://localhost:5984"), "custom_db");

        @Bean
        CouchDbClientSettings couchDbClientSettings() {
            return settings;
        }
    }

    static class CustomBuilderConfig {
        static final RestClient.Builder builder = RestClient.builder();

        @Bean
        RestClient.Builder couchWeaveRestClientBuilder() {
            return builder;
        }
    }

    static class CustomConversionsConfig {
        static final CouchWeaveCustomConversions conversions = new CouchWeaveCustomConversions(java.util.List.of());

        @Bean
        CouchWeaveCustomConversions couchWeaveCustomConversions() {
            return conversions;
        }
    }

    static class CustomConverterConfig {
        static final CouchWeaveConverter converter = new MappingCouchWeaveConverter(
                new CouchMappingContext("custom_db", false), new CouchWeaveCustomConversions(java.util.List.of()));

        @Bean
        CouchWeaveConverter couchWeaveConverter() {
            return converter;
        }
    }

    static class CustomOperationsConfig {
        static final CouchWeaveOperations operations = org.mockito.Mockito.mock(CouchWeaveOperations.class);

        @Bean
        CouchWeaveOperations couchWeaveOperations() {
            return operations;
        }
    }
}
