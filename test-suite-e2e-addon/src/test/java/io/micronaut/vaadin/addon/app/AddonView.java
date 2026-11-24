package io.micronaut.vaadin.addon.app;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import io.micronaut.vaadin.addon.HelloAddon;

@Route("")
public class AddonView extends Div {

    public AddonView() {
        HelloAddon addon = new HelloAddon();
        addon.setId("addon");
        add(addon);
    }
}
