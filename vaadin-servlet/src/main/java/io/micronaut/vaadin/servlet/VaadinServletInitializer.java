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
package io.micronaut.vaadin.servlet;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinServlet;
import io.micronaut.context.ApplicationContext;
import io.micronaut.vaadin.MicronautVaadinServlet;
import io.micronaut.vaadin.VaadinConfigurationProperties;
import io.micronaut.vaadin.startup.VaadinStartup;
import io.micronaut.web.router.Router;
import io.micronaut.web.router.resource.StaticResourceResolver;
import jakarta.inject.Singleton;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.atmosphere.cpr.ApplicationConfig;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Registers the Vaadin servlet with the servlet container and starts Vaadin with the types known at
 * compile time.
 *
 * <p>With the default URL mapping, {@code /*}, Vaadin serves the root of the application next to its
 * controllers: the servlet is mapped to {@value #ROOT_MAPPING_SERVLET_PATH}, and the {@link VaadinRootFilter}
 * forwards it the requests that no route, static resource or excluded URL handles.</p>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Singleton
public class VaadinServletInitializer implements ServletContainerInitializer {

    /**
     * The name of the Vaadin servlet.
     */
    public static final String SERVLET_NAME = "vaadinServlet";

    /**
     * The mapping of the Vaadin servlet when Vaadin serves the root of the application.
     */
    public static final String ROOT_MAPPING_SERVLET_PATH = "/vaadinServlet/*";

    private final ApplicationContext applicationContext;
    private final VaadinConfigurationProperties configuration;
    private final VaadinStartup startup;

    /**
     * @param applicationContext The application context
     * @param configuration      The Vaadin configuration
     * @param startup            Starts Vaadin
     */
    public VaadinServletInitializer(ApplicationContext applicationContext,
                                    VaadinConfigurationProperties configuration,
                                    VaadinStartup startup) {
        this.applicationContext = applicationContext;
        this.configuration = configuration;
        this.startup = startup;
    }

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
        startup.initialize(servletContext);

        String mapping = configuration.getUrlMapping();
        boolean rootMapping = isRootMapping(mapping);
        ServletRegistration.Dynamic registration = servletContext.addServlet(SERVLET_NAME, new MicronautVaadinServlet(applicationContext, rootMapping));
        registration.setAsyncSupported(configuration.isAsyncSupported());
        registration.setLoadOnStartup(configuration.isLoadOnStartup() ? 1 : -1);
        registration.setMultipartConfig(new MultipartConfigElement((String) null));
        String pushPath;
        if (rootMapping) {
            registration.addMapping(ROOT_MAPPING_SERVLET_PATH);
            registration.setInitParameter(VaadinServlet.INTERNAL_VAADIN_SERVLET_VITE_DEV_MODE_FRONTEND_PATH, "");
            pushPath = "/" + Constants.PUSH_MAPPING;
        } else {
            // the frontend resources live under /VAADIN whatever the mapping of the views
            registration.addMapping(mapping, "/" + Constants.VAADIN_MAPPING + "*");
            pushPath = mapping.substring(0, mapping.length() - 2) + "/" + Constants.PUSH_MAPPING;
        }
        registration.setInitParameter(ApplicationConfig.JSR356_MAPPING_PATH, pushPath);

        if (rootMapping) {
            Optional<StaticResourceResolver> staticResources = applicationContext.findBean(StaticResourceResolver.class);
            VaadinRootFilter filter = new VaadinRootFilter(
                applicationContext.getBean(Router.class),
                staticResources.orElse(StaticResourceResolver.EMPTY),
                configuration.getExcludeUrls()
            );
            FilterRegistration.Dynamic filterRegistration = servletContext.addFilter(VaadinRootFilter.NAME, filter);
            filterRegistration.setAsyncSupported(true);
            filterRegistration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
        }
    }

    private static boolean isRootMapping(String mapping) {
        return "/*".equals(mapping) || "/".equals(mapping);
    }
}
