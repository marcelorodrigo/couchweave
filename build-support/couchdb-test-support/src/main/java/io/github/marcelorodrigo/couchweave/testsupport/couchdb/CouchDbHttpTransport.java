package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import java.io.IOException;
import java.net.http.HttpRequest;

@FunctionalInterface
interface CouchDbHttpTransport {

    CouchDbHttpResponse send(HttpRequest request) throws IOException, InterruptedException;
}
