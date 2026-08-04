package io.github.marcelorodrigo.couchweave.client;

import org.springframework.dao.PermissionDeniedDataAccessException;

/** Exception raised when CouchDB rejects the configured credentials. */
public final class CouchDbAuthenticationException extends PermissionDeniedDataAccessException {

    /** Database involved in the rejected request, when the request was database-scoped. */
    private final String database;

    /** Document involved in the rejected request, when the request was document-scoped. */
    private final String documentId;

    /**
     * Creates an authentication failure with the request location and original cause.
     *
     * @param message human-readable description of the failure
     * @param database database involved in the request, or {@code null}
     * @param documentId document involved in the request, or {@code null}
     * @param cause underlying CouchDB or transport failure
     */
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
