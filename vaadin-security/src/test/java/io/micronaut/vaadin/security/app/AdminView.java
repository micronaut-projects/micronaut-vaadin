package io.micronaut.vaadin.security.app;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route("admin")
@RolesAllowed("ADMIN")
public class AdminView extends Span {
    public AdminView() {
        super("Admin area");
    }
}
