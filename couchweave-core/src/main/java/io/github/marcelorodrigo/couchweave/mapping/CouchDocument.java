package io.github.marcelorodrigo.couchweave.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.data.annotation.Persistent;

/**
 * Marks a type as a CouchDB root document.
 */
@Persistent
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CouchDocument {

    /**
     * Returns the persisted type discriminator.
     *
     * @return the discriminator, or an empty string to use the Java simple class name
     */
    String type() default "";

    /**
     * Returns the CouchDB database containing the document.
     *
     * @return the database, or an empty string to use the mapping context default
     */
    String database() default "";
}
