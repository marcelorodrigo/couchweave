package io.github.marcelorodrigo.couchweave.autoconfigure;

import io.github.marcelorodrigo.couchweave.repository.config.CouchWeaveRepositoryConfigurationExtension;
import io.github.marcelorodrigo.couchweave.repository.config.EnableCouchWeaveRepositories;
import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * Registers CouchWeave repository bean definitions using the Spring Boot auto-configuration base
 * packages instead of a static package declaration.
 */
public class CouchWeaveRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

    @Override
    protected Class<? extends java.lang.annotation.Annotation> getAnnotation() {
        return EnableCouchWeaveRepositories.class;
    }

    @Override
    protected Class<?> getConfiguration() {
        return CouchWeaveRepositoriesConfiguration.class;
    }

    @Override
    protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
        return new CouchWeaveRepositoryConfigurationExtension();
    }

    @Override
    protected org.springframework.data.repository.config.BootstrapMode getBootstrapMode() {
        return org.springframework.data.repository.config.BootstrapMode.DEFAULT;
    }

    @EnableCouchWeaveRepositories
    static class CouchWeaveRepositoriesConfiguration {}
}
