package io.github.marcelorodrigo.couchweave.client.internal;

import java.util.List;
import java.util.Objects;

/**
 * Sanitizable location and revision context carried through a CouchDB request.
 *
 * @param database request database, or {@code null} for server-scoped requests
 * @param documentId request document, or {@code null} when not document-scoped
 * @param revision document revision, or {@code null} when not revision-scoped
 */
record CouchDbRequestContext(String database, String documentId, String revision) {

    /** Rejects blank location components while allowing server-scoped values to remain null. */
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

    /**
     * Builds request context from URI path segments, falling back to the configured database.
     *
     * @param defaultDatabase database used when the path does not specify one
     * @param pathSegments path components after the CouchDB server URI
     * @return context derived from the path
     */
    static CouchDbRequestContext fromPath(String defaultDatabase, List<String> pathSegments) {
        Objects.requireNonNull(defaultDatabase, "defaultDatabase must not be null");
        Objects.requireNonNull(pathSegments, "pathSegments must not be null");
        var database = pathSegments.isEmpty() ? defaultDatabase : pathSegments.getFirst();
        var documentId = pathSegments.size() > 1 ? pathSegments.get(1) : null;
        return new CouchDbRequestContext(database, documentId, null);
    }

    /**
     * Builds context for an operation targeting one document.
     *
     * @param database target database
     * @param documentId target document identifier
     * @param revision document revision, when the operation uses one
     * @return document request context
     */
    static CouchDbRequestContext forDocument(String database, String documentId, String revision) {
        return new CouchDbRequestContext(database, documentId, revision);
    }
}
