package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import java.net.URI;

record CouchDbHttpResponse(URI uri, int statusCode, String body) {}
