package io.github.marcelorodrigo.couchweave.client.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcelorodrigo.couchweave.client.CouchDbAuthenticationException;
import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import io.github.marcelorodrigo.couchweave.client.CouchDbNotFoundException;
import io.github.marcelorodrigo.couchweave.client.CouchDbResponseException;
import io.github.marcelorodrigo.couchweave.client.CouchOptimisticLockingFailureException;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.core.JacksonException;

class CouchDbFailureTranslatorTest {

    private static final CouchDbRequestContext DOCUMENT_CONTEXT =
            CouchDbRequestContext.forDocument("books", "book-42", "3-stale");

    private final CouchDbFailureTranslator translator = new CouchDbFailureTranslator(settings(null, null));

    @ParameterizedTest
    @MethodSource("statusTranslations")
    @DisplayName("should translate CouchDB statuses into Spring data-access categories")
    void shouldTranslateCouchDbStatusesIntoSpringDataAccessCategories(
            int statusCode, Class<? extends DataAccessException> expectedType) {
        // given
        var response = response(statusCode, "{\"error\":\"failure\",\"reason\":\"request failed\"}");

        // when
        var exception = translator.translate(response, DOCUMENT_CONTEXT);

        // then
        assertThat(exception)
                .isInstanceOf(expectedType)
                .hasMessageContaining("database 'books'")
                .hasMessageContaining("document 'book-42'");
    }

    @Test
    @DisplayName("should expose safe CouchDB context for an optimistic locking conflict")
    void shouldExposeSafeCouchDbContextForAnOptimisticLockingConflict() {
        // given
        var response = response(409, "{\"error\":\"conflict\",\"reason\":\"Document update conflict.\"}");

        // when
        var exception = translator.translate(response, DOCUMENT_CONTEXT);

        // then
        assertThat(exception).isInstanceOfSatisfying(CouchOptimisticLockingFailureException.class, conflict -> {
            assertThat(conflict.database()).isEqualTo("books");
            assertThat(conflict.documentId()).isEqualTo("book-42");
            assertThat(conflict.revision()).isEqualTo("3-stale");
            assertThat(conflict).hasMessageContaining("conflict").hasMessageContaining("3-stale");
        });
    }

    @Test
    @DisplayName("should produce a deterministic fallback for a malformed CouchDB error response")
    void shouldProduceADeterministicFallbackForAMalformedCouchDbErrorResponse() {
        // given
        var response = response(502, "not-json");

        // when
        var exception = translator.translate(response, DOCUMENT_CONTEXT);

        // then
        assertThat(exception).isInstanceOfSatisfying(CouchDbResponseException.class, failure -> {
            assertThat(failure.statusCode()).isEqualTo(502);
            assertThat(failure.error()).isEqualTo("unknown");
            assertThat(failure.reason()).isEqualTo("CouchDB returned an unreadable error response");
            assertThat(failure.getCause()).isInstanceOf(JacksonException.class);
            assertThat(failure).hasMessageNotContaining("not-json");
        });
    }

    @Test
    @DisplayName("should decode CouchDB error fields when the response contains additional properties")
    void shouldDecodeCouchDbErrorFieldsWhenTheResponseContainsAdditionalProperties() {
        // given
        var response = response(
                500, "{\"error\":\"internal_server_error\",\"reason\":\"request failed\",\"request_id\":\"abc-123\"}");

        // when
        var exception = translator.translate(response, DOCUMENT_CONTEXT);

        // then
        assertThat(exception).isInstanceOfSatisfying(CouchDbResponseException.class, failure -> {
            assertThat(failure.error()).isEqualTo("internal_server_error");
            assertThat(failure.reason()).isEqualTo("request failed");
        });
    }

    @Test
    @DisplayName("should preserve the original transport failure")
    void shouldPreserveTheOriginalTransportFailure() {
        // given
        var rootCause = new IOException("connection refused");
        var transportFailure = new ResourceAccessException("I/O error", rootCause);

        // when
        var exception = translator.translate(transportFailure, DOCUMENT_CONTEXT);

        // then
        assertThat(exception)
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasCause(transportFailure)
                .hasRootCause(rootCause);
    }

    @Test
    @DisplayName("should redact configured credentials from decoded CouchDB failures")
    void shouldRedactConfiguredCredentialsFromDecodedCouchDbFailures() {
        // given
        var credentialAwareTranslator = new CouchDbFailureTranslator(settings("admin", "very-secret"));
        var response =
                response(500, "{\"error\":\"admin_failure\",\"reason\":\"credential very-secret was rejected\"}");

        // when
        var exception = credentialAwareTranslator.translate(response, DOCUMENT_CONTEXT);

        // then
        assertThat(exception).isInstanceOfSatisfying(CouchDbResponseException.class, failure -> {
            assertThat(failure.error()).doesNotContain("admin");
            assertThat(failure.reason()).doesNotContain("very-secret");
            assertThat(failure).hasMessageNotContaining("admin").hasMessageNotContaining("very-secret");
        });
    }

    @Test
    @DisplayName("should decode valid error responses when credentials match JSON field names")
    void shouldDecodeValidErrorResponsesWhenCredentialsMatchJsonFieldNames() {
        // given
        var credentialAwareTranslator = new CouchDbFailureTranslator(settings("error", "reason"));
        var response = response(500, "{\"error\":\"conflict\",\"reason\":\"request failed\"}");

        // when
        var exception = credentialAwareTranslator.translate(response, DOCUMENT_CONTEXT);

        // then
        assertThat(exception).isInstanceOfSatisfying(CouchDbResponseException.class, failure -> {
            assertThat(failure.error()).isEqualTo("conflict");
            assertThat(failure.reason()).isEqualTo("request failed");
        });
    }

    @Test
    @DisplayName("should redact configured credentials from malformed response causes and context")
    void shouldRedactConfiguredCredentialsFromMalformedResponseCausesAndContext() {
        // given
        var credentialAwareTranslator = new CouchDbFailureTranslator(settings("admin", "very-secret"));
        var response = response(409, "very-secret is not valid JSON");
        var sensitiveContext = CouchDbRequestContext.forDocument("books", "admin", "very-secret");

        // when
        var exception = credentialAwareTranslator.translate(response, sensitiveContext);

        // then
        assertThat(exception).isInstanceOfSatisfying(CouchOptimisticLockingFailureException.class, conflict -> {
            assertThat(conflict.documentId()).isEqualTo("<redacted>");
            assertThat(conflict.revision()).isEqualTo("<redacted>");
            assertThat(conflict).hasMessageNotContaining("admin").hasMessageNotContaining("very-secret");
            assertThat(conflict.getCause()).hasMessageNotContaining("very-secret");
        });
    }

    private static Stream<Arguments> statusTranslations() {
        return Stream.of(
                Arguments.of(400, InvalidDataAccessResourceUsageException.class),
                Arguments.of(401, CouchDbAuthenticationException.class),
                Arguments.of(403, PermissionDeniedDataAccessException.class),
                Arguments.of(404, CouchDbNotFoundException.class),
                Arguments.of(409, CouchOptimisticLockingFailureException.class),
                Arguments.of(500, CouchDbResponseException.class));
    }

    private CouchDbResponse response(int statusCode, String body) {
        return new CouchDbResponse(statusCode, HttpHeaders.EMPTY, body);
    }

    private CouchDbClientSettings settings(String username, String password) {
        return new CouchDbClientSettings(
                URI.create("https://couch.example.test"),
                "books",
                username,
                password,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1));
    }
}
