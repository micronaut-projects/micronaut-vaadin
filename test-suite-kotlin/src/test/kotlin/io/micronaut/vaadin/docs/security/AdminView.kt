package io.micronaut.vaadin.docs.security

// tag::admin[]
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import io.micronaut.vaadin.security.AuthenticationContext
import jakarta.annotation.security.RolesAllowed

@Route("admin")
@RolesAllowed("ADMIN") // <1>
class AdminView(authentication: AuthenticationContext) : VerticalLayout() { // <2>

    init {
        add(Span("Hello " + authentication.name.orElse("")),
            Button("Log out") { authentication.logout() }) // <3>
    }
}
// end::admin[]
