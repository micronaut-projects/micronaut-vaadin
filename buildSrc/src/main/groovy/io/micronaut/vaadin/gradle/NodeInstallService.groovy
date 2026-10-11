package io.micronaut.vaadin.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Task
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Vaadin installs Node.js into ~/.vaadin when it builds a frontend and finds no Node.js of its own: two builds
 * installing it at the same time fail on the shared directory. The tasks that may install it run one at a time.
 */
@CompileStatic
abstract class NodeInstallService implements BuildService<BuildServiceParameters.None> {

    /**
     * Runs a task alone among the tasks that may install Node.js.
     *
     * @param task A task that may install Node.js
     */
    static void serialize(Task task) {
        def service = task.project.gradle.sharedServices.registerIfAbsent('vaadinNodeInstall', NodeInstallService) {
            it.maxParallelUsages.set(1)
        }
        task.usesService(service)
    }
}
