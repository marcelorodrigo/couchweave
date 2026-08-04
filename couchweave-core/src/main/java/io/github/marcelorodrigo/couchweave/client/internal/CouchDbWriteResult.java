package io.github.marcelorodrigo.couchweave.client.internal;

record CouchDbWriteResult(String documentId, String revision) {

    CouchDbWriteResult {
        documentId = requireText(documentId, "documentId");
        revision = requireText(revision, "revision");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
