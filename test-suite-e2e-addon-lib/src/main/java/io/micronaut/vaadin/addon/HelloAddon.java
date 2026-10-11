package io.micronaut.vaadin.addon;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

/**
 * A component whose element is defined by the frontend module of the add-on.
 */
@Tag("hello-addon")
@JsModule("./hello-addon.js")
public class HelloAddon extends Component {
}
