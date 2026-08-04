package io.github.marcelorodrigo.couchweave.client.internal;

record CouchDbWriteResult(String documentId, String revision) {

    CouchDbWriteResult {
        requireText(documentId, "documentId");
        requireText(revision, "revision");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
