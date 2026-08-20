package io.github.marcelorodrigo.couchweave.client.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.marcelorodrigo.couchweave.client.CouchDbResponseException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import tools.jackson.databind.ObjectMapper;

class CouchDbResponseDecoderTest {

    private final CouchDbResponseDecoder decoder = new CouchDbResponseDecoder(new ObjectMapper());
    private final CouchDbRequestContext context = CouchDbRequestContext.forDocument("books", "book-42", null);

    @Test
    @DisplayName("should decode a raw CouchDB document")
    void shouldDecodeARawCouchDbDocument() {
        // given
        var response = response("{\"_id\":\"book-42\",\"_rev\":\"1-abc\",\"title\":\"CouchWeave\"}");

        // when
        var document = decoder.decodeDocument(response, context);

        // then
        assertThat(document.get("title").stringValue()).isEqualTo("CouchWeave");
    }

    @Test
    @DisplayName("should decode a successful write result")
    void shouldDecodeASuccessfulWriteResult() {
        // given
        var response = response("{\"ok\":true,\"id\":\"book-42\",\"rev\":\"2-def\"}");

        // when
        var result = decoder.decodeWriteResult(response, context);

        // then
        assertThat(result).isEqualTo(new CouchDbWriteResult("book-42", "2-def"));
    }

    @Test
    @DisplayName("should decode document bodies from an all documents response in order")
    void shouldDecodeDocumentBodiesFromAnAllDocumentsResponseInOrder() {
        // given
        var response = response("""
                {"total_rows":2,"rows":[{"id":"book-1","doc":{"_id":"book-1","title":"First"}},
                {"id":"book-2","doc":{"_id":"book-2","title":"Second"}}]}
                """);

        // when
        var documents = decoder.decodeDocuments(response, context);

        // then
        assertThat(documents)
                .extracting(document -> document.get("title").stringValue())
                .containsExactly("First", "Second");
    }

    @Test
    @DisplayName("should return an empty list when an all documents response has no rows")
    void shouldReturnAnEmptyListWhenAnAllDocumentsResponseHasNoRows() {
        // given
        var response = response("{\"rows\":[]}");

        // when
        var documents = decoder.decodeDocuments(response, context);

        // then
        assertThat(documents).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidAllDocuments")
    @DisplayName("should reject malformed all documents responses")
    void shouldRejectMalformedAllDocumentsResponses(String body) {
        // given
        var response = response(body);

        // when / then
        assertInvalidDocumentsResponse(response);
    }

    @ParameterizedTest
    @MethodSource("invalidDocuments")
    @DisplayName("should reject malformed document responses")
    void shouldRejectMalformedDocumentResponses(String body) {
        // given
        var response = response(body);

        // when / then
        assertThatThrownBy(() -> decoder.decodeDocument(response, context))
                .isInstanceOf(CouchDbResponseException.class)
                .hasMessageContaining("unreadable success response")
                .hasMessageNotContaining(body);
    }

    @ParameterizedTest
    @MethodSource("invalidWriteResults")
    @DisplayName("should reject malformed write responses")
    void shouldRejectMalformedWriteResponses(String body) {
        // given
        var response = response(body);

        // when / then
        assertThatThrownBy(() -> decoder.decodeWriteResult(response, context))
                .isInstanceOf(CouchDbResponseException.class)
                .hasMessageContaining("book-42")
                .hasMessageNotContaining(body);
    }

    private CouchDbResponse response(String body) {
        return new CouchDbResponse(200, HttpHeaders.EMPTY, body);
    }

    private void assertInvalidDocumentsResponse(CouchDbResponse response) {
        assertThatThrownBy(() -> decoder.decodeDocuments(response, context))
                .isInstanceOf(CouchDbResponseException.class)
                .extracting(exception -> ((CouchDbResponseException) exception).error())
                .isEqualTo("invalid_response");
    }

    private static Stream<Arguments> invalidDocuments() {
        return Stream.of(
                Arguments.of("not-json"),
                Arguments.of("[]"),
                Arguments.of("{\"_rev\":\"1-abc\"}"),
                Arguments.of("{\"_id\":\"book-42\"}"));
    }

    private static Stream<Arguments> invalidAllDocuments() {
        return Stream.of(
                Arguments.of("{}"),
                Arguments.of("{\"rows\":{}}"),
                Arguments.of("{\"rows\":[null]}"),
                Arguments.of("{\"rows\":[{}]}"),
                Arguments.of("{\"rows\":[{\"doc\":[]}]}"));
    }

    private static Stream<Arguments> invalidWriteResults() {
        return Stream.of(
                Arguments.of("{\"ok\":false,\"id\":\"book-42\",\"rev\":\"1-abc\"}"),
                Arguments.of("{\"ok\":true,\"rev\":\"1-abc\"}"),
                Arguments.of("{\"ok\":true,\"id\":\"book-42\"}"));
    }
}
