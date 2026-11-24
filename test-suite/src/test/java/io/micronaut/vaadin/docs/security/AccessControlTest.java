package io.micronaut.vaadin.docs.security;

import com.vaadin.flow.component.login.LoginForm;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.vaadin.browserless.MicronautBrowserlessTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
@Property(name = "vaadin.security.enabled", value = "true")
@Property(name = "vaadin.security.login-view", value = "/login")
class AccessControlTest extends MicronautBrowserlessTest {

    @Test
    void anAnonymousVisitorOfAnAdminViewSignsInFirst() {
        navigate("admin", LoginView.class);

        LoginForm form = $(LoginForm.class).single();
        assertEquals("login", form.getAction());
        assertFalse(form.isError());
    }

    @Test
    void theLoginViewShowsAFailedSignIn() {
        navigate("login?error", LoginView.class);

        assertTrue($(LoginForm.class).single().isError());
    }
}
