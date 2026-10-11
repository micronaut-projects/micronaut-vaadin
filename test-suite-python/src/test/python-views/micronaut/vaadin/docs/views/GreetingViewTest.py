from com.vaadin.flow.component.button import Button
from com.vaadin.flow.component.html import Span
from com.vaadin.flow.component.textfield import TextField
from micronaut.test.extensions.junit5.annotation import MicronautTest
from micronaut.vaadin.browserless import MicronautBrowserlessTest
from org.junit.jupiter.api import Test

from micronaut.vaadin.docs.views.GreetingView import GreetingView
from micronaut.vaadin.docs.views.VisitCounter import VisitCounter


# tag::test[]
@MicronautTest  # <1>
class GreetingViewTest(MicronautBrowserlessTest):  # <2>

    @Test
    def greets_the_visitor(self) -> None:
        self.navigate(GreetingView)  # <3>
        self.test(self.find(TextField).single()).setValue("Micronaut")
        self.test(self.find(Button).single()).click()

        assert self.find(Span).single().getText() == "Hello Micronaut"
        assert self.getApplicationContext().getBean(VisitCounter).increment() == 2  # <4>
# end::test[]
