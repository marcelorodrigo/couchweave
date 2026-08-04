package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CouchDbDatabaseNameGeneratorTest {

    private final CouchDbDatabaseNameGenerator databaseNameGenerator = new CouchDbDatabaseNameGenerator();

    @Test
    @DisplayName("should generate unique CouchDB-safe database names concurrently")
    void shouldGenerateUniqueCouchDbSafeDatabaseNamesConcurrently() {
        // given
        var numberOfNames = 1_000;

        // when
        var names = IntStream.range(0, numberOfNames)
                .parallel()
                .mapToObj(ignored -> databaseNameGenerator.next())
                .toList();

        // then
        assertThat(names)
                .hasSize(numberOfNames)
                .doesNotHaveDuplicates()
                .allMatch(name -> name.matches("couchweave_test_[a-f0-9]{32}"));
    }
}
