package io.github.marcelorodrigo.couchweave.repository.config;

import io.github.marcelorodrigo.couchweave.repository.CouchWeaveRepository;
import io.github.marcelorodrigo.couchweave.repository.support.CouchWeaveRepositoryFactoryBean;
import io.github.marcelorodrigo.couchweave.repository.support.SimpleCouchWeaveRepository;
import java.util.Collection;
import java.util.Collections;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.core.RepositoryMetadata;

/** Provides Spring Data repository discovery metadata for CouchWeave repositories. */
public class CouchWeaveRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

    /** Returns the module display name. */
    @Override
    public String getModuleName() {
        return "CouchWeave";
    }

    /** Returns the module configuration prefix. */
    @Override
    protected String getModulePrefix() {
        return "couchweave";
    }

    /** Returns the marker repository type. */
    @Override
    protected Collection<Class<?>> getIdentifyingTypes() {
        return Collections.singleton(CouchWeaveRepository.class);
    }

    /** Returns no identifying annotations. */
    @Override
    protected Collection<Class<? extends java.lang.annotation.Annotation>> getIdentifyingAnnotations() {
        return Collections.emptySet();
    }

    /** Returns the factory bean class name used for discovered repositories. */
    @Override
    public String getRepositoryFactoryBeanClassName() {
        return CouchWeaveRepositoryFactoryBean.class.getName();
    }

    /** Returns the repository base class name used for discovered repositories. */
    @Override
    public String getRepositoryBaseClassName() {
        return SimpleCouchWeaveRepository.class.getName();
    }

    /** Claims only repositories explicitly extending CouchWeaveRepository. */
    @Override
    protected boolean isStrictRepositoryCandidate(RepositoryMetadata metadata) {
        return CouchWeaveRepository.class.isAssignableFrom(metadata.getRepositoryInterface());
    }
}
