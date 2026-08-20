package io.github.marcelorodrigo.couchweave.autoconfigure.excluded;

import io.github.marcelorodrigo.couchweave.repository.CouchWeaveRepository;

/** Repository placed outside the scanned base package, so it must not be discovered. */
public interface ExcludedRepository extends CouchWeaveRepository<ExcludedPerson, String> {}
