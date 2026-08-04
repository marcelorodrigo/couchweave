package io.github.marcelorodrigo.couchweave.client.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CouchDbUriBuilderTest {

    @Test
    @DisplayName("should preserve an encoded base path and encode document path segments once")
    void shouldPreserveAnEncodedBasePathAndEncodeDocumentPathSegmentsOnce() {
        // given
        var builder = new CouchDbUriBuilder(
                new CouchDbClientSettings(URI.create("https://couch.example.test/base%20path/"), "books"));

        // when
        var uri = builder.build(List.of("books", "document/a b%20ü"));

        // then
        assertThat(uri).hasToString("https://couch.example.test/base%20path/books/document%2Fa%20b%2520%C3%BC");
    }

    @Test
    @DisplayName("should return the server URI when no endpoint path is required")
    void shouldReturnTheServerUriWhenNoEndpointPathIsRequired() {
        // given
        var builder = new CouchDbUriBuilder(
                new CouchDbClientSettings(URI.create("https://couch.example.test/base"), "books"));

        // when
        var uri = builder.build(List.of());

        // then
        assertThat(uri).hasToString("https://couch.example.test/base");
    }

    @Test
    @DisplayName("should reject a null path segment")
    void shouldRejectANullPathSegment() {
        // given
        var builder =
                new CouchDbUriBuilder(new CouchDbClientSettings(URI.create("https://couch.example.test"), "books"));

        // when / then
        assertThatNullPointerException().isThrownBy(() -> builder.build(List.of("books", null)));
    }
}
