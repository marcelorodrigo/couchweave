package io.github.marcelorodrigo.couchweave.client.internal;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.web.util.UriUtils;

final class CouchDbUriBuilder {

    private final String baseUri;

    CouchDbUriBuilder(CouchDbClientSettings settings) {
        this.baseUri = withoutTrailingSlash(Objects.requireNonNull(settings, "settings must not be null")
                .serverUri()
                .toASCIIString());
    }

    URI build(List<String> pathSegments) {
        return build(pathSegments, Map.of());
    }

    URI build(List<String> pathSegments, Map<String, String> queryParameters) {
        Objects.requireNonNull(pathSegments, "pathSegments must not be null");
        Objects.requireNonNull(queryParameters, "queryParameters must not be null");

        var encodedPath = pathSegments.stream()
                .map(segment -> UriUtils.encodePathSegment(
                        Objects.requireNonNull(segment, "path segment must not be null"), StandardCharsets.UTF_8))
                .reduce((left, right) -> left + "/" + right)
                .map(path -> "/" + path)
                .orElse("");
        var encodedQuery = queryParameters.entrySet().stream()
                .map(entry -> encodeQueryParameter(entry.getKey(), entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .map(query -> "?" + query)
                .orElse("");
        return URI.create(baseUri + encodedPath + encodedQuery);
    }

    private String encodeQueryParameter(String name, String value) {
        return UriUtils.encodeQueryParam(
                        Objects.requireNonNull(name, "query parameter name must not be null"), StandardCharsets.UTF_8)
                + "="
                + UriUtils.encodeQueryParam(
                        Objects.requireNonNull(value, "query parameter value must not be null"),
                        StandardCharsets.UTF_8);
    }

    private static String withoutTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
