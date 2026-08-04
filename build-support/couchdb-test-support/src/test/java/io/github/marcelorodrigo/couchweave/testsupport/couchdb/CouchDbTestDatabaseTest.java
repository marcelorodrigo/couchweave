package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CouchDbTestDatabaseTest {

    @Test
    @DisplayName("should retain valid CouchDB connection details")
    void shouldRetainValidCouchDbConnectionDetails() {
        // given
        var serverUri = URI.create("http://localhost:49152");
        var databaseUri = URI.create("http://localhost:49152/couchweave_test_database");

        // when
        var database = new CouchDbTestDatabase(serverUri, databaseUri, "couchweave_test_database", "admin", "secret");

        // then
        assertThat(database.databaseUri()).isEqualTo(databaseUri);
    }

    @Test
    @DisplayName("should reject a relative server URI")
    void shouldRejectARelativeServerUri() {
        // given
        var serverUri = URI.create("/couchdb");
        var databaseUri = URI.create("http://localhost:49152/couchweave_test_database");

        // when / then
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new CouchDbTestDatabase(serverUri, databaseUri, "couchweave_test_database", "admin", "secret"))
            .withMessage("server URI must be an absolute HTTP URI");
    }

    @Test
    @DisplayName("should reject a blank database name")
    void shouldRejectABlankDatabaseName() {
        // given
        var serverUri = URI.create("http://localhost:49152");
        var databaseUri = URI.create("http://localhost:49152/couchweave_test_database");

        // when / then
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new CouchDbTestDatabase(serverUri, databaseUri, " ", "admin", "secret"))
            .withMessage("database name must not be blank");
    }
}
