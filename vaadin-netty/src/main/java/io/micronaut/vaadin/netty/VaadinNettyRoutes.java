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
package io.micronaut.vaadin.netty;

import com.vaadin.flow.server.Constants;
import io.micronaut.core.util.PathMatcher;
import io.micronaut.http.HttpRequest;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.vaadin.VaadinConfigurationProperties;
import io.micronaut.web.router.builder.HttpRouteBuilder;
import io.micronaut.web.router.builder.HttpRouteGroup;
import io.micronaut.web.router.builder.HttpRoutes;
import io.micronaut.web.router.builder.RouteCondition;
import io.micronaut.web.router.resource.StaticResourceResolver;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;

/**
 * The routes of Vaadin on Netty: every path under the mapping of Vaadin, and its frontend resources
 * under {@code /VAADIN}, go to the Vaadin servlet on the blocking executor. Vaadin holds the lock of the
 * session while it runs application code, which must never happen on an event loop.
 *
 * <p>When Vaadin serves the root of the application, the routes of the controllers win, and the paths of
 * static resources and of {@code vaadin.exclude-urls} stay with Micronaut.</p>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Singleton
final class VaadinNettyRoutes implements HttpRoutes {

    private final VaadinNettyRuntime runtime;
    private final StaticResourceResolver staticResources;
    private final List<String> excludeUrls;

    VaadinNettyRoutes(VaadinNettyRuntime runtime,
                      VaadinConfigurationProperties configuration,
                      Optional<StaticResourceResolver> staticResources) {
        this.runtime = runtime;
        this.staticResources = staticResources.orElse(StaticResourceResolver.EMPTY);
        this.excludeUrls = List.copyOf(configuration.getExcludeUrls());
    }

    @Override
    public void routes(HttpRouteBuilder routes) {
        String prefix = runtime.getPrefix();
        routes.group(group -> {
            route(group, prefix.isEmpty() ? "/" : prefix);
            route(group, prefix + "/{+path}");
            if (!prefix.isEmpty()) {
                route(group, "/" + Constants.VAADIN_MAPPING + "{+path}");
            }
        });
    }

    private void route(HttpRouteGroup group, String uri) {
        group.any(uri)
            .executeOn(TaskExecutors.BLOCKING)
            .where(RouteCondition.custom(this::handledByVaadin))
            .body()
            .handleAsync((request, pathVariables, body) -> runtime.service(request, body));
    }

    private boolean handledByVaadin(HttpRequest<?> request) {
        String path = request.getPath();
        for (String pattern : excludeUrls) {
            if (PathMatcher.ANT.matches(pattern, path)) {
                return false;
            }
        }
        return "/".equals(path) || staticResources.resolve(path).isEmpty();
    }
}
