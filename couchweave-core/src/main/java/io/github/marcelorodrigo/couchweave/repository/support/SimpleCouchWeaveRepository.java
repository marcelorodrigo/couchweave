package io.github.marcelorodrigo.couchweave.repository.support;

import io.github.marcelorodrigo.couchweave.CouchWeaveOperations;
import io.github.marcelorodrigo.couchweave.repository.CouchWeaveRepository;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/** Sequential, non-atomic {@link CouchWeaveRepository} adapter over CouchWeave operations. */
public class SimpleCouchWeaveRepository<T, ID> implements CouchWeaveRepository<T, ID> {

    private final CouchWeaveOperations operations;
    private final CouchWeaveEntityInformation<T, ID> entityInformation;

    /** Creates a repository backed by explicit CouchWeave operations and entity metadata. */
    public SimpleCouchWeaveRepository(
            CouchWeaveOperations operations, CouchWeaveEntityInformation<T, ID> entityInformation) {
        this.operations = Objects.requireNonNull(operations, "operations must not be null");
        this.entityInformation = Objects.requireNonNull(entityInformation, "entityInformation must not be null");
    }

    /** Saves one entity. */
    @Override
    public <S extends T> S save(S entity) {
        return operations.save(entity);
    }

    /** Saves entities sequentially in input order and stops at the first failure. */
    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
        var saved = new ArrayList<S>();
        for (var entity : entities) {
            saved.add(operations.save(entity));
        }
        return saved;
    }

    /** Finds one entity by its nonblank string identifier. */
    @Override
    public Optional<T> findById(ID id) {
        return operations.findById(asId(id), entityClass());
    }

    /** Checks one entity by its nonblank string identifier. */
    @Override
    public boolean existsById(ID id) {
        return operations.existsById(asId(id), entityClass());
    }

    /** Lists all mapped entities in the operations layer's order. */
    @Override
    public Iterable<T> findAll() {
        var result = new ArrayList<T>();
        operations.findAll(entityClass()).forEach(result::add);
        return result;
    }

    /** Finds identifiers sequentially, preserving order and duplicates while omitting misses. */
    @Override
    public Iterable<T> findAllById(Iterable<ID> ids) {
        var result = new ArrayList<T>();
        for (var id : ids) {
            findById(id).ifPresent(result::add);
        }
        return result;
    }

    /** Counts mapped entities. */
    @Override
    public long count() {
        return operations.count(entityClass());
    }

    /** Deletes by current revision using a nonblank string identifier. */
    @Override
    public void deleteById(ID id) {
        operations.deleteById(asId(id), entityClass());
    }

    /** Deletes an entity using its mapped revision. */
    @Override
    public void delete(T entity) {
        operations.delete(entity);
    }

    /** Deletes identifiers sequentially and stops at the first failure. */
    @Override
    public void deleteAllById(Iterable<? extends ID> ids) {
        for (var id : ids) {
            deleteById(id);
        }
    }

    /** Deletes entities sequentially using their revisions and stops at the first failure. */
    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        for (var entity : entities) {
            delete(entity);
        }
    }

    /** Lists and deletes all entities sequentially using their revisions. */
    @Override
    public void deleteAll() {
        deleteAll(findAll());
    }

    private Class<T> entityClass() {
        return entityInformation.getJavaType();
    }

    private String asId(ID id) {
        if (!(id instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        return value;
    }
}
