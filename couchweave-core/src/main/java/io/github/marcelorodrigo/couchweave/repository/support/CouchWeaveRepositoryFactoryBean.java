package io.github.marcelorodrigo.couchweave.repository.support;

import io.github.marcelorodrigo.couchweave.CouchWeaveOperations;
import io.github.marcelorodrigo.couchweave.mapping.CouchMappingContext;
import io.github.marcelorodrigo.couchweave.repository.CouchWeaveRepositoryConfigurationException;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;

/**
 * Spring factory bean that wires a CouchWeave repository interface to its repository factory.
 *
 * @param <T> the repository interface type
 * @param <S> the domain type managed by the repository
 * @param <I> the document identifier type
 */
public class CouchWeaveRepositoryFactoryBean<T extends Repository<S, I>, S, I>
        extends RepositoryFactoryBeanSupport<T, S, I> {

    /** The synchronous CouchWeave operations dependency. */
    private CouchWeaveOperations operations;
    /** The CouchWeave mapping context dependency. */
    private CouchMappingContext mappingContext;

    /** Creates a factory bean for the supplied repository interface.
     *
     * @param repositoryInterface the repository interface to configure
     */
    protected CouchWeaveRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    /** Sets the synchronous CouchWeave operations dependency.
     *
     * @param operations the synchronous CouchWeave operations
     */
    public void setOperations(CouchWeaveOperations operations) {
        this.operations = operations;
    }

    /** Sets the CouchWeave mapping context used by the repository factory.
     *
     * @param mappingContext the CouchWeave mapping context
     */
    public void setMappingContext(CouchMappingContext mappingContext) {
        this.mappingContext = mappingContext;
    }

    /** Creates a repository factory with the configured dependencies. */
    @Override
    protected CouchWeaveRepositoryFactory createRepositoryFactory() {
        return new CouchWeaveRepositoryFactory(operations, mappingContext);
    }

    /** Validates required dependencies and forwards the mapping context to Spring Data metadata. */
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
        super.setMappingContext(mappingContext);
        super.afterPropertiesSet();
    }
}
