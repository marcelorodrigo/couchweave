package io.github.marcelorodrigo.couchweave.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Activates CouchWeave repository discovery from the Spring Boot auto-configuration base packages.
 *
 * <p>The imported registrar reuses the core {@link io.github.marcelorodrigo.couchweave.repository.config.CouchWeaveRepositoryConfigurationExtension}
 * but obtains the scan packages from {@code AutoConfigurationPackages}, so repositories are discovered
 * from the application's base packages rather than a static package declaration. Scanning is disabled
 * when {@code spring.data.couchweave.repositories.enabled} is {@code false}.
 */
@AutoConfiguration(
        after = {
            CouchWeaveClientAutoConfiguration.class,
            CouchWeaveMappingAutoConfiguration.class,
            CouchWeaveOperationsAutoConfiguration.class
        })
@ConditionalOnClass(name = "io.github.marcelorodrigo.couchweave.repository.CouchWeaveRepository")
@ConditionalOnProperty(
        prefix = "spring.data.couchweave.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import(CouchWeaveRepositoriesRegistrar.class)
public class CouchWeaveRepositoriesAutoConfiguration {}
