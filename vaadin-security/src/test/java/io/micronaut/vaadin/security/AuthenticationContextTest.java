package io.micronaut.vaadin.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinRequest;
import io.micronaut.security.authentication.Authentication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The user of the current Vaadin request.
 */
class AuthenticationContextTest {

    private final VaadinSecurityConfiguration configuration = new VaadinSecurityConfiguration();
    private final AuthenticationContext context = new AuthenticationContext(configuration);

    @AfterEach
    void clear() {
        CurrentInstance.clearAll();
    }

    @Test
    void withoutARequestNobodyIsSignedIn() {
        assertSignedOut();
    }

    @Test
    void aPrincipalThatIsNotAnAuthenticationIsNotSignedIn() {
        currentRequest(() -> "someone");
        assertSignedOut();
    }

    @Test
    void theAuthenticationOfTheRequest() {
        currentRequest(Authentication.build("user", List.of("ADMIN"), Map.of()));
        assertTrue(context.isAuthenticated());
        assertEquals(Optional.of("user"), context.getName());
        assertTrue(context.hasRole("ADMIN"));
        assertFalse(context.hasRole("OTHER"));
    }

    @Test
    void loggingOutGoesToTheLogoutPath() {
        assertEquals(VaadinSecurityConfiguration.DEFAULT_LOGOUT_PATH, configuration.getLogoutPath());
        configuration.setLogoutPath("/sign-out");
        List<String> locations = new ArrayList<>();
        UI.setCurrent(new UI() {
            private final Page page = new Page(this) {
                @Override
                public void setLocation(String uri) {
                    locations.add(uri);
                }
            };

            @Override
            public Page getPage() {
                return page;
            }
        });

        context.logout();

        assertEquals(List.of("/sign-out"), locations);
    }

    private void assertSignedOut() {
        assertFalse(context.isAuthenticated());
        assertEquals(Optional.empty(), context.getName());
        assertFalse(context.hasRole("ADMIN"));
    }

    private static void currentRequest(Principal principal) {
        VaadinRequest request = (VaadinRequest) Proxy.newProxyInstance(VaadinRequest.class.getClassLoader(),
            new Class<?>[] {VaadinRequest.class},
            (proxy, method, args) -> "getUserPrincipal".equals(method.getName()) ? principal : null);
        CurrentInstance.set(VaadinRequest.class, request);
    }
}
