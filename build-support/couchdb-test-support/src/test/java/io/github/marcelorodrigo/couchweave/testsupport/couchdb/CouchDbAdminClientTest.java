package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import java.net.URI;
import java.net.http.HttpRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouchDbAdminClientTest {

    @Test
    @DisplayName("should authenticate the CouchDB readiness check")
    void shouldAuthenticateTheCouchDbReadinessCheck() {
        // given
        var transport = new FakeTransport(200, "{\"status\":\"ok\"}");
        var client = client(transport);

        // when
        client.assertHealthy();

        // then
        assertThat(transport.request.method()).isEqualTo("GET");
        assertThat(transport.request.uri()).hasPath("/_up");
        assertThat(transport.request.headers().firstValue("Authorization"))
            .hasValueSatisfying(value -> assertThat(value).startsWith("Basic "));
    }

    @Test
    @DisplayName("should create the isolated database with a PUT request")
    void shouldCreateTheIsolatedDatabaseWithAPutRequest() {
        // given
        var transport = new FakeTransport(201, "{\"ok\":true}");
        var client = client(transport);

        // when
        var database = client.createDatabase("couchweave_test_database");

        // then
        assertThat(database.databaseUri()).hasToString("http://localhost:49152/couchweave_test_database");
        assertThat(transport.request.method()).isEqualTo("PUT");
    }

    @Test
    @DisplayName("should treat a missing database as deleted during cleanup")
    void shouldTreatAMissingDatabaseAsDeletedDuringCleanup() {
        // given
        var transport = new FakeTransport(404, "{\"error\":\"not_found\"}");
        var client = client(transport);

        // when
        client.deleteDatabase("couchweave_test_database");

        // then
        assertThat(transport.request.method()).isEqualTo("DELETE");
    }

    @Test
    @DisplayName("should report an unexpected database creation response without credentials")
    void shouldReportAnUnexpectedDatabaseCreationResponseWithoutCredentials() {
        // given
        var transport = new FakeTransport(401, "{\"error\":\"unauthorized\"}");
        var client = client(transport);

        // when / then
        assertThatThrownBy(() -> client.createDatabase("couchweave_test_database"))
            .isInstanceOf(CouchDbTestHarnessException.class)
            .hasMessageContaining("HTTP 401")
            .hasMessageNotContaining("secret");
    }

    private CouchDbAdminClient client(CouchDbHttpTransport transport) {
        return new CouchDbAdminClient(transport, URI.create("http://localhost:49152"), "admin", "secret");
    }

    private static final class FakeTransport implements CouchDbHttpTransport {

        private final int responseStatus;
        private final String responseBody;
        private HttpRequest request;

        private FakeTransport(int responseStatus, String responseBody) {
            this.responseStatus = responseStatus;
            this.responseBody = responseBody;
        }

        @Override
        public CouchDbHttpResponse send(HttpRequest request) {
            this.request = request;
            return new CouchDbHttpResponse(request.uri(), responseStatus, responseBody);
        }
    }
}
