package io.github.marcelorodrigo.couchweave.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class CouchDbClientSettingsTest {

    @Test
    @DisplayName("should apply documented defaults when only server and database are configured")
    void shouldApplyDocumentedDefaultsWhenOnlyServerAndDatabaseAreConfigured() {
        // given
        var serverUri = URI.create("https://couch.example.test");

        // when
        var settings = new CouchDbClientSettings(serverUri, "books");

        // then
        assertThat(settings.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(settings.readTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(settings.hasCredentials()).isFalse();
    }

    @Test
    @DisplayName("should retain a complete credential pair")
    void shouldRetainACompleteCredentialPair() {
        // given
        var serverUri = URI.create("https://couch.example.test/base");

        // when
        var settings = new CouchDbClientSettings(
                serverUri, "books", "admin", "secret", Duration.ofSeconds(2), Duration.ofSeconds(4));

        // then
        assertThat(settings.username()).isEqualTo("admin");
        assertThat(settings.password()).isEqualTo("secret");
        assertThat(settings.hasCredentials()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"ftp://couch.example.test", "https:/couch.example.test", "https://couch.example.test?replica=one"
            })
    @DisplayName("should reject an invalid server URI")
    void shouldRejectAnInvalidServerUri(String serverUri) {
        // given
        var uri = URI.create(serverUri);

        // when / then
        assertThatThrownBy(() -> new CouchDbClientSettings(uri, "books"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("serverUri");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Books", "book name", "_users", "books?replica=one"})
    @DisplayName("should reject an illegal database name")
    void shouldRejectAnIllegalDatabaseName(String database) {
        // given
        var serverUri = URI.create("https://couch.example.test");

        // when / then
        assertThatThrownBy(() -> new CouchDbClientSettings(serverUri, database))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("database");
    }

    @ParameterizedTest
    @MethodSource("invalidCredentials")
    @DisplayName("should reject an incomplete or blank credential pair")
    void shouldRejectAnIncompleteOrBlankCredentialPair(String username, String password) {
        // given
        var serverUri = URI.create("https://couch.example.test");

        // when / then
        assertThatThrownBy(() -> new CouchDbClientSettings(
                        serverUri, "books", username, password, Duration.ofSeconds(1), Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username and password");
    }

    @Test
    @DisplayName("should reject a non-positive connect timeout")
    void shouldRejectANonPositiveConnectTimeout() {
        // given
        var serverUri = URI.create("https://couch.example.test");

        // when / then
        assertThatThrownBy(() ->
                        new CouchDbClientSettings(serverUri, "books", null, null, Duration.ZERO, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connectTimeout");
    }

    @Test
    @DisplayName("should reject a missing read timeout")
    void shouldRejectAMissingReadTimeout() {
        // given
        var serverUri = URI.create("https://couch.example.test");

        // when / then
        assertThatThrownBy(() -> new CouchDbClientSettings(serverUri, "books", null, null, Duration.ofSeconds(1), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("readTimeout");
    }

    @Test
    @DisplayName("should redact the password from its string representation")
    void shouldRedactThePasswordFromItsStringRepresentation() {
        // given
        var settings = new CouchDbClientSettings(
                URI.create("https://couch.example.test"),
                "books",
                "admin",
                "very-secret",
                Duration.ofSeconds(1),
                Duration.ofSeconds(1));

        // when
        var value = settings.toString();

        // then
        assertThat(value).contains("password=<redacted>");
        assertThat(value).doesNotContain("very-secret");
    }

    private static Stream<Arguments> invalidCredentials() {
        return Stream.of(
                Arguments.of("admin", null),
                Arguments.of(null, "secret"),
                Arguments.of("", "secret"),
                Arguments.of("admin", ""));
    }
}
