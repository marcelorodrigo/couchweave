package io.github.marcelorodrigo.couchweave.testsupport.couchdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterContext;

class CouchDbIntegrationExtensionTest {

    private final CouchDbIntegrationExtension extension = new CouchDbIntegrationExtension();

    @Test
    @DisplayName("should support CouchDB database parameters")
    void shouldSupportCouchDbDatabaseParameters() throws NoSuchMethodException {
        // given
        var parameterContext = parameterContextFor("usesCouchDbDatabase", CouchDbTestDatabase.class);

        // when
        var supported = extension.supportsParameter(parameterContext, null);

        // then
        assertThat(supported).isTrue();
    }

    @Test
    @DisplayName("should reject unsupported parameters")
    void shouldRejectUnsupportedParameters() throws NoSuchMethodException {
        // given
        var parameterContext = parameterContextFor("usesString", String.class);

        // when
        var supported = extension.supportsParameter(parameterContext, null);

        // then
        assertThat(supported).isFalse();
    }

    private ParameterContext parameterContextFor(String methodName, Class<?> parameterType)
            throws NoSuchMethodException {
        var method = getClass().getDeclaredMethod(methodName, parameterType);
        return (ParameterContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {ParameterContext.class},
                (proxy, invokedMethod, arguments) ->
                        invokedMethod.getName().equals("getParameter") ? method.getParameters()[0] : null);
    }

    @SuppressWarnings("unused")
    private void usesCouchDbDatabase(CouchDbTestDatabase database) {}

    @SuppressWarnings("unused")
    private void usesString(String value) {}
}
