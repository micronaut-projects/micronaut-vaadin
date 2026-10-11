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
class AdminView extends VerticalLayout {

    AdminView(AuthenticationContext authentication) { // <2>
        add(new Span("Hello " + authentication.name.orElse("")),
            new Button("Log out", { event -> authentication.logout() })) // <3>
    }
}
// end::admin[]
