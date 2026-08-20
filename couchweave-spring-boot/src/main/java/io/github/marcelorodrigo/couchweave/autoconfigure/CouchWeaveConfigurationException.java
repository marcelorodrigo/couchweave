package io.github.marcelorodrigo.couchweave.autoconfigure;

/**
 * Indicates that CouchWeave Spring Boot configuration is invalid.
 */
public final class CouchWeaveConfigurationException extends RuntimeException {

    /**
     * Creates an exception with a configuration failure message.
     *
     * @param message the configuration failure message
     */
    public CouchWeaveConfigurationException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a configuration failure message and cause.
     *
     * @param message the configuration failure message
     * @param cause the underlying configuration failure
     */
    public CouchWeaveConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
