package io.github.marcelorodrigo.couchweave.client.internal;

import java.util.List;
import java.util.Objects;

record CouchDbRequestContext(String database, String documentId, String revision) {

    CouchDbRequestContext {
        if (database != null && database.isBlank()) {
            throw new IllegalArgumentException("database must not be blank");
        }
        if (documentId != null && documentId.isBlank()) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
        if (revision != null && revision.isBlank()) {
            throw new IllegalArgumentException("revision must not be blank");
        }
    }

    static CouchDbRequestContext fromPath(String defaultDatabase, List<String> pathSegments) {
        Objects.requireNonNull(defaultDatabase, "defaultDatabase must not be null");
        Objects.requireNonNull(pathSegments, "pathSegments must not be null");
        var database = pathSegments.isEmpty() ? defaultDatabase : pathSegments.getFirst();
        var documentId = pathSegments.size() > 1 ? pathSegments.get(1) : null;
        return new CouchDbRequestContext(database, documentId, null);
    }

    static CouchDbRequestContext forDocument(String database, String documentId, String revision) {
        return new CouchDbRequestContext(database, documentId, revision);
    }
}
