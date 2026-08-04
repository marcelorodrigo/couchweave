package io.github.marcelorodrigo.couchweave.client;

import org.springframework.dao.PermissionDeniedDataAccessException;

/** Exception raised when CouchDB rejects the configured credentials. */
public final class CouchDbAuthenticationException extends PermissionDeniedDataAccessException {

    private final String database;
    private final String documentId;

    public CouchDbAuthenticationException(String message, String database, String documentId, Throwable cause) {
        super(message, cause);
        this.database = database;
        this.documentId = documentId;
    }

    /**
     * Returns the database associated with the failed request.
     *
     * @return the database name, or {@code null} when the request was server-scoped
     */
    public String database() {
        return database;
    }

    /**
     * Returns the document associated with the failed request.
     *
     * @return the document identifier, or {@code null} when the request was not document-scoped
     */
    public String documentId() {
        return documentId;
    }
}
