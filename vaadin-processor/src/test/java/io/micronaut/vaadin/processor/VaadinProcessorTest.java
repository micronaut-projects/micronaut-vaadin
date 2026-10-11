package io.micronaut.vaadin.processor;

import io.micronaut.annotation.processing.test.JavaParser;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bean definitions the processor creates for the Vaadin types of an application.
 */
class VaadinProcessorTest {

    @Test
    void aRouteIsABean() {
        assertTrue(hasBeanDefinition("example.HelloView", """
            package example;

            @com.vaadin.flow.router.Route("hello")
            public class HelloView extends com.vaadin.flow.component.Component {
            }
            """));
    }

    @Test
    void aLayoutIsABean() {
        assertTrue(hasBeanDefinition("example.MainLayout", """
            package example;

            @com.vaadin.flow.router.Layout
            public class MainLayout extends com.vaadin.flow.component.Component implements com.vaadin.flow.router.RouterLayout {
            }
            """));
    }

    @Test
    void anErrorViewWithoutAnnotationIsABean() {
        assertTrue(hasBeanDefinition("example.NotFound", """
            package example;

            public class NotFound extends com.vaadin.flow.component.Component
                implements com.vaadin.flow.router.HasErrorParameter<com.vaadin.flow.router.NotFoundException> {

                @Override
                public int setErrorParameter(com.vaadin.flow.router.BeforeEnterEvent event,
                                             com.vaadin.flow.router.ErrorParameter<com.vaadin.flow.router.NotFoundException> parameter) {
                    return 404;
                }
            }
            """));
    }

    @Test
    void anAbstractErrorViewIsNotABean() {
        assertFalse(hasBeanDefinition("example.AbstractNotFound", """
            package example;

            public abstract class AbstractNotFound extends com.vaadin.flow.component.Component
                implements com.vaadin.flow.router.HasErrorParameter<com.vaadin.flow.router.NotFoundException> {
            }
            """));
    }

    @Test
    void aComponentWithoutAnnotationIsNotABean() {
        assertFalse(hasBeanDefinition("example.Plain", """
            package example;

            public class Plain extends com.vaadin.flow.component.Component {
            }
            """));
    }

    private static boolean hasBeanDefinition(String className, String source) {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        List<String> generated = new ArrayList<>();
        try (JavaParser parser = new JavaParser()) {
            for (JavaFileObject file : parser.generate(className, source)) {
                generated.add(file.getName());
            }
        }
        // $HelloView$Definition, or $NotFound$NotFound0$Definition for a bean the processor associates with a class
        return generated.stream().anyMatch(name -> name.contains("/$" + simpleName + "$") && name.endsWith("$Definition.class"));
    }
}
