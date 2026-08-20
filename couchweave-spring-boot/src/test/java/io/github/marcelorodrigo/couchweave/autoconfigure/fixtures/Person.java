package io.github.marcelorodrigo.couchweave.autoconfigure.fixtures;

import io.github.marcelorodrigo.couchweave.mapping.CouchDocument;
import org.springframework.data.annotation.Id;

/** Document entity managed by {@link PersonRepository}. */
@CouchDocument
public class Person {
    @Id
    String id;

    public Person(String id) {
        this.id = id;
    }
}
