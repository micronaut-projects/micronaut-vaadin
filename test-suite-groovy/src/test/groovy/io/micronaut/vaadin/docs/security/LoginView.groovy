package io.micronaut.vaadin.docs.security

// tag::login[]
import com.vaadin.flow.component.login.LoginForm
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed

@Route("login")
@AnonymousAllowed // <1>
class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm()

    LoginView() {
        login.action = "login" // <2>
        add(login)
    }

    @Override
    void beforeEnter(BeforeEnterEvent event) {
        login.error = event.location.queryParameters.parameters.containsKey("error") // <3>
    }
}
// end::login[]
