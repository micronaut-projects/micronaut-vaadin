# Repository Guidance

Micronaut Vaadin integrates Vaadin Flow 25 with Micronaut 5.3, on the Netty server and on Micronaut Servlet containers. It was created from micronaut-project-template. Keep root guidance short and update it when the build changes.

## Repository Shape

- `vaadin-core/` is the transport-neutral core, published as `micronaut-vaadin-core` (`useStandardizedProjectNames`).
- `vaadin-bom/` is the BOM. It imports `com.vaadin:vaadin-bom`.
- `buildSrc/src/main/groovy/io.micronaut.build.internal.vaadin-*.gradle` holds the convention plugins: `vaadin-base`, `vaadin-module` for published modules, and `vaadin-tests` for the documentation suites.
- `test-suite`, `test-suite-kotlin`, `test-suite-groovy` and `test-suite-python` hold the guide samples in Java, Kotlin (KSP), Groovy and Python. Scala is intentionally excluded.
- `.agents/skills/` is shared agent guidance synced from the template.

## Versions

- Micronaut core is `5.3.0-SNAPSHOT`, from the Central snapshots repository (`addSnapshotRepository()` in `settings.gradle`). Move to the 5.3.0 release when it ships.
- Vaadin is 25 only (`managed-vaadin` in `gradle/libs.versions.toml`).

## Documentation

- User guide sources live in `src/main/docs/guide`, with navigation in `toc.yml`.
- Every sample is a `snippet::` with the same fully qualified name in each test suite. Python sources drop the leading `io` package: `io.micronaut.vaadin.docs.X` lives in `test-suite-python/src/test/python/micronaut/vaadin/docs/X.py`.
- Python view samples are an open experiment. Until it succeeds, view samples use `languages="java,kotlin,groovy"`.
- Build the guide with `./gradlew publishGuide`.

## Verification

- `./gradlew check` runs the JVM tests. Python tests run with `./gradlew pythonCheck` and in the `python.yml` workflow.
- Run `./gradlew publishGuide` after guide or `toc.yml` changes.

## Contributing Guidelines

- Before opening or updating a pull request, read `CONTRIBUTING.md` and follow every requirement it names.
