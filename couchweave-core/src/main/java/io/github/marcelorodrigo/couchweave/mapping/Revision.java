package io.github.marcelorodrigo.couchweave.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.data.annotation.Version;

/**
 * Marks the property containing a CouchDB document revision.
 */
@Version
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.RECORD_COMPONENT})
public @interface Revision {}
