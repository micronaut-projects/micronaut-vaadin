from com.vaadin.flow.component.login import LoginForm
from micronaut.context.annotation import Property
from micronaut.test.extensions.junit5.annotation import MicronautTest
from micronaut.vaadin.browserless import MicronautBrowserlessTest
from org.junit.jupiter.api import Test

from micronaut.vaadin.docs.security.LoginView import LoginView


@MicronautTest
@Property(name="vaadin.security.enabled", value="true")
@Property(name="vaadin.security.login-view", value="/login")
class AccessControlTest(MicronautBrowserlessTest):

    @Test
    def an_anonymous_visitor_of_an_admin_view_signs_in_first(self) -> None:
        self.navigate("admin", LoginView)

        form = self.find(LoginForm).single()
        assert form.getAction() == "login"
        assert not form.isError()

    @Test
    def the_login_view_shows_a_failed_sign_in(self) -> None:
        self.navigate("login?error", LoginView)

        assert self.find(LoginForm).single().isError()
