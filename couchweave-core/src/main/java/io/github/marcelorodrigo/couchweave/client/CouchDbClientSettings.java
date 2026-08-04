package io.github.marcelorodrigo.couchweave.client;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable connection settings for one CouchDB server and its default database.
 *
 * @param serverUri the absolute HTTP(S) URI of the CouchDB server
 * @param database the default CouchDB database name
 * @param username the optional basic-auth username
 * @param password the optional basic-auth password
 * @param connectTimeout the timeout for establishing HTTP connections
 * @param readTimeout the timeout for receiving an HTTP response
 */
public record CouchDbClientSettings(
        URI serverUri,
        String database,
        String username,
        String password,
        Duration connectTimeout,
        Duration readTimeout) {

    /** Default timeout used while establishing a connection to CouchDB. */
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);

    /** Default timeout used while waiting for a CouchDB response. */
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

    /** CouchDB database-name grammar accepted by the client. */
    private static final Pattern DATABASE_NAME = Pattern.compile("[a-z][a-z0-9_$()+\\-/]*");

    /** Maximum database-name length permitted by CouchDB. */
    private static final int MAXIMUM_DATABASE_NAME_LENGTH = 238;

    /** Validates all record components before making the settings instance available. */
    public CouchDbClientSettings {
        validateServerUri(serverUri);
        validateDatabase(database);
        validateCredentials(username, password);
        validateTimeout(connectTimeout, "connectTimeout");
        validateTimeout(readTimeout, "readTimeout");
    }

    /**
     * Creates settings with no credentials and the standard client timeouts.
     *
     * @param serverUri absolute HTTP(S) URI of the CouchDB server
     * @param database default database name
     */
    public CouchDbClientSettings(URI serverUri, String database) {
        this(serverUri, database, null, null, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     * Returns whether requests should include HTTP basic authentication.
     *
     * @return {@code true} when a complete credential pair is configured
     */
    public boolean hasCredentials() {
        return username != null;
    }

    @Override
    public String toString() {
        return "CouchDbClientSettings[serverUri=%s, database=%s, username=%s, password=<redacted>, connectTimeout=%s, readTimeout=%s]"
                .formatted(serverUri, database, username, connectTimeout, readTimeout);
    }

    /**
     * Ensures the server URI is safe to use as the base for generated requests.
     *
     * @param serverUri URI to validate
     */
    private static void validateServerUri(URI serverUri) {
        Objects.requireNonNull(serverUri, "serverUri must not be null");
        var scheme = serverUri.getScheme();
        if (!serverUri.isAbsolute()
                || scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                || serverUri.getHost() == null
                || serverUri.getRawUserInfo() != null
                || serverUri.getRawQuery() != null
                || serverUri.getRawFragment() != null) {
            throw new IllegalArgumentException(
                    "serverUri must be an absolute HTTP(S) URI without credentials, query, or fragment");
        }
    }

    /**
     * Ensures the configured database name satisfies CouchDB naming rules.
     *
     * @param database database name to validate
     */
    private static void validateDatabase(String database) {
        if (database == null || database.isBlank()) {
            throw new IllegalArgumentException("database must not be blank");
        }
        if (database.length() > MAXIMUM_DATABASE_NAME_LENGTH
                || !DATABASE_NAME.matcher(database).matches()) {
            throw new IllegalArgumentException("database must be a legal CouchDB database name");
        }
    }

    /**
     * Ensures credentials are either both absent or a complete, non-blank pair.
     *
     * @param username configured username
     * @param password configured password
     */
    private static void validateCredentials(String username, String password) {
        if ((username == null) != (password == null)) {
            throw new IllegalArgumentException("username and password must be configured together");
        }
        if (username != null && (username.isBlank() || password.isBlank())) {
            throw new IllegalArgumentException("username and password must not be blank when configured");
        }
    }

    /**
     * Ensures a timeout is present and represents a positive duration.
     *
     * @param timeout duration to validate
     * @param propertyName setting name used in validation errors
     */
    private static void validateTimeout(Duration timeout, String propertyName) {
        Objects.requireNonNull(timeout, propertyName + " must not be null");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(propertyName + " must be positive");
        }
    }
}
