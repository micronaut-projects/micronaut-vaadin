from com.vaadin.flow.component.login import LoginForm
from com.vaadin.flow.component.orderedlayout import VerticalLayout
from com.vaadin.flow.router import BeforeEnterEvent, BeforeEnterObserver, Route
from com.vaadin.flow.server.auth import AnonymousAllowed


# tag::login[]
@Route("login")
@AnonymousAllowed  # <1>
class LoginView(VerticalLayout, BeforeEnterObserver):

    def __init__(self):
        super().__init__()
        self.login = LoginForm()
        self.login.setAction("login")  # <2>
        self.add(self.login)

    def beforeEnter(self, event: BeforeEnterEvent) -> None:
        self.login.setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"))  # <3>
# end::login[]
