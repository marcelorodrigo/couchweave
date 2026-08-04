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

final class CouchDbFailureTranslator {

    private static final String UNKNOWN_ERROR = "unknown";
    private static final String MALFORMED_REASON = "CouchDB returned an unreadable error response";
    private static final String REDACTED = "<redacted>";

    private final ObjectMapper objectMapper;
    private final List<String> sensitiveValues;

    CouchDbFailureTranslator(CouchDbClientSettings settings) {
        this(settings, new ObjectMapper());
    }

    CouchDbFailureTranslator(CouchDbClientSettings settings, ObjectMapper objectMapper) {
        Objects.requireNonNull(settings, "settings must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.sensitiveValues =
                settings.hasCredentials() ? List.of(settings.username(), settings.password()) : List.of();
    }

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

    DataAccessResourceFailureException translate(ResourceAccessException exception, CouchDbRequestContext context) {
        Objects.requireNonNull(exception, "exception must not be null");
        Objects.requireNonNull(context, "context must not be null");
        return new DataAccessResourceFailureException(sanitize("Unable to access " + describe(context)), exception);
    }

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

    private String message(int statusCode, DecodedError error, CouchDbRequestContext context) {
        return sanitize("CouchDB request failed with HTTP %d for %s (error=%s, reason=%s)"
                .formatted(statusCode, describe(context), error.error(), error.reason()));
    }

    private String textValue(JsonNode root, String fieldName) {
        var value = root.get(fieldName);
        return value != null && value.isString() ? value.stringValue() : null;
    }

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

    private String sanitize(String value) {
        var sanitized = value;
        for (var sensitiveValue : sensitiveValues) {
            sanitized = sanitized.replace(sensitiveValue, REDACTED);
        }
        return sanitized;
    }

    private CouchDbRequestContext sanitize(CouchDbRequestContext context) {
        return new CouchDbRequestContext(
                context.database() == null ? null : sanitize(context.database()),
                context.documentId() == null ? null : sanitize(context.documentId()),
                context.revision() == null ? null : sanitize(context.revision()));
    }

    private record DecodedError(String error, String reason, Throwable cause) {}
}
