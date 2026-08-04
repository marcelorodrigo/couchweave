package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Starts the shared CouchDB test container and injects an isolated database into the test class.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(CouchDbIntegrationExtension.class)
public @interface CouchDbIntegrationTest {
}
