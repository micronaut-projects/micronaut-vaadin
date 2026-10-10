/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.vaadin.browserless;

import com.vaadin.browserless.BaseBrowserlessTest;
import com.vaadin.browserless.BrowserlessConfiguration;
import com.vaadin.browserless.BrowserlessTestConfigExtension;
import com.vaadin.browserless.TesterWrappers;
import com.vaadin.browserless.internal.MockRouteNotFoundError;
import com.vaadin.browserless.internal.MockVaadin;
import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.mocks.MockedUI;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.RouterLayout;
import io.micronaut.context.ApplicationContext;
import io.micronaut.vaadin.startup.VaadinTypeIndex;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Base class of browserless UI tests for Micronaut Vaadin applications. Annotate the test with
 * {@code @MicronautTest}: views are created by the application context, so they receive dependency
 * injection, and the Vaadin scopes work as in the running application.
 *
 * <p>The routes are the views known to the application at compile time: no classpath scanning
 * takes place.</p>
 *
 * <pre>{@code
 * @MicronautTest
 * class HelloViewTest extends MicronautBrowserlessTest {
 *     @Test
 *     void greets() {
 *         HelloView view = navigate(HelloView.class);
 *         ...
 *     }
 * }
 * }</pre>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@ExtendWith(BrowserlessTestConfigExtension.class)
public abstract class MicronautBrowserlessTest extends BaseBrowserlessTest implements TesterWrappers {

    private @Nullable ApplicationContext applicationContext;

    /**
     * Receives the application context of the test.
     *
     * @param applicationContext The application context
     */
    @Inject
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * @return The application context of the test
     */
    protected ApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            throw new IllegalStateException("No application context: annotate the test class with @MicronautTest");
        }
        return applicationContext;
    }

    @Override
    protected final String testingEngine() {
        return "JUnit 6";
    }

    @BeforeEach
    @Override
    protected void initVaadinEnvironment() {
        scanTesters();
        MockMicronautServlet servlet = new MockMicronautServlet(discoverRoutes(), getApplicationContext(), MockedUI::new);
        BrowserlessConfiguration configuration = testConfiguration();
        MockVaadin.setup(MockedUI::new, servlet, allLookupServices(configuration), configuration);
        initSignalsSupport();
    }

    @AfterEach
    @Override
    protected void cleanVaadinEnvironment() {
        super.cleanVaadinEnvironment();
    }

    /**
     * The routes of the application, from its compile-time index of Vaadin types.
     *
     * @return The routes
     */
    @Override
    @SuppressWarnings("unchecked")
    protected synchronized Routes discoverRoutes() {
        VaadinTypeIndex index = getApplicationContext().getBean(VaadinTypeIndex.class);
        Set<Class<? extends Component>> routes = new LinkedHashSet<>(index.getRoutes());
        Set<Class<? extends HasErrorParameter<?>>> errorRoutes = new LinkedHashSet<>();
        errorRoutes.add(MockRouteNotFoundError.class);
        errorRoutes.addAll(index.getErrorViews());
        Set<Class<? extends RouterLayout>> layouts = new LinkedHashSet<>();
        for (Class<? extends Component> layout : index.getLayouts()) {
            if (RouterLayout.class.isAssignableFrom(layout)) {
                layouts.add((Class<? extends RouterLayout>) layout);
            }
        }
        return new Routes(routes, errorRoutes, layouts, true);
    }
}
