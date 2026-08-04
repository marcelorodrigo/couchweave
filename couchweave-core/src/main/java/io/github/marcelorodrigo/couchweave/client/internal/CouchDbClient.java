package io.github.marcelorodrigo.couchweave.client.internal;

import java.util.Optional;
import tools.jackson.databind.JsonNode;

interface CouchDbClient {

    CouchDbWriteResult putDocument(String database, String documentId, JsonNode document);

    Optional<JsonNode> getDocument(String database, String documentId);

    boolean documentExists(String database, String documentId);

    CouchDbWriteResult deleteDocument(String database, String documentId, String revision);
}
