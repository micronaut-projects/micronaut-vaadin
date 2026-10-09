from typing import Annotated

from jakarta.inject import Inject
from micronaut.context import BeanContext
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from micronaut.vaadin.docs.views.GreetingService import GreetingService
from micronaut.vaadin.docs.views.VisitCounter import VisitCounter


@MicronautTest(startApplication=False)
class GreetingServiceTest:
    greeting_service: Annotated[GreetingService, Inject]
    bean_context: Annotated[BeanContext, Inject]

    @Test
    def greets(self) -> None:
        assert self.greeting_service.greet("Micronaut") == "Hello Micronaut"

    @Test
    def the_visit_counter_is_ui_scoped(self) -> None:
        definition = self.bean_context.getBeanDefinition(VisitCounter)
        assert definition.getScopeName().orElse(None) == "io.micronaut.vaadin.annotation.UIScope"
