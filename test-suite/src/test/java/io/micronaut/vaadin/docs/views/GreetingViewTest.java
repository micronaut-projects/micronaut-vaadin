package io.micronaut.vaadin.docs.views;

// tag::test[]
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.vaadin.browserless.MicronautBrowserlessTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest // <1>
class GreetingViewTest extends MicronautBrowserlessTest { // <2>

    @Test
    void greetsTheVisitor() {
        navigate(GreetingView.class); // <3>
        test(find(TextField.class).single()).setValue("Micronaut");
        test(find(Button.class).single()).click();

        assertEquals("Hello Micronaut", find(Span.class).single().getText());
        assertEquals(2, getApplicationContext().getBean(VisitCounter.class).increment()); // <4>
    }
}
// end::test[]
