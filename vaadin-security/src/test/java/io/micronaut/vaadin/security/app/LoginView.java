package io.micronaut.vaadin.security.app;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@AnonymousAllowed
public class LoginView extends Span {
    public LoginView() {
        super("Please log in");
    }
}
