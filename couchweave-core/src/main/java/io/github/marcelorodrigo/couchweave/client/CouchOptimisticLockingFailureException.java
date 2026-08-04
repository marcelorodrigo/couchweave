package io.github.marcelorodrigo.couchweave.client;

import org.springframework.dao.OptimisticLockingFailureException;

/** Exception raised when CouchDB rejects a stale document revision. */
public final class CouchOptimisticLockingFailureException extends OptimisticLockingFailureException {

    private final String database;
    private final String documentId;
    private final String revision;

    public CouchOptimisticLockingFailureException(
            String message, String database, String documentId, String revision, Throwable cause) {
        super(message, cause);
        this.database = database;
        this.documentId = documentId;
        this.revision = revision;
    }

    public String database() {
        return database;
    }

    public String documentId() {
        return documentId;
    }

    public String revision() {
        return revision;
    }
}
