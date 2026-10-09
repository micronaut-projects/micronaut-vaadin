package io.micronaut.vaadin.browserless;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.server.VaadinService;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.vaadin.browserless.app.GreetingService;
import io.micronaut.vaadin.browserless.app.HelloView;
import io.micronaut.vaadin.browserless.app.LayoutState;
import io.micronaut.vaadin.browserless.app.MainLayout;
import io.micronaut.vaadin.browserless.app.OtherView;
import io.micronaut.vaadin.browserless.app.SessionState;
import io.micronaut.vaadin.browserless.app.UiCounter;
import io.micronaut.vaadin.browserless.app.WizardState;
import io.micronaut.vaadin.browserless.app.WizardView;
import io.micronaut.vaadin.MicronautInstantiator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

@MicronautTest
@Property(name = "vaadin.heartbeat-interval", value = "123")
class MicronautBrowserlessTestTest extends MicronautBrowserlessTest {

    @Test
    void viewsReceiveDependencyInjection() {
        HelloView view = navigate(HelloView.class);
        assertEquals(getApplicationContext().getBean(GreetingService.class).greet("Micronaut"), view.getGreeting().getText());
        assertInstanceOf(MicronautInstantiator.class, VaadinService.getCurrent().getInstantiator());
    }

    @Test
    void uiScopedBeansLiveAsLongAsTheUI() {
        HelloView view = navigate(HelloView.class);
        test($(Button.class).single()).click();
        test($(Button.class).single()).click();
        assertEquals("2", view.getCount().getText());
        assertSame(view.getCounter(), getApplicationContext().getBean(UiCounter.class));

        // navigating to the current route keeps the view, so leave it first
        navigate(OtherView.class);
        HelloView again = navigate(HelloView.class);
        assertNotSame(view, again);
        assertSame(view.getCounter(), again.getCounter());
    }

    @Test
    void sessionScopedBeansLiveAsLongAsTheSession() {
        HelloView view = navigate(HelloView.class);
        assertSame(view.getSession(), getApplicationContext().getBean(SessionState.class));
        assertEquals(view.getSession().getId(), navigate(HelloView.class).getSession().getId());
    }

    @Test
    void routeScopedBeansAreDestroyedWhenTheirOwnerLeavesTheNavigationChain() {
        WizardView wizard = navigate(WizardView.class);
        WizardState state = wizard.getState();
        int destroyed = WizardState.DESTROYED.get();

        navigate(OtherView.class);
        assertEquals(destroyed + 1, WizardState.DESTROYED.get());

        assertNotSame(state, navigate(WizardView.class).getState());
    }

    @Test
    void routeScopedBeansOwnedByALayoutSurviveNavigationUnderIt() {
        navigate(HelloView.class);
        LayoutState state = layout().getState();
        navigate(OtherView.class);
        assertSame(state, layout().getState());
    }

    @Test
    void vaadinInitParametersComeFromTheConfiguration() {
        assertEquals(123, VaadinService.getCurrent().getDeploymentConfiguration().getHeartbeatInterval());
    }

    private static MainLayout layout() {
        return UI.getCurrent().getInternals().getActiveRouterTargetsChain().stream()
            .filter(MainLayout.class::isInstance)
            .map(MainLayout.class::cast)
            .findFirst()
            .orElseThrow();
    }
}
