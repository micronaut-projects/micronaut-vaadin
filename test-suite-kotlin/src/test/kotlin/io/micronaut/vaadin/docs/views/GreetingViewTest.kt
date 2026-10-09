package io.micronaut.vaadin.docs.views

// tag::test[]
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.TextField
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.vaadin.browserless.MicronautBrowserlessTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@MicronautTest // <1>
class GreetingViewTest : MicronautBrowserlessTest() { // <2>

    @Test
    fun greetsTheVisitor() {
        navigate(GreetingView::class.java) // <3>
        test(`$`(TextField::class.java).single()).setValue("Micronaut")
        test(`$`(Button::class.java).single()).click()

        assertEquals("Hello Micronaut", `$`(Span::class.java).single().text)
        assertEquals(2, applicationContext.getBean(VisitCounter::class.java).increment()) // <4>
    }
}
// end::test[]
