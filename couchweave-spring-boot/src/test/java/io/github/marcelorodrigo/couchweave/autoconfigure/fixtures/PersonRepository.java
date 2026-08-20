package io.github.marcelorodrigo.couchweave.autoconfigure.fixtures;

import io.github.marcelorodrigo.couchweave.repository.CouchWeaveRepository;

/** Scanned CouchWeave repository used by the repository auto-configuration tests. */
public interface PersonRepository extends CouchWeaveRepository<Person, String> {}
