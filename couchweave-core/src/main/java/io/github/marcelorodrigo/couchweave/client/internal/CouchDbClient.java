package io.github.marcelorodrigo.couchweave.client.internal;

import java.util.Optional;
import tools.jackson.databind.JsonNode;

/** Internal CRUD boundary implemented by the CouchDB HTTP client. */
interface CouchDbClient {

    /**
     * Creates or replaces a document.
     *
     * @param database target database
     * @param documentId target document identifier
     * @param document JSON document to store
     * @return CouchDB-assigned document identifier and revision
     */
    CouchDbWriteResult putDocument(String database, String documentId, JsonNode document);

    /**
     * Reads a document when it exists.
     *
     * @param database source database
     * @param documentId document identifier
     * @return document contents, or an empty optional when CouchDB returns not found
     */
    Optional<JsonNode> getDocument(String database, String documentId);

    /**
     * Checks whether a document exists without downloading its body.
     *
     * @param database source database
     * @param documentId document identifier
     * @return {@code true} when CouchDB responds successfully to the existence check
     */
    boolean documentExists(String database, String documentId);

    /**
     * Deletes a document using its current revision.
     *
     * @param database target database
     * @param documentId document identifier
     * @param revision current document revision required by CouchDB
     * @return CouchDB-assigned document identifier and tombstone revision
     */
    CouchDbWriteResult deleteDocument(String database, String documentId, String revision);
}
