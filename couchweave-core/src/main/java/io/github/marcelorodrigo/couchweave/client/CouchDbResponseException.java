package io.github.marcelorodrigo.couchweave.client;

import org.springframework.dao.UncategorizedDataAccessException;

/** Exception raised for a CouchDB response that has no more specific Spring data-access category. */
public final class CouchDbResponseException extends UncategorizedDataAccessException {

    private final int statusCode;
    private final String error;
    private final String reason;
    private final String database;
    private final String documentId;

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

    public int statusCode() {
        return statusCode;
    }

    public String error() {
        return error;
    }

    public String reason() {
        return reason;
    }

    public String database() {
        return database;
    }

    public String documentId() {
        return documentId;
    }
}
