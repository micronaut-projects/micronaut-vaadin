package io.micronaut.vaadin.hotdeploy;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("")
@CssImport("./hotdeploy.css")
public class HotDeployView extends Span {

    public HotDeployView() {
        super("Styled by Vite");
        setId("styled");
        addClassName("hotdeploy");
    }
}
