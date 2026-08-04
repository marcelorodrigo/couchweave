package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CouchDbIntegrationTest
class CouchDbTestHarnessIT {

    @Test
    @DisplayName("should provide an authenticated ready CouchDB database on a dynamic port")
    void shouldProvideAnAuthenticatedReadyCouchDbDatabaseOnADynamicPort(CouchDbTestDatabase database) throws Exception {
        // given
        var authorization = Base64.getEncoder()
                .encodeToString((database.username() + ":" + database.password()).getBytes(StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(database.serverUri().resolve("/_up"))
                .header("Authorization", "Basic " + authorization)
                .GET()
                .build();

        // when
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(database.serverUri().getPort()).isPositive();
        assertThat(database.databaseUri()).hasToString(database.serverUri() + "/" + database.databaseName());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"ok\"");
    }
}
