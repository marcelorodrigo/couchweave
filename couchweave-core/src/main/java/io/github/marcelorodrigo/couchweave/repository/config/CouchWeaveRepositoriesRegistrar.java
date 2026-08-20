package io.github.marcelorodrigo.couchweave.repository.config;

import java.lang.annotation.Annotation;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * Registers CouchWeave repository bean definitions for {@link EnableCouchWeaveRepositories}.
 */
public class CouchWeaveRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {

    /** Creates the registrar used by {@link EnableCouchWeaveRepositories}. */
    public CouchWeaveRepositoriesRegistrar() {}

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableCouchWeaveRepositories.class;
    }

    @Override
    protected RepositoryConfigurationExtension getExtension() {
        return new CouchWeaveRepositoryConfigurationExtension();
    }
}
