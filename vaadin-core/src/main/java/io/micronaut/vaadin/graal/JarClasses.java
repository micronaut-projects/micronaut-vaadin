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
package io.micronaut.vaadin.graal;

import io.micronaut.core.annotation.Internal;
import org.graalvm.nativeimage.hosted.Feature;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Lists the classes of the jar of a library at native image build time, for the libraries that load their
 * own classes by name.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Internal
public final class JarClasses {

    private static final String CLASS_SUFFIX = ".class";

    private JarClasses() {
    }

    /**
     * Calls the consumer with every class of the jar that holds the anchor class, when the class is on the
     * class path of the image.
     *
     * @param access The access of the feature
     * @param anchorClassName The name of a class of the jar
     * @param packageName The package to list, with its sub-packages, such as {@code org.atmosphere}
     * @param classNameFilter Which classes, by their fully qualified name, to list
     * @param consumer What to do with each class
     */
    public static void forEach(Feature.FeatureAccess access,
                               String anchorClassName,
                               String packageName,
                               Predicate<String> classNameFilter,
                               Consumer<Class<?>> consumer) {
        Class<?> anchor = access.findClassByName(anchorClassName);
        if (anchor == null) {
            return;
        }
        CodeSource codeSource = anchor.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return;
        }
        Path location;
        try {
            location = Path.of(codeSource.getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        scan(access, location, packageName, classNameFilter, consumer);
    }

    /**
     * Calls the consumer with every class of a package in the jars of the class path of the image.
     *
     * @param access The access of the feature
     * @param packageName The package to list, with its sub-packages, such as {@code com.vaadin.flow.component}
     * @param classNameFilter Which classes, by their fully qualified name, to list
     * @param consumer What to do with each class
     */
    public static void forEachOnClassPath(Feature.FeatureAccess access,
                                          String packageName,
                                          Predicate<String> classNameFilter,
                                          Consumer<Class<?>> consumer) {
        for (Path location : access.getApplicationClassPath()) {
            scan(access, location, packageName, classNameFilter, consumer);
        }
    }

    private static void scan(Feature.FeatureAccess access,
                             Path location,
                             String packageName,
                             Predicate<String> classNameFilter,
                             Consumer<Class<?>> consumer) {
        if (!Files.isRegularFile(location)) {
            return;
        }
        String prefix = packageName.replace('.', '/') + '/';
        try (JarFile jar = new JarFile(location.toFile())) {
            jar.stream()
                .map(JarEntry::getName)
                .filter(name -> name.startsWith(prefix) && name.endsWith(CLASS_SUFFIX))
                .map(name -> name.substring(0, name.length() - CLASS_SUFFIX.length()).replace('/', '.'))
                .filter(classNameFilter)
                .map(access::findClassByName)
                .filter(Objects::nonNull)
                .forEach(consumer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
