package io.github.marcelorodrigo.couchweave.client;

import org.springframework.dao.OptimisticLockingFailureException;

/** Exception raised when CouchDB rejects a stale document revision. */
public final class CouchOptimisticLockingFailureException extends OptimisticLockingFailureException {

    /** Database containing the document whose revision was stale. */
    private final String database;

    /** Identifier of the document whose revision was stale. */
    private final String documentId;

    /** Revision supplied by the caller and rejected by CouchDB. */
    private final String revision;

    /** Mapped Java entity type affected by the conflict, or {@code null} when not available. */
    private final Class<?> entityType;

    /**
     * Creates a stale-revision failure with the affected document context.
     *
     * @param message human-readable description of the conflict
     * @param database database containing the document
     * @param documentId document identifier
     * @param revision revision supplied by the caller
     * @param cause underlying CouchDB response failure
     */
    public CouchOptimisticLockingFailureException(
            String message, String database, String documentId, String revision, Throwable cause) {
        this(message, database, documentId, revision, null, cause);
    }

    /**
     * Creates a stale-revision failure with the affected document context and mapped entity type.
     *
     * @param message human-readable description of the conflict
     * @param database database containing the document
     * @param documentId document identifier
     * @param revision revision supplied by the caller
     * @param entityType mapped Java entity type, or {@code null} when not available
     * @param cause underlying CouchDB response failure
     */
    public CouchOptimisticLockingFailureException(
            String message, String database, String documentId, String revision, Class<?> entityType, Throwable cause) {
        super(message, cause);
        this.database = database;
        this.documentId = documentId;
        this.revision = revision;
        this.entityType = entityType;
    }

    /**
     * Returns a copy of this failure enriched with the mapped entity type.
     *
     * <p>When the failure already carries an entity type, this instance is returned unchanged.
     *
     * @param entityType mapped Java entity type affected by the conflict
     * @return an exception carrying the supplied entity type
     */
    public CouchOptimisticLockingFailureException withEntityType(Class<?> entityType) {
        if (this.entityType != null || entityType == null) {
            return this;
        }
        return new CouchOptimisticLockingFailureException(
                getMessage(), database, documentId, revision, entityType, getCause());
    }

    /**
     * Returns the database containing the conflicting document.
     *
     * @return database name
     */
    public String database() {
        return database;
    }

    /**
     * Returns the identifier of the conflicting document.
     *
     * @return document identifier
     */
    public String documentId() {
        return documentId;
    }

    /**
     * Returns the stale revision supplied by the caller.
     *
     * @return rejected document revision
     */
    public String revision() {
        return revision;
    }

    /**
     * Returns the mapped Java entity type affected by the conflict.
     *
     * @return mapped entity type, or {@code null} when the failure originates from the raw client
     */
    public Class<?> entityType() {
        return entityType;
    }
}
