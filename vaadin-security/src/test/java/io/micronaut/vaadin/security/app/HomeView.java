package io.micronaut.vaadin.security.app;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("")
@AnonymousAllowed
public class HomeView extends Span {
    public HomeView() {
        super("Welcome");
    }
}
