package io.micronaut.vaadin.e2e.security.app;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.micronaut.vaadin.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;

@Route("admin")
@RolesAllowed("ADMIN")
public class AdminView extends VerticalLayout {
    public AdminView(AuthenticationContext authentication) {
        Span greeting = new Span("Admin area for " + authentication.getName().orElse("nobody"));
        greeting.setId("greeting");
        Button logout = new Button("Log out", event -> authentication.logout());
        logout.setId("logout");
        add(greeting, logout);
    }
}
