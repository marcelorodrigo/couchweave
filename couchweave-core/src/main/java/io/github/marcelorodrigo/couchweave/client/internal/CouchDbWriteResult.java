package io.github.marcelorodrigo.couchweave.client.internal;

/**
 * Identifiers returned by CouchDB after a successful write or delete operation.
 *
 * @param documentId document identifier returned by CouchDB
 * @param revision new document revision returned by CouchDB
 */
public record CouchDbWriteResult(String documentId, String revision) {

    /** Ensures CouchDB returned both required write-result values. */
    public CouchDbWriteResult {
        requireText(documentId, "documentId");
        requireText(revision, "revision");
    }

    /**
     * Rejects missing or blank values in a decoded write result.
     *
     * @param value value to validate
     * @param name field name used in the validation error
     */
    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
