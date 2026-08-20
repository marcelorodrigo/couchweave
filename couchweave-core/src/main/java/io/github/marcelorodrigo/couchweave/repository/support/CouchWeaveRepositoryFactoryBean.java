package io.github.marcelorodrigo.couchweave.repository.support;

import io.github.marcelorodrigo.couchweave.CouchWeaveOperations;
import io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext;
import io.github.marcelorodrigo.couchweave.repository.CouchWeaveRepositoryConfigurationException;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;

/** Spring factory bean that wires a CouchWeave repository interface to its repository factory. */
public class CouchWeaveRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
        extends RepositoryFactoryBeanSupport<T, S, ID> {

    private CouchWeaveOperations operations;
    private CouchMappingContext mappingContext;

    /** Creates a factory bean for the supplied repository interface. */
    protected CouchWeaveRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    /** Sets the synchronous CouchWeave operations dependency. */
    public void setOperations(CouchWeaveOperations operations) {
        this.operations = operations;
    }

    /** Sets and forwards the CouchWeave mapping context used by Spring Data metadata. */
    public void setMappingContext(CouchMappingContext mappingContext) {
        this.mappingContext = mappingContext;
        super.setMappingContext(mappingContext);
    }

    /** Creates a repository factory with the configured dependencies. */
    @Override
    protected CouchWeaveRepositoryFactory createRepositoryFactory() {
        return new CouchWeaveRepositoryFactory(operations, mappingContext);
    }

    /** Validates required dependencies before Spring Data initializes the factory. */
    @Override
    public void afterPropertiesSet() {
        if (operations == null) {
            throw new CouchWeaveRepositoryConfigurationException(
                    "CouchWeave repository factory requires an operations dependency.");
        }
        if (mappingContext == null) {
            throw new CouchWeaveRepositoryConfigurationException(
                    "CouchWeave repository factory requires a mappingContext dependency.");
        }
        super.afterPropertiesSet();
    }
}
