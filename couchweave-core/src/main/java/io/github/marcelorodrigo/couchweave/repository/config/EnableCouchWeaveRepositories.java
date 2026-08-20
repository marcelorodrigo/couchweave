package io.github.marcelorodrigo.couchweave.repository.config;

import io.github.marcelorodrigo.couchweave.repository.support.CouchWeaveRepositoryFactoryBean;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;
import org.springframework.data.repository.query.QueryLookupStrategy;

/**
 * Activates CouchWeave repository support in a plain Spring application context, without requiring
 * Spring Boot.
 *
 * <p>Repository interfaces extending {@link io.github.marcelorodrigo.couchweave.repository.CouchWeaveRepository}
 * are discovered through the standard Spring Data scanning attributes declared here. The imported
 * infrastructure configuration supplies default settings, REST client builder, mapping context,
 * custom conversions, converter, and operations; any of those beans may be overridden by the
 * application.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(CouchWeaveRepositoriesRegistrar.class)
public @interface EnableCouchWeaveRepositories {

    /** Alias for {@link #basePackages()}. */
    String[] value() default {};

    /** Base packages to scan for annotated repository interfaces. */
    String[] basePackages() default {};

    /** Type-safe alternative to {@link #basePackages()}; the packages of the listed classes are scanned. */
    Class<?>[] basePackageClasses() default {};

    /** Location of named queries; empty disables named-query lookup. */
    String namedQueriesLocation() default "";

    /** Strategy used to resolve repository query methods. */
    QueryLookupStrategy.Key queryLookupStrategy() default QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND;

    /** Suffix appended to repository interfaces to locate custom implementations. */
    String repositoryImplementationPostfix() default "Impl";

    /** Factory bean used to create repository proxies. */
    Class<?> repositoryFactoryBeanClass() default CouchWeaveRepositoryFactoryBean.class;

    /** Base class all repositories inherit; defaults to the Spring Data base class. */
    Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;

    /** Whether repository interfaces in nested classes are discovered. */
    boolean considerNestedRepositories() default false;

    /** Bootstrap mode for the repository infrastructure. */
    BootstrapMode bootstrapMode() default BootstrapMode.DEFAULT;

    /** Strategy for generating repository bean names. */
    Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

    /** Filters used to include repository candidates during scanning. */
    Filter[] includeFilters() default {};

    /** Filters used to exclude repository candidates during scanning. */
    Filter[] excludeFilters() default {};
}
