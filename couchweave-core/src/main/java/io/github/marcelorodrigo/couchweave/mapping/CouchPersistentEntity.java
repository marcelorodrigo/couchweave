package io.github.marcelorodrigo.couchweave.mapping;

import org.springframework.data.mapping.model.MutablePersistentEntity;

/**
 * CouchDB-specific persistent metadata for a Java document type.
 *
 * @param <T> the represented Java type
 */
public interface CouchPersistentEntity<T> extends MutablePersistentEntity<T, CouchPersistentProperty> {

    /**
     * Returns the discriminator persisted for this document type.
     *
     * @return the nonblank discriminator
     */
    String getDiscriminator();

    /**
     * Returns the database containing this document type.
     *
     * @return the nonblank database name
     */
    String getDatabase();
}
