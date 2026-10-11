package io.micronaut.vaadin.security.app;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import jakarta.annotation.security.PermitAll;

@Route("profile")
@PermitAll
public class ProfileView extends Span {
    public ProfileView() {
        super("Hello " + VaadinRequest.getCurrent().getUserPrincipal().getName());
    }
}
