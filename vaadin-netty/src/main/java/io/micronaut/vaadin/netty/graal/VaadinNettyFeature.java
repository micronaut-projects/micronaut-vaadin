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
package io.micronaut.vaadin.netty.graal;

import io.micronaut.core.annotation.Internal;
import io.micronaut.vaadin.graal.JarClasses;
import io.micronaut.vaadin.netty.NettyAtmosphereSupport;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

/**
 * Registers what the Netty runtime of Vaadin loads by name in a native image:
 * <ul>
 *     <li>the async support of Atmosphere for Netty, which Atmosphere creates from its class name;</li>
 *     <li>the {@code FACTORY} fields of the classes that Caffeine generates for each kind of cache and cache
 *     entry, which Caffeine finds by name and reads with a {@code VarHandle} when the in-memory session store
 *     of Micronaut Session, which keeps Vaadin's sessions on Netty, builds its cache.</li>
 *     <li>the methods of Netty's {@code ReferenceCountUtil}, which its static initializer lists to exclude them
 *     from leak reports, when Micronaut compresses a response.</li>
 * </ul>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Internal
public final class VaadinNettyFeature implements Feature {

    private static final String CACHE_PACKAGE = "com.github.benmanes.caffeine.cache";
    private static final String FACTORY_FIELD = "FACTORY";
    private static final String REFERENCE_COUNT_UTIL = "io.netty.util.ReferenceCountUtil";

    @Override
    public String getDescription() {
        return "Registers the classes that the Netty runtime of Vaadin loads by name";
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        RuntimeReflection.register(NettyAtmosphereSupport.class);
        RuntimeReflection.register(NettyAtmosphereSupport.class.getDeclaredConstructors());
        Class<?> referenceCountUtil = access.findClassByName(REFERENCE_COUNT_UTIL);
        if (referenceCountUtil != null) {
            RuntimeReflection.registerAllDeclaredMethods(referenceCountUtil);
        }
        JarClasses.forEach(access, CACHE_PACKAGE + ".LocalCacheFactory", CACHE_PACKAGE,
            VaadinNettyFeature::isGeneratedClass, VaadinNettyFeature::registerFactory);
    }

    /**
     * The generated classes have names made of capital letters only, such as {@code SSLA} or {@code PSW}.
     */
    private static boolean isGeneratedClass(String className) {
        String name = className.substring(CACHE_PACKAGE.length() + 1);
        return !name.isEmpty() && name.chars().allMatch(c -> c >= 'A' && c <= 'Z');
    }

    private static void registerFactory(Class<?> type) {
        try {
            RuntimeReflection.register(type);
            RuntimeReflection.register(type.getDeclaredField(FACTORY_FIELD));
        } catch (NoSuchFieldException e) {
            // not a factory
        }
    }
}
