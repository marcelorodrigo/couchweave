package io.github.marcelorodrigo.couchweave.client;

import org.springframework.dao.DataRetrievalFailureException;

/** Exception raised when CouchDB reports that a requested resource does not exist. */
public final class CouchDbNotFoundException extends DataRetrievalFailureException {

    /** Database containing the resource that could not be found. */
    private final String database;

    /** Document that could not be found, when the request was document-scoped. */
    private final String documentId;

    /**
     * Creates a not-found failure with the request location and original cause.
     *
     * @param message human-readable description of the missing resource
     * @param database database involved in the request, or {@code null}
     * @param documentId document involved in the request, or {@code null}
     * @param cause underlying CouchDB response failure
     */
    public CouchDbNotFoundException(String message, String database, String documentId, Throwable cause) {
        super(message, cause);
        this.database = database;
        this.documentId = documentId;
    }

    /**
     * Returns the database associated with the missing resource.
     *
     * @return database name, or {@code null} when the request was server-scoped
     */
    public String database() {
        return database;
    }

    /**
     * Returns the identifier of the missing document.
     *
     * @return document identifier, or {@code null} when the request was not document-scoped
     */
    public String documentId() {
        return documentId;
    }
}
