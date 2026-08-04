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

    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

    private static final Pattern DATABASE_NAME = Pattern.compile("[a-z][a-z0-9_$()+\\-/]*");
    private static final int MAXIMUM_DATABASE_NAME_LENGTH = 238;

    public CouchDbClientSettings {
        validateServerUri(serverUri);
        validateDatabase(database);
        validateCredentials(username, password);
        validateTimeout(connectTimeout, "connectTimeout");
        validateTimeout(readTimeout, "readTimeout");
    }

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

    private static void validateDatabase(String database) {
        if (database == null || database.isBlank()) {
            throw new IllegalArgumentException("database must not be blank");
        }
        if (database.length() > MAXIMUM_DATABASE_NAME_LENGTH
                || !DATABASE_NAME.matcher(database).matches()) {
            throw new IllegalArgumentException("database must be a legal CouchDB database name");
        }
    }

    private static void validateCredentials(String username, String password) {
        if ((username == null) != (password == null)) {
            throw new IllegalArgumentException("username and password must be configured together");
        }
        if (username != null && (username.isBlank() || password.isBlank())) {
            throw new IllegalArgumentException("username and password must not be blank when configured");
        }
    }

    private static void validateTimeout(Duration timeout, String propertyName) {
        Objects.requireNonNull(timeout, propertyName + " must not be null");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(propertyName + " must be positive");
        }
    }
}
