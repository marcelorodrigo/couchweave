package io.github.marcelorodrigo.couchweave.client.internal;

import java.util.Objects;
import org.springframework.http.HttpHeaders;

record CouchDbResponse(int statusCode, HttpHeaders headers, String body) {

    CouchDbResponse {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("statusCode must be a valid HTTP status code");
        }
        headers = HttpHeaders.readOnlyHttpHeaders(Objects.requireNonNull(headers, "headers must not be null"));
        body = Objects.requireNonNull(body, "body must not be null");
    }
}
