package io.micronaut.vaadin.docs.views

// tag::test[]
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.TextField
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.vaadin.browserless.MicronautBrowserlessTest
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

@MicronautTest // <1>
class GreetingViewTest extends MicronautBrowserlessTest { // <2>

    @Test
    void greetsTheVisitor() {
        navigate(GreetingView) // <3>
        test($(TextField).single()).value = "Micronaut"
        test($(Button).single()).click()

        assertEquals("Hello Micronaut", $(Span).single().text)
        assertEquals(2, applicationContext.getBean(VisitCounter).increment()) // <4>
    }
}
// end::test[]
