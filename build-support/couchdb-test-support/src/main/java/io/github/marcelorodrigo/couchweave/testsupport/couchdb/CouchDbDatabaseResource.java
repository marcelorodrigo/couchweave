package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

final class CouchDbDatabaseResource {

    private final CouchDbTestDatabase database;
    private final CouchDbAdminClient adminClient;
    private boolean closed;

    CouchDbDatabaseResource(CouchDbTestDatabase database, CouchDbAdminClient adminClient) {
        this.database = database;
        this.adminClient = adminClient;
    }

    CouchDbTestDatabase database() {
        return database;
    }

    synchronized void close() {
        if (closed) {
            return;
        }
        adminClient.deleteDatabase(database.databaseName());
        closed = true;
    }
}
