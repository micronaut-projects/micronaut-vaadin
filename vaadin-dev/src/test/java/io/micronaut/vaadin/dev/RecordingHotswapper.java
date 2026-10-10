package io.micronaut.vaadin.dev;

import com.vaadin.base.devserver.hotswap.VaadinHotswapper;
import com.vaadin.flow.server.VaadinService;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Records the classes that reach Flow's hotswap.
 */
public class RecordingHotswapper implements VaadinHotswapper {

    static final Set<String> REDEFINED = new CopyOnWriteArraySet<>();

    @Override
    public boolean onClassLoadEvent(VaadinService service, Set<Class<?>> classes, boolean redefined) {
        if (redefined) {
            classes.forEach(type -> REDEFINED.add(type.getName()));
        }
        return false;
    }
}
