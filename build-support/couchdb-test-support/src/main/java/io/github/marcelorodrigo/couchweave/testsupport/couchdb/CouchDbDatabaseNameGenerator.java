package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import java.util.UUID;

final class CouchDbDatabaseNameGenerator {

    private static final String PREFIX = "couchweave_test_";

    String next() {
        return PREFIX + UUID.randomUUID().toString().replace("-", "");
    }
}
