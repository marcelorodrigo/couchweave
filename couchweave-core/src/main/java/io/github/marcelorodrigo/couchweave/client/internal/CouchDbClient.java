package io.github.marcelorodrigo.couchweave.client.internal;

import java.util.List;
import org.springframework.http.HttpMethod;

interface CouchDbClient {

    CouchDbResponse exchange(HttpMethod method, List<String> pathSegments, String body);
}
