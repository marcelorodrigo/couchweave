package io.github.marcelorodrigo.couchweave.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import io.github.marcelorodrigo.couchweave.mapping.CouchDocument;
import io.github.marcelorodrigo.couchweave.mapping.Revision;
import io.github.marcelorodrigo.couchweave.repository.config.EnableCouchWeaveRepositories;
import io.github.marcelorodrigo.couchweave.testsupport.couchdb.CouchDbIntegrationTest;
import io.github.marcelorodrigo.couchweave.testsupport.couchdb.CouchDbTestDatabase;
import java.time.Duration;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;

@CouchDbIntegrationTest
class CouchWeaveRepositoryAutoConfigurationIT {

    @Test
    @DisplayName("should perform real CRUD through an enabled repository")
    void shouldPerformCrudThroughEnabledRepository(CouchDbTestDatabase database) {
        var settings = new CouchDbClientSettings(
                database.serverUri(),
                database.databaseName(),
                database.username(),
                database.password(),
                Duration.ofSeconds(5),
                Duration.ofSeconds(5));
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(CouchDbClientSettings.class, () -> settings);
            context.register(EnableConfig.class);
            context.refresh();

            var repository = context.getBean(AutoBookRepository.class);
            repository.deleteAll();

            var saved = repository.save(new AutoBook(null, null, "Enabled"));
            assertThat(saved.id()).isNotBlank();
            assertThat(saved.revision()).isNotBlank();

            var found = repository.findById(saved.id());
            assertThat(found).contains(saved);

            var updated = repository.save(new AutoBook(saved.id(), saved.revision(), "Updated"));
            assertThat(repository.findById(saved.id())).contains(updated);

            repository.deleteById(saved.id());
            assertThat(repository.existsById(saved.id())).isFalse();
        }
    }

    @Configuration
    @EnableCouchWeaveRepositories(basePackageClasses = CouchWeaveRepositoryAutoConfigurationIT.class)
    static class EnableConfig {}

    @CouchDocument(type = "it-autoconfig-book")
    record AutoBook(@Id String id, @Revision String revision, String title) {

        AutoBook {
            Objects.requireNonNull(title);
        }
    }
}

interface AutoBookRepository extends CouchWeaveRepository<CouchWeaveRepositoryAutoConfigurationIT.AutoBook, String> {}
