package io.github.marcelorodrigo.couchweave.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Synchronous CRUD repository for CouchWeave documents.
 *
 * <p>{@code I} must be exactly {@link String}; CouchWeave stores CouchDB document identifiers as
 * strings and rejects repositories using another identifier type during startup. Multi-document
 * operations are sequential and non-atomic: a failure stops subsequent work and no rollback or
 * retry is attempted.
 *
 * @param <T> the document type
 * @param <I> the document identifier type, which must be {@code String}
 */
@NoRepositoryBean
public interface CouchWeaveRepository<T, I> extends CrudRepository<T, I> {

    /** Saves entities one at a time in input order; earlier writes remain after a later failure. */
    @Override
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    /** Returns all documents with the exact mapped discriminator, without pagination or sorting guarantees. */
    @Override
    Iterable<T> findAll();

    /** Returns matching entities in input order, including duplicates, and omits missing identifiers. */
    @Override
    Iterable<T> findAllById(Iterable<I> ids);

    /** Counts documents with the exact mapped discriminator. */
    @Override
    long count();

    /** Deletes by reading the current CouchDB revision, without stale-snapshot detection. */
    @Override
    void deleteById(I id);

    /** Deletes each identifier sequentially using its current CouchDB revision. */
    @Override
    void deleteAllById(Iterable<? extends I> ids);

    /** Deletes entities sequentially using their revisions and detects stale snapshots. */
    @Override
    void deleteAll(Iterable<? extends T> entities);

    /** Lists and then deletes entities sequentially using their revisions. */
    @Override
    void deleteAll();
}
