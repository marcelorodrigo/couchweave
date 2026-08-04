package io.github.marcelorodrigo.couchweave.client;

import org.springframework.dao.DataRetrievalFailureException;

/** Exception raised when CouchDB reports that a requested resource does not exist. */
public final class CouchDbNotFoundException extends DataRetrievalFailureException {

    private final String database;
    private final String documentId;

    public CouchDbNotFoundException(String message, String database, String documentId, Throwable cause) {
        super(message, cause);
        this.database = database;
        this.documentId = documentId;
    }

    public String database() {
        return database;
    }

    public String documentId() {
        return documentId;
    }
}
