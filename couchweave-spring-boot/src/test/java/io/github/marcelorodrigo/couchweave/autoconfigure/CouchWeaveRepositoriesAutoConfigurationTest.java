package io.github.marcelorodrigo.couchweave.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcelorodrigo.couchweave.CouchWeaveOperations;
import io.github.marcelorodrigo.couchweave.autoconfigure.excluded.ExcludedRepository;
import io.github.marcelorodrigo.couchweave.autoconfigure.fixtures.FixturesBasePackage;
import io.github.marcelorodrigo.couchweave.autoconfigure.fixtures.PersonRepository;
import io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

class CouchWeaveRepositoriesAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    CouchWeaveClientAutoConfiguration.class,
                    CouchWeaveMappingAutoConfiguration.class,
                    CouchWeaveOperationsAutoConfiguration.class,
                    CouchWeaveRepositoriesAutoConfiguration.class))
            .withUserConfiguration(FixturesBasePackage.class)
            .withPropertyValues(
                    "spring.data.couchweave.server-uri=http://localhost:5984",
                    "spring.data.couchweave.database=couchweave_test");

    @Test
    @DisplayName("should discover repositories from the Boot auto-configuration base packages")
    void shouldDiscoverRepositoriesFromBasePackages() {
        runner.run(context -> {
            assertThat(context).hasBean("personRepository");
            assertThat(context.getBean(PersonRepository.class)).isNotNull();
        });
    }

    @Test
    @DisplayName("should not discover repositories outside the scanned base packages")
    void shouldNotDiscoverRepositoriesOutsideBasePackages() {
        runner.run(context -> assertThat(context).doesNotHaveBean(ExcludedRepository.class));
    }

    @Test
    @DisplayName("should disable repository scanning when repositories.enabled is false")
    void shouldDisableRepositoryScanning() {
        runner.withPropertyValues("spring.data.couchweave.repositories.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(PersonRepository.class);
                    assertThat(context).hasSingleBean(CouchWeaveOperations.class);
                });
    }

    @Test
    @DisplayName("should use a custom mapping context and operations for discovered repositories")
    void shouldUseCustomInfrastructureForRepositories() {
        runner.withUserConfiguration(CustomInfrastructureConfig.class).run(context -> {
            assertThat(context.getBean(CouchMappingContext.class)).isSameAs(CustomInfrastructureConfig.mappingContext);
            assertThat(context.getBean(PersonRepository.class)).isNotNull();
        });
    }

    @Test
    @DisplayName("should back off when the core repository class is absent")
    void shouldBackOffWhenRepositoryClassAbsent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CouchWeaveRepositoriesAutoConfiguration.class))
                .withUserConfiguration(FixturesBasePackage.class)
                .withClassLoader(new FilteredClassLoader(
                        io.github.marcelorodrigo.couchweave.repository.CouchWeaveRepository.class))
                .run(context -> assertThat(context).doesNotHaveBean(PersonRepository.class));
    }

    static class CustomInfrastructureConfig {
        static final CouchMappingContext mappingContext = new CouchMappingContext("custom_db", false);

        @Bean
        CouchMappingContext couchMappingContext() {
            return mappingContext;
        }

        @Bean
        CouchWeaveOperations couchWeaveOperations() {
            return org.mockito.Mockito.mock(CouchWeaveOperations.class);
        }
    }
}
