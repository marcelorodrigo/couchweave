package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import java.net.URI;
import java.util.Objects;

/**
 * Connection details for the database isolated for one CouchDB integration-test class.
 */
public record CouchDbTestDatabase(
    URI serverUri,
    URI databaseUri,
    String databaseName,
    String username,
    String password
) {

    public CouchDbTestDatabase {
        validateUri(serverUri, "server URI");
        validateUri(databaseUri, "database URI");
        validateText(databaseName, "database name");
        validateText(username, "username");
        validateText(password, "password");
    }

    private static void validateUri(URI uri, String name) {
        Objects.requireNonNull(uri, name + " must not be null");
        if (!uri.isAbsolute() || !"http".equals(uri.getScheme())) {
            throw new IllegalArgumentException(name + " must be an absolute HTTP URI");
        }
    }

    private static void validateText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
