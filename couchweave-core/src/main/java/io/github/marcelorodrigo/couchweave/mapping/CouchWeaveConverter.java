package io.github.marcelorodrigo.couchweave.mapping;

import org.springframework.core.convert.ConversionService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Converts CouchWeave document entities to and from Jackson trees.
 */
public interface CouchWeaveConverter {

    /**
     * Converts an entity into a new CouchDB document tree.
     *
     * @param source the mapped entity
     * @return a new document tree
     */
    ObjectNode write(Object source);

    /**
     * Reconstructs an entity from a CouchDB document tree.
     *
     * @param targetType the mapped entity type
     * @param source the source document
     * @param <T> the entity type
     * @return the reconstructed entity
     */
    <T> T read(Class<T> targetType, JsonNode source);

    /**
     * Returns the mapping context used by this converter.
     *
     * @return the mapping context
     */
    CouchMappingContext getMappingContext();

    /**
     * Returns the conversion service containing CouchWeave custom conversions.
     *
     * @return the conversion service
     */
    ConversionService getConversionService();
}
