package io.github.marcelorodrigo.couchweave;

import java.util.Optional;

/**
 * Synchronous CRUD operations for mapped CouchDB documents.
 *
 * <p>This contract is independent of Spring Boot. Applications can construct a
 * {@link CouchWeaveTemplate} directly when they need a small, explicit CouchDB integration.
 */
public interface CouchWeaveOperations {

    /**
     * Saves an entity and returns its server-backed representation.
     *
     * @param entity entity to create or replace
     * @param <T> entity type
     * @return reconstructed entity containing server-generated identity metadata
     */
    <T> T save(T entity);

    /**
     * Finds an entity by its CouchDB document identifier.
     *
     * @param id document identifier
     * @param entityType mapped entity type
     * @param <T> entity type
     * @return the entity when the document exists
     */
    <T> Optional<T> findById(String id, Class<T> entityType);

    /**
     * Checks whether a document exists.
     *
     * @param id document identifier
     * @param entityType mapped entity type
     * @return {@code true} when the document exists
     */
    boolean existsById(String id, Class<?> entityType);

    /**
     * Deletes an entity using its mapped identifier and current revision.
     *
     * @param entity entity to delete
     */
    void delete(Object entity);

    /**
     * Deletes a document by reading its current revision first.
     *
     * @param id document identifier
     * @param entityType mapped entity type
     */
    void deleteById(String id, Class<?> entityType);
}
