from com.vaadin.flow.component.button import Button
from com.vaadin.flow.component.html import Span
from com.vaadin.flow.component.orderedlayout import VerticalLayout
from com.vaadin.flow.router import Route
from micronaut.vaadin.security import AuthenticationContext
from jakarta.annotation.security import RolesAllowed


# tag::admin[]
@Route("admin")
@RolesAllowed("ADMIN")  # <1>
class AdminView(VerticalLayout):

    def __init__(self, authentication: AuthenticationContext):  # <2>
        super().__init__()
        self.add(Span("Hello " + authentication.getName().orElse("")),
                 Button("Log out", lambda event: authentication.logout()))  # <3>
# end::admin[]
