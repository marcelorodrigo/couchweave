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
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestClient;

/** Provides Spring Data repository discovery metadata for CouchWeave repositories. */
public class CouchWeaveRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

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

    private static boolean containsBeanOfType(Class<?> type, BeanDefinitionRegistry registry) {
        for (var beanName : registry.getBeanDefinitionNames()) {
            var resolved = resolveBeanType(registry.getBeanDefinition(beanName));
            if (resolved != null && type.isAssignableFrom(resolved)) {
                return true;
            }
        }
        return false;
    }

    private static Class<?> resolveBeanType(BeanDefinition definition) {
        if (definition instanceof AnnotatedBeanDefinition annotated && annotated.getFactoryMethodMetadata() != null) {
            var resolved = safeResolve(annotated.getFactoryMethodMetadata().getReturnTypeName());
            if (resolved != null) {
                return resolved;
            }
        }
        return safeResolve(definition.getBeanClassName());
    }

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

    private static org.springframework.beans.factory.support.AbstractBeanDefinition restClientBuilderDefinition() {
        return BeanDefinitionBuilder.rootBeanDefinition(RestClient.class, "builder")
                .getBeanDefinition();
    }

    private static org.springframework.beans.factory.support.AbstractBeanDefinition mappingContextDefinition() {
        return BeanDefinitionBuilder.genericBeanDefinition(CouchMappingContext.class)
                .addConstructorArgValue(new RuntimeBeanReference(CouchDbClientSettings.class))
                .addConstructorArgValue(false)
                .getBeanDefinition();
    }

    private static org.springframework.beans.factory.support.AbstractBeanDefinition customConversionsDefinition() {
        return BeanDefinitionBuilder.genericBeanDefinition(CouchWeaveCustomConversions.class)
                .addConstructorArgValue(List.of())
                .getBeanDefinition();
    }

    private static org.springframework.beans.factory.support.AbstractBeanDefinition converterDefinition() {
        return BeanDefinitionBuilder.genericBeanDefinition(MappingCouchWeaveConverter.class)
                .addConstructorArgValue(new RuntimeBeanReference(CouchMappingContext.class))
                .addConstructorArgValue(new RuntimeBeanReference(CouchWeaveCustomConversions.class))
                .getBeanDefinition();
    }

    private static org.springframework.beans.factory.support.AbstractBeanDefinition operationsDefinition() {
        return BeanDefinitionBuilder.genericBeanDefinition(CouchWeaveTemplate.class)
                .addConstructorArgValue(new RuntimeBeanReference(CouchDbClientSettings.class))
                .addConstructorArgValue(new RuntimeBeanReference(RestClient.Builder.class))
                .addConstructorArgValue(new RuntimeBeanReference(CouchWeaveConverter.class))
                .getBeanDefinition();
    }
}
