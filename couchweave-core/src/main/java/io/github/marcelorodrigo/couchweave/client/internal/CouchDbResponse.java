package io.github.marcelorodrigo.couchweave.client.internal;

import java.util.Objects;
import org.springframework.http.HttpHeaders;

/**
 * Buffered HTTP response used by the CouchDB response decoders and failure translator.
 *
 * @param statusCode HTTP status returned by CouchDB
 * @param headers response headers
 * @param body decoded UTF-8 response body
 */
record CouchDbResponse(int statusCode, HttpHeaders headers, String body) {

    /** Validates the status and makes response headers immutable before storage. */
    CouchDbResponse {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("statusCode must be a valid HTTP status code");
        }
        headers = HttpHeaders.readOnlyHttpHeaders(Objects.requireNonNull(headers, "headers must not be null"));
        body = Objects.requireNonNull(body, "body must not be null");
    }
}
