package io.github.marcelorodrigo.couchweave.client.internal;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.web.util.UriUtils;

/** Encodes CouchDB path and query components against the configured server URI. */
final class CouchDbUriBuilder {

    /** Delimiter used when joining encoded path segments. */
    private static final String PATH_DELIMITER = "/";

    /** Server URI without a trailing slash, suitable for appending request paths. */
    private final String baseUri;

    /**
     * Creates a URI builder from validated client settings.
     *
     * @param settings validated CouchDB connection settings
     */
    CouchDbUriBuilder(CouchDbClientSettings settings) {
        this.baseUri = withoutTrailingSlash(Objects.requireNonNull(settings, "settings must not be null")
                .serverUri()
                .toASCIIString());
    }

    /**
     * Builds a URI with encoded path segments and no query parameters.
     *
     * @param pathSegments path components to append to the server URI
     * @return encoded request URI
     */
    URI build(List<String> pathSegments) {
        return build(pathSegments, Map.of());
    }

    /**
     * Builds a URI with encoded path segments and query parameters.
     *
     * @param pathSegments path components to append to the server URI
     * @param queryParameters query parameters to encode and append
     * @return encoded request URI
     */
    URI build(List<String> pathSegments, Map<String, String> queryParameters) {
        Objects.requireNonNull(pathSegments, "pathSegments must not be null");
        Objects.requireNonNull(queryParameters, "queryParameters must not be null");

        var encodedPath = pathSegments.stream()
                .map(segment -> UriUtils.encodePathSegment(
                        Objects.requireNonNull(segment, "path segment must not be null"), StandardCharsets.UTF_8))
                .reduce((left, right) -> left + PATH_DELIMITER + right)
                .map(path -> PATH_DELIMITER + path)
                .orElse("");
        var encodedQuery = queryParameters.entrySet().stream()
                .map(entry -> encodeQueryParameter(entry.getKey(), entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .map(query -> "?" + query)
                .orElse("");
        return URI.create(baseUri + encodedPath + encodedQuery);
    }

    /**
     * Encodes one query parameter name-value pair using UTF-8.
     *
     * @param name query parameter name
     * @param value query parameter value
     * @return encoded name-value pair
     */
    private String encodeQueryParameter(String name, String value) {
        return UriUtils.encodeQueryParam(
                        Objects.requireNonNull(name, "query parameter name must not be null"), StandardCharsets.UTF_8)
                + "="
                + UriUtils.encodeQueryParam(
                        Objects.requireNonNull(value, "query parameter value must not be null"),
                        StandardCharsets.UTF_8);
    }

    /**
     * Removes the optional trailing slash from a server URI string.
     *
     * @param value server URI string
     * @return URI string without a trailing slash
     */
    private static String withoutTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
