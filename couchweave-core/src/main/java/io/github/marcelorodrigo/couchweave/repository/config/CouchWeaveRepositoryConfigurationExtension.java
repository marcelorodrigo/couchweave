package io.github.marcelorodrigo.couchweave.repository.config;

import io.github.marcelorodrigo.couchweave.CouchWeaveOperations;
import io.github.marcelorodrigo.couchweave.CouchWeaveTemplate;
import io.github.marcelorodrigo.couchweave.client.CouchDbClientSettings;
import io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext;
import io.github.marcelorodrigo.couchweave.mapping.CouchWeaveConverter;
import io.github.marcelorodrigo.couchweave.mapping.CouchWeaveCustomConversions;
import io.github.marcelorodrigo.couchweave.mapping.MappingCouchWeaveConverter;
import io.github.marcelorodrigo.couchweave.repository.CouchWeaveRepository;
import io.github.marcelorodrigo.couchweave.repository.support.CouchWeaveRepositoryFactoryBean;
import io.github.marcelorodrigo.couchweave.repository.support.SimpleCouchWeaveRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestClient;

/** Provides Spring Data repository discovery metadata for CouchWeave repositories. */
public class CouchWeaveRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

    /** Creates the CouchWeave repository configuration extension. */
    public CouchWeaveRepositoryConfigurationExtension() {}

    /** Returns the module display name. */
    @Override
    public String getModuleName() {
        return "CouchWeave";
    }

    /** Returns the module configuration prefix. */
    @Override
    protected String getModulePrefix() {
        return "couchweave";
    }

    /** Returns the marker repository type. */
    @Override
    protected Collection<Class<?>> getIdentifyingTypes() {
        return Collections.singleton(CouchWeaveRepository.class);
    }

    /** Returns no identifying annotations. */
    @Override
    protected Collection<Class<? extends java.lang.annotation.Annotation>> getIdentifyingAnnotations() {
        return Collections.emptySet();
    }

    /** Returns the factory bean class name used for discovered repositories. */
    @Override
    public String getRepositoryFactoryBeanClassName() {
        return CouchWeaveRepositoryFactoryBean.class.getName();
    }

    /** Returns the repository base class name used for discovered repositories. */
    @Override
    public String getRepositoryBaseClassName() {
        return SimpleCouchWeaveRepository.class.getName();
    }

    /**
     * Wires the CouchWeave mapping context and operations beans into each discovered repository
     * factory by type, so custom infrastructure beans take precedence over defaults regardless of
     * their bean names.
     */
    @Override
    public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {
        builder.addPropertyValue("mappingContext", new RuntimeBeanReference(CouchMappingContext.class));
        builder.addPropertyValue("operations", new RuntimeBeanReference(CouchWeaveOperations.class));
    }

    /** Claims only repositories explicitly extending CouchWeaveRepository. */
    @Override
    protected boolean isStrictRepositoryCandidate(RepositoryMetadata metadata) {
        return CouchWeaveRepository.class.isAssignableFrom(metadata.getRepositoryInterface());
    }

    /** Restricts repository creation to CouchWeave repository interfaces. */
    @Override
    protected boolean useRepositoryConfiguration(RepositoryMetadata metadata) {
        return CouchWeaveRepository.class.isAssignableFrom(metadata.getRepositoryInterface());
    }

    /**
     * Registers the default CouchWeave infrastructure when the application has not supplied the
     * equivalent bean. The default mapping context is non-strict so repository domain types
     * discovered during scanning are resolved lazily, while explicitly supplied mapping contexts
     * keep their configured strictness.
     */
    @Override
    public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource source) {
        super.registerBeansForRoot(registry, source);

        if (!containsBeanOfType(RestClient.Builder.class, registry)) {
            registerIfNotAlreadyRegistered(
                    () -> restClientBuilderDefinition(), registry, "couchWeaveRestClientBuilder", null);
        }
        if (!containsBeanOfType(CouchMappingContext.class, registry)) {
            registerIfNotAlreadyRegistered(() -> mappingContextDefinition(), registry, "couchMappingContext", null);
        }
        if (!containsBeanOfType(CouchWeaveCustomConversions.class, registry)) {
            registerIfNotAlreadyRegistered(
                    () -> customConversionsDefinition(), registry, "couchWeaveCustomConversions", null);
        }
        if (!containsBeanOfType(CouchWeaveConverter.class, registry)) {
            registerIfNotAlreadyRegistered(() -> converterDefinition(), registry, "couchWeaveConverter", null);
        }
        if (!containsBeanOfType(CouchWeaveOperations.class, registry)) {
            registerIfNotAlreadyRegistered(() -> operationsDefinition(), registry, "couchWeaveOperations", null);
        }
    }

    /**
     * Checks whether the registry contains a bean of the requested type.
     *
     * @param type requested bean type
     * @param registry bean definition registry
     * @return whether a matching bean is registered
     */
    private static boolean containsBeanOfType(Class<?> type, BeanDefinitionRegistry registry) {
        for (var beanName : registry.getBeanDefinitionNames()) {
            if (providesType(registry.getBeanDefinition(beanName), type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a bean definition provides the requested type.
     *
     * @param definition bean definition to inspect
     * @param type requested bean type
     * @return whether the definition provides the requested type
     */
    private static boolean providesType(BeanDefinition definition, Class<?> type) {
        var candidate = resolveBeanType(definition);
        if (candidate != null && type.isAssignableFrom(candidate)) {
            return true;
        }
        // A FactoryBean is registered under its own type, so the product type must be resolved
        // from the definition attribute (populated from FactoryBean#getObjectType()) or the
        // FactoryBean generic, otherwise the default infrastructure would be registered twice.
        if (candidate != null && FactoryBean.class.isAssignableFrom(candidate)) {
            var product = resolveFactoryBeanObjectType(definition, candidate);
            return product != null && type.isAssignableFrom(product);
        }
        return false;
    }

    /**
     * Resolves the object type produced by a factory bean definition.
     *
     * @param definition factory bean definition
     * @param factoryBeanType factory bean type
     * @return resolved factory bean object type, or {@code null}
     */
    private static Class<?> resolveFactoryBeanObjectType(BeanDefinition definition, Class<?> factoryBeanType) {
        var attribute = definition.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
        if (attribute instanceof Class<?> clazz) {
            return clazz;
        }
        if (attribute instanceof String className) {
            var resolved = safeResolve(className);
            if (resolved != null) {
                return resolved;
            }
        }
        return ResolvableType.forClass(factoryBeanType)
                .as(FactoryBean.class)
                .getGeneric(0)
                .resolve();
    }

    /**
     * Resolves the class represented by a bean definition.
     *
     * @param definition bean definition to inspect
     * @return resolved bean type, or {@code null}
     */
    private static Class<?> resolveBeanType(BeanDefinition definition) {
        if (definition instanceof AnnotatedBeanDefinition annotated && annotated.getFactoryMethodMetadata() != null) {
            var resolved = safeResolve(annotated.getFactoryMethodMetadata().getReturnTypeName());
            if (resolved != null) {
                return resolved;
            }
        }
        return safeResolve(definition.getBeanClassName());
    }

    /**
     * Resolves a class name without propagating resolution failures.
     *
     * @param className class name to resolve
     * @return resolved class, or {@code null}
     */
    private static Class<?> safeResolve(String className) {
        if (className == null) {
            return null;
        }
        try {
            return ClassUtils.resolveClassName(className, null);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Creates the default REST client builder definition.
     *
     * @return REST client builder bean definition
     */
    private static org.springframework.beans.factory.support.AbstractBeanDefinition restClientBuilderDefinition() {
        return BeanDefinitionBuilder.rootBeanDefinition(RestClient.class, "builder")
                .getBeanDefinition();
    }

    /**
     * Creates the default mapping context definition.
     *
     * @return mapping context bean definition
     */
    private static org.springframework.beans.factory.support.AbstractBeanDefinition mappingContextDefinition() {
        return BeanDefinitionBuilder.genericBeanDefinition(CouchMappingContext.class)
                .addConstructorArgValue(new RuntimeBeanReference(CouchDbClientSettings.class))
                .addConstructorArgValue(false)
                .getBeanDefinition();
    }

    /**
     * Creates the default custom conversions definition.
     *
     * @return custom conversions bean definition
     */
    private static org.springframework.beans.factory.support.AbstractBeanDefinition customConversionsDefinition() {
        return BeanDefinitionBuilder.genericBeanDefinition(CouchWeaveCustomConversions.class)
                .addConstructorArgValue(List.of())
                .getBeanDefinition();
    }

    /**
     * Creates the default converter definition.
     *
     * @return converter bean definition
     */
    private static org.springframework.beans.factory.support.AbstractBeanDefinition converterDefinition() {
        return BeanDefinitionBuilder.genericBeanDefinition(MappingCouchWeaveConverter.class)
                .addConstructorArgValue(new RuntimeBeanReference(CouchMappingContext.class))
                .addConstructorArgValue(new RuntimeBeanReference(CouchWeaveCustomConversions.class))
                .getBeanDefinition();
    }

    /**
     * Creates the default operations definition.
     *
     * @return operations bean definition
     */
    private static org.springframework.beans.factory.support.AbstractBeanDefinition operationsDefinition() {
        return BeanDefinitionBuilder.genericBeanDefinition(CouchWeaveTemplate.class)
                .addConstructorArgValue(new RuntimeBeanReference(CouchDbClientSettings.class))
                .addConstructorArgValue(new RuntimeBeanReference(RestClient.Builder.class))
                .addConstructorArgValue(new RuntimeBeanReference(CouchWeaveConverter.class))
                .getBeanDefinition();
    }
}
