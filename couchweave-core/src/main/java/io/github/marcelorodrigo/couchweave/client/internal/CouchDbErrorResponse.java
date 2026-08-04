package io.github.marcelorodrigo.couchweave.client.internal;

/**
 * The two descriptive fields returned in a CouchDB error response.
 *
 * @param error machine-readable CouchDB error identifier
 * @param reason human-readable CouchDB error reason
 */
record CouchDbErrorResponse(String error, String reason) {}
