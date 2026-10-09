<!-- Checklist: https://github.com/micronaut-projects/micronaut-core/wiki/New-Module-Checklist -->

# Micronaut Vaadin

[![Maven Central](https://img.shields.io/maven-central/v/io.micronaut.vaadin/micronaut-vaadin-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.micronaut.vaadin%22%20AND%20a:%22micronaut-vaadin-core%22)
[![Build Status](https://github.com/micronaut-projects/micronaut-vaadin/workflows/Java%20CI/badge.svg)](https://github.com/micronaut-projects/micronaut-vaadin/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=micronaut-projects_micronaut-vaadin&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=micronaut-projects_micronaut-vaadin)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.micronaut.io/scans)

Micronaut Vaadin integrates [Vaadin Flow](https://vaadin.com/flow) with Micronaut, on both the Netty server and the servlet containers of [Micronaut Servlet](https://github.com/micronaut-projects/micronaut-servlet). It targets Vaadin 25 and Micronaut 5.3.

This project is under development.

## Documentation

See the [Documentation](https://micronaut-projects.github.io/micronaut-vaadin/latest/guide/) for more information.

See the [Snapshot Documentation](https://micronaut-projects.github.io/micronaut-vaadin/snapshot/guide/) for the current development docs.

## Snapshots and Releases

Snapshots are automatically published to [Central Snapshots](https://central.sonatype.com/repository/maven-snapshots/io/micronaut/vaadin/) using [GitHub Actions](https://github.com/micronaut-projects/micronaut-vaadin/actions).

See the documentation in the [Micronaut Docs](https://docs.micronaut.io/latest/guide/index.html#usingsnapshots) for how to configure your build to use snapshots.

Releases are published to Maven Central via [GitHub Actions](https://github.com/micronaut-projects/micronaut-vaadin/actions).

Releases are completely automated. To perform a release use the following steps:

* [Publish the draft release](https://github.com/micronaut-projects/micronaut-vaadin/releases). There should be already a draft release created, edit and publish it. The Git Tag should start with `v`. For example `v1.0.0`.
* [Monitor the Workflow](https://github.com/micronaut-projects/micronaut-vaadin/actions?query=workflow%3ARelease) to check it passed successfully.
* If everything went fine, [publish to Maven Central](https://github.com/micronaut-projects/micronaut-vaadin/actions?query=workflow%3A"Maven+Central+Sync").
* Celebrate!
