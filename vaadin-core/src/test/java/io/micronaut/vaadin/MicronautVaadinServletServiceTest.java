package io.micronaut.vaadin;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinServlet;
import io.micronaut.context.ApplicationContext;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The static resources of Vaadin, found in the web resources of the jars too.
 */
class MicronautVaadinServletServiceTest {

    private final ApplicationContext context = ApplicationContext.builder().start();

    @AfterEach
    void close() {
        context.close();
    }

    @Test
    void resourcesOfTheServletContextComeFirst() throws Exception {
        URL served = URI.create("file:/served.txt").toURL();
        assertEquals(served, service(served).getStaticResource("/web-resource.txt"));
    }

    @Test
    void theWebResourcesOfTheJarsAreAFallback() {
        URL resource = service(null).getStaticResource("/web-resource.txt");
        assertNotNull(resource);
        assertEquals(resource, getClass().getClassLoader().getResource("META-INF/resources/web-resource.txt"));
        assertNull(service(null).getStaticResource("/missing.txt"));
    }

    @Test
    void onlyAbsolutePathsWithinTheWebResources() {
        MicronautVaadinServletService service = service(null);
        assertNull(service.getStaticResource("web-resource.txt"));
        assertNull(service.getStaticResource("/../resources/web-resource.txt"));
    }

    private MicronautVaadinServletService service(URL servedResource) {
        ServletContext servletContext = proxy(ServletContext.class, (proxy, method, args) ->
            "getResource".equals(method.getName()) ? servedResource : defaultValue(method.getReturnType()));
        VaadinServlet servlet = new VaadinServlet() {
            @Override
            public ServletContext getServletContext() {
                return servletContext;
            }
        };
        DeploymentConfiguration configuration = proxy(DeploymentConfiguration.class,
            (proxy, method, args) -> defaultValue(method.getReturnType()));
        MicronautVaadinServletService service = new MicronautVaadinServletService(servlet, configuration, context);
        // set when the service initializes
        service.setClassLoader(getClass().getClassLoader());
        return service;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class || type == long.class) {
            return type == int.class ? (Object) 0 : (Object) 0L;
        }
        return null;
    }
}
