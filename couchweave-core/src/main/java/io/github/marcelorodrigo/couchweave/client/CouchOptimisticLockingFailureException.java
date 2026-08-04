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
        super(message, cause);
        this.database = database;
        this.documentId = documentId;
        this.revision = revision;
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
}
