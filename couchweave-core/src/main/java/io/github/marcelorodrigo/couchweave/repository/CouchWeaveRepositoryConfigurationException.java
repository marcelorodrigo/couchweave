package io.github.marcelorodrigo.couchweave.repository;

/** Indicates that a CouchWeave repository cannot be configured or started. */
public final class CouchWeaveRepositoryConfigurationException extends RuntimeException {

    /** Creates an exception with a configuration failure message. */
    public CouchWeaveRepositoryConfigurationException(String message) {
        super(message);
    }

    /** Creates an exception with a configuration failure message and cause. */
    public CouchWeaveRepositoryConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
