package io.github.marcelorodrigo.couchweave.client;

import org.springframework.dao.UncategorizedDataAccessException;

/** Exception raised for a CouchDB response that has no more specific Spring data-access category. */
public final class CouchDbResponseException extends UncategorizedDataAccessException {

    /** HTTP status returned by CouchDB. */
    private final int statusCode;

    /** CouchDB error identifier, such as {@code unauthorized} or {@code conflict}. */
    private final String error;

    /** Human-readable reason supplied by CouchDB. */
    private final String reason;

    /** Database involved in the failed request, when available. */
    private final String database;

    /** Document involved in the failed request, when available. */
    private final String documentId;

    /**
     * Creates a response failure preserving CouchDB's status, error details, and request location.
     *
     * @param message human-readable description of the failure
     * @param statusCode HTTP status returned by CouchDB
     * @param error CouchDB error identifier
     * @param reason CouchDB error reason
     * @param database database involved in the request, or {@code null}
     * @param documentId document involved in the request, or {@code null}
     * @param cause underlying response or decoding failure
     */
    public CouchDbResponseException(
            String message,
            int statusCode,
            String error,
            String reason,
            String database,
            String documentId,
            Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.error = error;
        this.reason = reason;
        this.database = database;
        this.documentId = documentId;
    }

    /**
     * Returns the HTTP status returned by CouchDB.
     *
     * @return HTTP status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Returns CouchDB's machine-readable error identifier.
     *
     * @return CouchDB error identifier
     */
    public String error() {
        return error;
    }

    /**
     * Returns CouchDB's human-readable error reason.
     *
     * @return CouchDB error reason
     */
    public String reason() {
        return reason;
    }

    /**
     * Returns the database associated with the failed request.
     *
     * @return database name, or {@code null} when unavailable
     */
    public String database() {
        return database;
    }

    /**
     * Returns the document associated with the failed request.
     *
     * @return document identifier, or {@code null} when unavailable
     */
    public String documentId() {
        return documentId;
    }
}
