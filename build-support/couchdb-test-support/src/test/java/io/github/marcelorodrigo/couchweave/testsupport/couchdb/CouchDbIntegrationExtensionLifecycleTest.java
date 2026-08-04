package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

class CouchDbIntegrationExtensionLifecycleTest {

    @Test
    @DisplayName("should isolate and clean databases after successful and failed test classes")
    void shouldIsolateAndCleanDatabasesAfterSuccessfulAndFailedTestClasses() {
        // given
        var rootStore = new InMemoryStore();
        var root = rootContext(rootStore);
        var firstContext = classContext(root, FirstTestClass.class, new InMemoryStore(), Optional.empty());
        var secondContext = classContext(
                root,
                SecondTestClass.class,
                new InMemoryStore(),
                Optional.of(new AssertionError("simulated test failure")));
        var containerStarts = new AtomicInteger();
        var clients = new ArrayList<RecordingAdminClient>();
        var extension = new CouchDbIntegrationExtension(
                () -> {
                    containerStarts.incrementAndGet();
                    return new CouchDbContainerResource(
                            new GenericContainer<>(DockerImageName.parse("couchdb:3.5")),
                            URI.create("http://localhost:49152"),
                            "admin",
                            "secret");
                },
                (serverUri, username, password) -> {
                    var client = new RecordingAdminClient(serverUri, username, password);
                    clients.add(client);
                    return client;
                },
                new CouchDbDatabaseNameGenerator());

        // when
        extension.beforeAll(firstContext);
        extension.beforeAll(secondContext);
        extension.afterAll(firstContext);
        extension.afterAll(secondContext);

        // then
        assertThat(containerStarts).hasValue(1);
        assertThat(clients)
                .hasSize(2)
                .extracting(client -> client.createdDatabase.databaseName())
                .doesNotHaveDuplicates();
        assertThat(clients).allSatisfy(client -> {
            assertThat(client.healthChecks).isOne();
            assertThat(client.deletedDatabaseName).isEqualTo(client.createdDatabase.databaseName());
        });
    }

    private ExtensionContext rootContext(ExtensionContext.Store rootStore) {
        return context((proxy, method, arguments) -> switch (method.getName()) {
            case "getRoot" -> proxy;
            case "getStore" -> rootStore;
            default -> null;
        });
    }

    private ExtensionContext classContext(
            ExtensionContext root,
            Class<?> testClass,
            ExtensionContext.Store classStore,
            Optional<Throwable> executionException) {
        return context((proxy, method, arguments) -> switch (method.getName()) {
            case "getRoot" -> root;
            case "getStore" -> classStore;
            case "getRequiredTestClass" -> testClass;
            case "getExecutionException" -> executionException;
            default -> null;
        });
    }

    private ExtensionContext context(Invocation invocation) {
        return (ExtensionContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {ExtensionContext.class},
                (proxy, method, arguments) -> invocation.invoke(proxy, method, arguments));
    }

    private interface Invocation {

        Object invoke(Object proxy, java.lang.reflect.Method method, Object[] arguments) throws Throwable;
    }

    private static final class RecordingAdminClient extends CouchDbAdminClient {

        private CouchDbTestDatabase createdDatabase;
        private String deletedDatabaseName;
        private int healthChecks;

        private RecordingAdminClient(URI serverUri, String username, String password) {
            super(
                    request -> new CouchDbHttpResponse(request.uri(), 200, "{\"status\":\"ok\"}"),
                    serverUri,
                    username,
                    password);
        }

        @Override
        void assertHealthy() {
            healthChecks++;
        }

        @Override
        CouchDbTestDatabase createDatabase(String databaseName) {
            createdDatabase = new CouchDbTestDatabase(
                    URI.create("http://localhost:49152"),
                    URI.create("http://localhost:49152/" + databaseName),
                    databaseName,
                    "admin",
                    "secret");
            return createdDatabase;
        }

        @Override
        void deleteDatabase(String databaseName) {
            deletedDatabaseName = databaseName;
        }
    }

    private static final class InMemoryStore implements ExtensionContext.Store {

        private final Map<Object, Object> values = new HashMap<>();

        @Override
        public Object get(Object key) {
            return values.get(key);
        }

        @Override
        public <V> V get(Object key, Class<V> requiredType) {
            return requiredType.cast(values.get(key));
        }

        @Override
        public <K, V> Object getOrComputeIfAbsent(K key, Function<? super K, ? extends V> creator) {
            return values.computeIfAbsent(key, ignored -> creator.apply(key));
        }

        @Override
        public <K, V> Object computeIfAbsent(K key, Function<? super K, ? extends V> creator) {
            return values.computeIfAbsent(key, ignored -> creator.apply(key));
        }

        @Override
        public <K, V> V getOrComputeIfAbsent(K key, Function<? super K, ? extends V> creator, Class<V> requiredType) {
            return requiredType.cast(getOrComputeIfAbsent(key, creator));
        }

        @Override
        public <K, V> V computeIfAbsent(K key, Function<? super K, ? extends V> creator, Class<V> requiredType) {
            return requiredType.cast(computeIfAbsent(key, creator));
        }

        @Override
        public void put(Object key, Object value) {
            values.put(key, value);
        }

        @Override
        public Object remove(Object key) {
            return values.remove(key);
        }

        @Override
        public <V> V remove(Object key, Class<V> requiredType) {
            return requiredType.cast(values.remove(key));
        }
    }

    private static final class FirstTestClass {}

    private static final class SecondTestClass {}
}
