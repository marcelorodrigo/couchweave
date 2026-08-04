package io.github.marcelorodrigo.couchweave.mapping;

import org.springframework.data.mapping.PersistentProperty;

/**
 * CouchDB-specific persistent metadata for a Java property.
 */
public interface CouchPersistentProperty extends PersistentProperty<CouchPersistentProperty> {

    /**
     * Returns the field name used in the stored CouchDB document.
     *
     * @return the stored field name
     */
    String getFieldName();

    /**
     * Returns whether {@link CouchField} supplies a nonempty field-name override.
     *
     * @return {@code true} when the property has an explicit stored name
     */
    boolean hasExplicitFieldName();

    /**
     * Returns whether this property stores the CouchDB revision.
     *
     * @return {@code true} when the property is annotated with {@link Revision}
     */
    boolean isRevisionProperty();
}
