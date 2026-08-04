package io.github.marcelorodrigo.couchweave.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the stored name of a CouchDB document property.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.RECORD_COMPONENT})
public @interface CouchField {

    /**
     * Returns the stored field name.
     *
     * @return the field name, or an empty string to use the Java property name
     */
    String value() default "";
}
