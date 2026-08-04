package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import java.net.http.HttpClient;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit extension backing {@link CouchDbIntegrationTest}.
 */
public final class CouchDbIntegrationExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final Object CONTAINER_KEY = CouchDbContainerResource.class;
    private static final Object DATABASE_KEY = CouchDbDatabaseResource.class;
    private static final ExtensionContext.Namespace ROOT_NAMESPACE =
            ExtensionContext.Namespace.create(CouchDbIntegrationExtension.class, "container");

    private final CouchDbContainerFactory containerFactory;
    private final CouchDbAdminClientFactory adminClientFactory;
    private final CouchDbDatabaseNameGenerator databaseNameGenerator;

    public CouchDbIntegrationExtension() {
        this(
                CouchDbContainerResource::start,
                (serverUri, username, password) -> new CouchDbAdminClient(
                        new JdkCouchDbHttpTransport(HttpClient.newHttpClient()), serverUri, username, password),
                new CouchDbDatabaseNameGenerator());
    }

    CouchDbIntegrationExtension(
            CouchDbContainerFactory containerFactory,
            CouchDbAdminClientFactory adminClientFactory,
            CouchDbDatabaseNameGenerator databaseNameGenerator) {
        this.containerFactory = containerFactory;
        this.adminClientFactory = adminClientFactory;
        this.databaseNameGenerator = databaseNameGenerator;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        databaseResource(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        var databaseResource = classStore(context).remove(DATABASE_KEY, CouchDbDatabaseResource.class);
        if (databaseResource != null) {
            databaseResource.close();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType().equals(CouchDbTestDatabase.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return databaseResource(extensionContext).database();
    }

    private CouchDbDatabaseResource databaseResource(ExtensionContext context) {
        return classStore(context)
                .getOrComputeIfAbsent(DATABASE_KEY, ignored -> createDatabase(context), CouchDbDatabaseResource.class);
    }

    private CouchDbDatabaseResource createDatabase(ExtensionContext context) {
        var container = rootStore(context)
                .getOrComputeIfAbsent(
                        CONTAINER_KEY, ignored -> containerFactory.start(), CouchDbContainerResource.class);
        var adminClient = adminClientFactory.create(container.serverUri(), container.username(), container.password());
        adminClient.assertHealthy();
        var database = adminClient.createDatabase(databaseNameGenerator.next());
        return new CouchDbDatabaseResource(database, adminClient);
    }

    private ExtensionContext.Store rootStore(ExtensionContext context) {
        return context.getRoot().getStore(ROOT_NAMESPACE);
    }

    private ExtensionContext.Store classStore(ExtensionContext context) {
        return context.getStore(
                ExtensionContext.Namespace.create(CouchDbIntegrationExtension.class, context.getRequiredTestClass()));
    }

    @FunctionalInterface
    interface CouchDbContainerFactory {

        CouchDbContainerResource start();
    }

    @FunctionalInterface
    interface CouchDbAdminClientFactory {

        CouchDbAdminClient create(java.net.URI serverUri, String username, String password);
    }
}
