from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.test.extensions.junit5.annotation import MicronautTest
from micronaut.vaadin import VaadinConfigurationProperties
from org.junit.jupiter.api import Test


@MicronautTest(startApplication=False)
@Property(name="vaadin.url-mapping", value="/ui/*")
@Property(name="vaadin.exclude-urls", value="/api/**,/health")
class VaadinConfigurationTest:

    # tag::config[]
    vaadin: Annotated[VaadinConfigurationProperties, Inject]  # <1>

    @Test
    def reads_the_vaadin_configuration(self) -> None:
        assert self.vaadin.getUrlMapping() == "/ui/*"  # <2>
        assert list(self.vaadin.getExcludeUrls()) == ["/api/**", "/health"]
    # end::config[]
