package io.github.marcelorodrigo.couchweave.mapping;

/** Defines reserved CouchDB field names. */
final class CouchFieldNames {

    /** Stores the document identifier field name. */
    static final String ID = "_id";
    /** Stores the document revision field name. */
    static final String REVISION = "_rev";
    /** Stores the document discriminator field name. */
    static final String DISCRIMINATOR = "couchweave_type";

    /** Prevents instantiation. */
    private CouchFieldNames() {}
}
