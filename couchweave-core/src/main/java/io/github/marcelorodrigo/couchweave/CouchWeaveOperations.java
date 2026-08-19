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
     * <p>A {@code null} mapped identifier creates a new document with a server-generated identifier. A
     * nonblank identifier paired with a nonblank revision performs a revision-conditional update. A
     * nonblank identifier without a revision is only accepted when no document exists yet; attempting to
     * replace an existing document without a revision fails before any write. A CouchDB revision conflict
     * surfaces as {@link io.github.marcelorodrigo.couchweave.client.CouchOptimisticLockingFailureException}.
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
     * <p>The entity must carry a mapped nonblank identifier and revision; otherwise the call fails before
     * any request. A CouchDB revision conflict, including a stale caller snapshot, surfaces as
     * {@link io.github.marcelorodrigo.couchweave.client.CouchOptimisticLockingFailureException}.
     *
     * @param entity entity to delete
     */
    void delete(Object entity);

    /**
     * Deletes a document by reading its current revision first.
     *
     * <p>This convenience operation reads the latest revision and therefore does not enforce a caller
     * snapshot; it is not subject to stale-delete detection.
     *
     * @param id document identifier
     * @param entityType mapped entity type
     */
    void deleteById(String id, Class<?> entityType);
}
