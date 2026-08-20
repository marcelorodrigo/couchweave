package io.github.marcelorodrigo.couchweave.autoconfigure.excluded;

import io.github.marcelorodrigo.couchweave.mapping.CouchDocument;
import org.springframework.data.annotation.Id;

/** Document entity managed by {@link ExcludedRepository}. */
@CouchDocument
public class ExcludedPerson {
    @Id
    String id;
}
