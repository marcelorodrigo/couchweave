package io.github.marcelorodrigo.couchweave.client.internal;

import io.github.marcelorodrigo.couchweave.client.CouchDbResponseException;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class CouchDbResponseDecoder {

    private static final String INVALID_RESPONSE = "invalid_response";
    private static final String UNREADABLE_SUCCESS_RESPONSE = "CouchDB returned an unreadable success response";

    private final ObjectMapper objectMapper;

    CouchDbResponseDecoder(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    JsonNode decodeDocument(CouchDbResponse response, CouchDbRequestContext context) {
        var document = decodeObject(response, context);
        requireText(document, "_id", response, context);
        requireText(document, "_rev", response, context);
        return document;
    }

    CouchDbWriteResult decodeWriteResult(CouchDbResponse response, CouchDbRequestContext context) {
        var body = decodeObject(response, context);
        var ok = body.get("ok");
        if (ok == null || !ok.isBoolean() || !ok.booleanValue()) {
            throw invalidResponse(response, context, new IllegalArgumentException("ok must be true"));
        }
        return new CouchDbWriteResult(
                requireText(body, "id", response, context), requireText(body, "rev", response, context));
    }

    private JsonNode decodeObject(CouchDbResponse response, CouchDbRequestContext context) {
        Objects.requireNonNull(response, "response must not be null");
        Objects.requireNonNull(context, "context must not be null");
        try {
            var body = objectMapper.readTree(response.body());
            if (body == null || !body.isObject()) {
                throw new IllegalArgumentException("response body must be a JSON object");
            }
            return body;
        } catch (JacksonException | IllegalArgumentException exception) {
            throw invalidResponse(response, context, exception);
        }
    }

    private String requireText(
            JsonNode body, String fieldName, CouchDbResponse response, CouchDbRequestContext context) {
        var value = body.get(fieldName);
        if (value == null || !value.isString() || value.stringValue().isBlank()) {
            throw invalidResponse(
                    response, context, new IllegalArgumentException(fieldName + " must be a non-blank string"));
        }
        return value.stringValue();
    }

    private CouchDbResponseException invalidResponse(
            CouchDbResponse response, CouchDbRequestContext context, Throwable cause) {
        return new CouchDbResponseException(
                UNREADABLE_SUCCESS_RESPONSE
                        + " for database '%s' and document '%s'".formatted(context.database(), context.documentId()),
                response.statusCode(),
                INVALID_RESPONSE,
                UNREADABLE_SUCCESS_RESPONSE,
                context.database(),
                context.documentId(),
                cause);
    }
}
