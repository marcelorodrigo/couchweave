package io.github.marcelorodrigo.couchweave.client.internal;

import io.github.marcelorodrigo.couchweave.client.CouchDbResponseException;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Decodes successful CouchDB JSON responses and validates their required fields. */
final class CouchDbResponseDecoder {

    /** Error identifier used when a successful response has an invalid shape. */
    private static final String INVALID_RESPONSE = "invalid_response";

    /** Reason shared by failures caused by malformed successful responses. */
    private static final String UNREADABLE_SUCCESS_RESPONSE = "CouchDB returned an unreadable success response";

    /** JSON parser used to decode CouchDB response bodies. */
    private final ObjectMapper objectMapper;

    /**
     * Creates a decoder using the supplied JSON parser.
     *
     * @param objectMapper parser for CouchDB response bodies
     */
    CouchDbResponseDecoder(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Decodes a document response and verifies its CouchDB identity fields.
     *
     * @param response successful CouchDB response
     * @param context request location used in any decoding failure
     * @return decoded document object
     */
    JsonNode decodeDocument(CouchDbResponse response, CouchDbRequestContext context) {
        var document = decodeObject(response, context);
        requireText(document, "_id", response, context);
        requireText(document, "_rev", response, context);
        return document;
    }

    /**
     * Decodes the acknowledgement returned after a write or delete.
     *
     * @param response successful CouchDB response
     * @param context request location used in any decoding failure
     * @return decoded document identifier and revision
     */
    CouchDbWriteResult decodeWriteResult(CouchDbResponse response, CouchDbRequestContext context) {
        var body = decodeObject(response, context);
        var ok = body.get("ok");
        if (ok == null || !ok.isBoolean() || !ok.booleanValue()) {
            throw invalidResponse(response, context, new IllegalArgumentException("ok must be true"));
        }
        return new CouchDbWriteResult(
                requireText(body, "id", response, context), requireText(body, "rev", response, context));
    }

    /**
     * Parses a response body and requires its top-level value to be a JSON object.
     *
     * @param response response whose body should be decoded
     * @param context request context used in a decoding failure
     * @return decoded JSON object
     */
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

    /**
     * Reads a required non-blank string field from a decoded CouchDB object.
     *
     * @param body decoded JSON object
     * @param fieldName required field name
     * @param response response used in a decoding failure
     * @param context request context used in a decoding failure
     * @return required textual field value
     */
    private String requireText(
            JsonNode body, String fieldName, CouchDbResponse response, CouchDbRequestContext context) {
        var value = body.get(fieldName);
        if (value == null || !value.isString() || value.stringValue().isBlank()) {
            throw invalidResponse(
                    response, context, new IllegalArgumentException(fieldName + " must be a non-blank string"));
        }
        return value.stringValue();
    }

    /**
     * Creates the standardized exception for an invalid successful response.
     *
     * @param response invalid response
     * @param context request context
     * @param cause decoding or validation cause
     * @return standardized response exception
     */
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
