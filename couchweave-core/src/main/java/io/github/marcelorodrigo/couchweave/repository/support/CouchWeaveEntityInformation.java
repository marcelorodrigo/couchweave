package io.github.marcelorodrigo.couchweave.repository.support;

import io.github.marcelorodrigo.couchweave.mapping.CouchPersistentEntity;
import java.util.Objects;
import org.springframework.data.repository.core.EntityInformation;

/**
 * Adapts CouchWeave persistent metadata to Spring Data repository metadata.
 *
 * @param <T> the document type
 * @param <I> the document identifier type
 */
public class CouchWeaveEntityInformation<T, I> implements EntityInformation<T, I> {

    /** The mapped CouchWeave entity metadata. */
    private final CouchPersistentEntity<T> entityMetadata;

    /** Creates entity information for resolved CouchWeave metadata.
     *
     * @param entityMetadata the resolved CouchWeave entity metadata
     */
    public CouchWeaveEntityInformation(CouchPersistentEntity<T> entityMetadata) {
        this.entityMetadata = Objects.requireNonNull(entityMetadata, "entityMetadata must not be null");
    }

    /** Delegates new-entity detection to the mapped persistent entity. */
    @Override
    public boolean isNew(T entity) {
        return entityMetadata.isNew(entity);
    }

    /** Reads the mapped identifier property from the entity. */
    @Override
    @SuppressWarnings("unchecked") // CouchWeave mapping requires String document identifiers.
    public I getId(T entity) {
        return (I) entityMetadata.getPropertyAccessor(entity).getProperty(entityMetadata.getRequiredIdProperty());
    }

    /** Returns the required CouchDB identifier type. */
    @Override
    @SuppressWarnings("unchecked") // The repository factory validates this contract before use.
    public Class<I> getIdType() {
        return (Class<I>) String.class;
    }

    /** Returns the mapped Java domain type. */
    @Override
    public Class<T> getJavaType() {
        return entityMetadata.getType();
    }
}
