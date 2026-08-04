package io.github.marcelorodrigo.couchweave.client.internal;

import io.github.marcelorodrigo.couchweave.client.CouchDbAuthenticationException;
import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import io.github.marcelorodrigo.couchweave.client.CouchDbNotFoundException;
import io.github.marcelorodrigo.couchweave.client.CouchDbResponseException;
import io.github.marcelorodrigo.couchweave.client.CouchOptimisticLockingFailureException;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Maps CouchDB and transport failures to Spring's data-access exception hierarchy. */
final class CouchDbFailureTranslator {

    /** Fallback CouchDB error identifier for malformed error bodies. */
    private static final String UNKNOWN_ERROR = "unknown";

    /** Fallback reason used when CouchDB's error body cannot be decoded. */
    private static final String MALFORMED_REASON = "CouchDB returned an unreadable error response";

    /** Replacement used to keep credentials out of exception messages and causes. */
    private static final String REDACTED = "<redacted>";

    /** JSON parser used to inspect CouchDB error bodies. */
    private final ObjectMapper objectMapper;

    /** Credential values that must be removed from diagnostic text. */
    private final List<String> sensitiveValues;

    /**
     * Creates a translator using a default JSON parser.
     *
     * @param settings client settings whose credentials must be protected
     */
    CouchDbFailureTranslator(CouchDbClientSettings settings) {
        this(settings, new ObjectMapper());
    }

    /**
     * Creates a translator with an explicit JSON parser.
     *
     * @param settings client settings whose credentials must be protected
     * @param objectMapper parser for CouchDB error bodies
     */
    CouchDbFailureTranslator(CouchDbClientSettings settings, ObjectMapper objectMapper) {
        Objects.requireNonNull(settings, "settings must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.sensitiveValues =
                settings.hasCredentials() ? List.of(settings.username(), settings.password()) : List.of();
    }

    /**
     * Converts an HTTP error response to the most specific applicable data-access exception.
     *
     * @param response CouchDB response with a non-success status
     * @param context request location and revision
     * @return translated Spring data-access exception
     */
    DataAccessException translate(CouchDbResponse response, CouchDbRequestContext context) {
        Objects.requireNonNull(response, "response must not be null");
        Objects.requireNonNull(context, "context must not be null");
        context = sanitize(context);
        var error = decode(response.body());
        var message = message(response.statusCode(), error, context);

        return switch (response.statusCode()) {
            case 400 -> new InvalidDataAccessResourceUsageException(message, error.cause());
            case 401 ->
                new CouchDbAuthenticationException(message, context.database(), context.documentId(), error.cause());
            case 403 -> new PermissionDeniedDataAccessException(message, error.cause());
            case 404 -> new CouchDbNotFoundException(message, context.database(), context.documentId(), error.cause());
            case 409 ->
                new CouchOptimisticLockingFailureException(
                        message, context.database(), context.documentId(), context.revision(), error.cause());
            default ->
                new CouchDbResponseException(
                        message,
                        response.statusCode(),
                        error.error(),
                        error.reason(),
                        context.database(),
                        context.documentId(),
                        error.cause());
        };
    }

    /**
     * Converts a transport-level failure to a resource-access exception.
     *
     * @param exception original transport failure
     * @param context request location used in the diagnostic message
     * @return translated resource-access exception
     */
    DataAccessResourceFailureException translate(ResourceAccessException exception, CouchDbRequestContext context) {
        Objects.requireNonNull(exception, "exception must not be null");
        Objects.requireNonNull(context, "context must not be null");
        return new DataAccessResourceFailureException(sanitize("Unable to access " + describe(context)), exception);
    }

    /**
     * Decodes CouchDB's error and reason fields, using safe fallbacks for malformed JSON.
     *
     * @param body CouchDB response body
     * @return decoded error details and optional parsing cause
     */
    private DecodedError decode(String body) {
        try {
            var root = objectMapper.readTree(body);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("CouchDB error response must be a JSON object");
            }
            var error = new CouchDbErrorResponse(textValue(root, "error"), textValue(root, "reason"));
            if (error.error() == null
                    || error.error().isBlank()
                    || error.reason() == null
                    || error.reason().isBlank()) {
                throw new IllegalArgumentException("CouchDB error response must contain error and reason");
            }
            return new DecodedError(sanitize(error.error()), sanitize(error.reason()), null);
        } catch (JacksonException exception) {
            return new DecodedError(UNKNOWN_ERROR, MALFORMED_REASON, sanitizedParsingCause(body, exception));
        } catch (IllegalArgumentException exception) {
            return new DecodedError(UNKNOWN_ERROR, MALFORMED_REASON, exception);
        }
    }

    /**
     * Creates a parsing cause that cannot retain credential text from the original body.
     *
     * @param body original response body
     * @param original original JSON parsing failure
     * @return sanitized parsing cause
     */
    private Throwable sanitizedParsingCause(String body, JacksonException original) {
        var sanitizedBody = sanitize(body);
        if (sanitizedBody.equals(body)) {
            return original;
        }
        try {
            objectMapper.readTree(sanitizedBody);
            return new IllegalArgumentException("CouchDB error response could not be decoded");
        } catch (JacksonException sanitized) {
            return sanitized;
        }
    }

    /**
     * Builds a sanitized diagnostic message containing status, location, and CouchDB details.
     *
     * @param statusCode HTTP status returned by CouchDB
     * @param error decoded CouchDB error details
     * @param context sanitized request context
     * @return sanitized diagnostic message
     */
    private String message(int statusCode, DecodedError error, CouchDbRequestContext context) {
        return sanitize("CouchDB request failed with HTTP %d for %s (error=%s, reason=%s)"
                .formatted(statusCode, describe(context), error.error(), error.reason()));
    }

    /**
     * Returns a JSON string field or null when the field is absent or not textual.
     *
     * @param root JSON object to inspect
     * @param fieldName field to read
     * @return textual field value, or {@code null}
     */
    private String textValue(JsonNode root, String fieldName) {
        var value = root.get(fieldName);
        return value != null && value.isString() ? value.stringValue() : null;
    }

    /**
     * Formats the database, document, and revision represented by a request context.
     *
     * @param context request context to describe
     * @return human-readable request description
     */
    private String describe(CouchDbRequestContext context) {
        var description =
                context.database() == null ? "the CouchDB server" : "database '%s'".formatted(context.database());
        if (context.documentId() != null) {
            description += " and document '%s'".formatted(context.documentId());
        }
        if (context.revision() != null) {
            description += " at revision '%s'".formatted(context.revision());
        }
        return description;
    }

    /**
     * Replaces every configured credential value in diagnostic text.
     *
     * @param value text that may contain credentials
     * @return text with credentials replaced by a redaction marker
     */
    private String sanitize(String value) {
        var sanitized = value;
        for (var sensitiveValue : sensitiveValues) {
            sanitized = sanitized.replace(sensitiveValue, REDACTED);
        }
        return sanitized;
    }

    /**
     * Returns a request context with all potentially sensitive components redacted.
     *
     * @param context context that may contain credentials
     * @return sanitized request context
     */
    private CouchDbRequestContext sanitize(CouchDbRequestContext context) {
        return new CouchDbRequestContext(
                context.database() == null ? null : sanitize(context.database()),
                context.documentId() == null ? null : sanitize(context.documentId()),
                context.revision() == null ? null : sanitize(context.revision()));
    }

    /**
     * Decoded CouchDB error details plus the cause that should be attached to the exception.
     *
     * @param error decoded machine-readable error identifier
     * @param reason decoded human-readable error reason
     * @param cause parsing or validation cause, when available
     */
    private record DecodedError(String error, String reason, Throwable cause) {}
}
