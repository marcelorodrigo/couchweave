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

    /** The synchronous CouchWeave operations used by repositories. */
    private final CouchWeaveOperations operations;
    /** The mapping context used to resolve persistent entities. */
    private final CouchMappingContext mappingContext;

    /** Creates a repository factory with explicit CouchWeave dependencies.
     *
     * @param operations the synchronous CouchWeave operations
     * @param mappingContext the CouchWeave mapping context
     */
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
            entity(metadata.getDomainType());
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
    public <T, I> EntityInformation<T, I> getEntityInformation(Class<T> domainClass) {
        return new CouchWeaveEntityInformation<>(entity(domainClass));
    }

    /** Resolves the persistent entity for a domain type.
     *
     * @param <T> the entity type
     * @param domainClass the domain type to resolve
     * @return the resolved CouchWeave persistent entity
     */
    private <T> CouchPersistentEntity<T> entity(Class<T> domainClass) {
        // A non-strict mapping context lazily creates the entity for annotated document types,
        // which is required when repository domain types are discovered during scanning.
        var persistentEntity = mappingContext.getPersistentEntity(domainClass);
        if (persistentEntity == null) {
            throw new CouchWeaveRepositoryConfigurationException(
                    "CouchWeave domain type '%s' has no valid @CouchDocument mapping registered."
                            .formatted(domainClass.getName()));
        }
        @SuppressWarnings("unchecked")
        var entity = (CouchPersistentEntity<T>) persistentEntity;
        return entity;
    }
}
