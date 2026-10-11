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
class LoginView : VerticalLayout(), BeforeEnterObserver {

    private val login = LoginForm()

    init {
        login.action = "login" // <2>
        add(login)
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        login.isError = event.location.queryParameters.parameters.containsKey("error") // <3>
    }
}
// end::login[]
