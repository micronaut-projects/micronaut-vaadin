from com.vaadin.flow.component.button import Button
from com.vaadin.flow.component.html import Span
from com.vaadin.flow.component.orderedlayout import VerticalLayout
from com.vaadin.flow.component.textfield import TextField
from com.vaadin.flow.router import Route

from micronaut.vaadin.docs.views.GreetingService import GreetingService
from micronaut.vaadin.docs.views.VisitCounter import VisitCounter


# tag::view[]
@Route("greeting")  # <1>
class GreetingView(VerticalLayout):

    def __init__(self, greeting_service: GreetingService, counter: VisitCounter):  # <2>
        super().__init__()
        name = TextField("Name")
        greeting = Span()

        def greet(event) -> None:
            counter.increment()
            greeting.setText(greeting_service.greet(name.getValue()))

        self.add(name, Button("Greet", greet), greeting)
# end::view[]
