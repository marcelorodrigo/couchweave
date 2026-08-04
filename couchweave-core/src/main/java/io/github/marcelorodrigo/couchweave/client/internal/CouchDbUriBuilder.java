package io.github.marcelorodrigo.couchweave.client.internal;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
        Objects.requireNonNull(pathSegments, "pathSegments must not be null");
        if (pathSegments.isEmpty()) {
            return URI.create(baseUri);
        }

        var encodedPath = pathSegments.stream()
                .map(segment -> UriUtils.encodePathSegment(
                        Objects.requireNonNull(segment, "path segment must not be null"), StandardCharsets.UTF_8))
                .reduce((left, right) -> left + "/" + right)
                .orElseThrow();
        return URI.create(baseUri + "/" + encodedPath);
    }

    private static String withoutTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
