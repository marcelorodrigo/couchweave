package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

/**
 * Signals that the CouchDB integration-test infrastructure could not complete an operation.
 */
public final class CouchDbTestHarnessException extends RuntimeException {

    CouchDbTestHarnessException(String message) {
        super(message);
    }

    CouchDbTestHarnessException(String message, Throwable cause) {
        super(message, cause);
    }
}
