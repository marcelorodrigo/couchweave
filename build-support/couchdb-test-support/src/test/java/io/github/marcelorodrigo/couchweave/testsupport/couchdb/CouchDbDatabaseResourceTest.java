package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CouchDbDatabaseResourceTest {

    @Test
    @DisplayName("should delete a database only once when cleanup is repeated")
    void shouldDeleteADatabaseOnlyOnceWhenCleanupIsRepeated() {
        // given
        var adminClient = new RecordingAdminClient();
        var database = new CouchDbTestDatabase(
            URI.create("http://localhost:49152"),
            URI.create("http://localhost:49152/couchweave_test_database"),
            "couchweave_test_database",
            "admin",
            "secret"
        );
        var resource = new CouchDbDatabaseResource(database, adminClient);

        // when
        resource.close();
        resource.close();

        // then
        assertThat(adminClient.deletedDatabaseName).isEqualTo("couchweave_test_database");
        assertThat(adminClient.deleteCalls).isOne();
    }

    private static final class RecordingAdminClient extends CouchDbAdminClient {

        private String deletedDatabaseName;
        private int deleteCalls;

        private RecordingAdminClient() {
            super(request -> new CouchDbHttpResponse(request.uri(), 200, "{\"ok\":true}"),
                URI.create("http://localhost:49152"), "admin", "secret");
        }

        @Override
        void deleteDatabase(String databaseName) {
            deletedDatabaseName = databaseName;
            deleteCalls++;
        }
    }
}
