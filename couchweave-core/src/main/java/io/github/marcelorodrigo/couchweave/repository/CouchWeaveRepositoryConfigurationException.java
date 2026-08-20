package io.github.marcelorodrigo.couchweave.repository;

/** Indicates that a CouchWeave repository cannot be configured or started. */
public final class CouchWeaveRepositoryConfigurationException extends RuntimeException {

    /** Creates an exception with a configuration failure message.
     *
     * @param message the configuration failure message
     */
    public CouchWeaveRepositoryConfigurationException(String message) {
        super(message);
    }

    /** Creates an exception with a configuration failure message and cause.
     *
     * @param message the configuration failure message
     * @param cause the underlying configuration failure
     */
    public CouchWeaveRepositoryConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
