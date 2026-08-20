package io.github.marcelorodrigo.couchweave.repository.support;

import io.github.marcelorodrigo.couchweave.CouchWeaveOperations;
import io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext;
import io.github.marcelorodrigo.couchweave.mapping.CouchPersistentEntity;
import io.github.marcelorodrigo.couchweave.repository.CouchWeaveRepositoryConfigurationException;
import java.util.Objects;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/** Creates Spring Data repository proxies backed by CouchWeave's synchronous operations. */
public class CouchWeaveRepositoryFactory extends RepositoryFactorySupport {

    private final CouchWeaveOperations operations;
    private final CouchMappingContext mappingContext;

    /** Creates a repository factory with explicit CouchWeave dependencies. */
    public CouchWeaveRepositoryFactory(CouchWeaveOperations operations, CouchMappingContext mappingContext) {
        this.operations = Objects.requireNonNull(operations, "operations must not be null");
        this.mappingContext = Objects.requireNonNull(mappingContext, "mappingContext must not be null");
    }

    /** Validates the repository identifier and mapped document type before proxy creation. */
    @Override
    protected void validate(RepositoryMetadata metadata) {
        if (!String.class.equals(metadata.getIdType())) {
            throw new CouchWeaveRepositoryConfigurationException(
                    "CouchWeave repository '%s' for domain type '%s' declares ID type '%s'; CouchWeave repositories require ID type java.lang.String."
                            .formatted(
                                    metadata.getRepositoryInterface().getName(),
                                    metadata.getDomainType().getName(),
                                    metadata.getIdType().getName()));
        }
        try {
            if (!mappingContext.hasPersistentEntityFor(metadata.getDomainType())) {
                throw new IllegalStateException("No mapped persistent entity");
            }
            mappingContext.getRequiredPersistentEntity(metadata.getDomainType());
        } catch (RuntimeException exception) {
            throw new CouchWeaveRepositoryConfigurationException(
                    "CouchWeave repository '%s' targets domain type '%s', but no valid @CouchDocument mapping is registered."
                            .formatted(
                                    metadata.getRepositoryInterface().getName(),
                                    metadata.getDomainType().getName()),
                    exception);
        }
        super.validate(metadata);
    }

    /** Creates the sequential CouchWeave repository implementation for a repository proxy. */
    @Override
    protected Object getTargetRepository(RepositoryInformation metadata) {
        var entity = entity(metadata.getDomainType());
        return new SimpleCouchWeaveRepository<>(operations, new CouchWeaveEntityInformation<>(entity));
    }

    /** Returns the CouchWeave base implementation type. */
    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleCouchWeaveRepository.class;
    }

    /** Returns entity information backed by the resolved CouchWeave mapping. */
    @Override
    public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        return new CouchWeaveEntityInformation<>(entity(domainClass));
    }

    private <T> CouchPersistentEntity<T> entity(Class<T> domainClass) {
        if (!mappingContext.hasPersistentEntityFor(domainClass)) {
            throw new CouchWeaveRepositoryConfigurationException(
                    "CouchWeave domain type '%s' has no valid @CouchDocument mapping registered."
                            .formatted(domainClass.getName()));
        }
        // The mapping context resolves the entity for the requested domain class.
        @SuppressWarnings("unchecked")
        var entity = (CouchPersistentEntity<T>) mappingContext.getRequiredPersistentEntity(domainClass);
        return entity;
    }
}
