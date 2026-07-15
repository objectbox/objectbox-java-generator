## What this repository is

The ObjectBox Gradle Plugin and its supporting build tools: a Gradle plugin (`io.objectbox` and `io.objectbox.sync`), an annotation processor, a code generator, and byte-code transformers.
All are published as Maven artifacts under the group `io.objectbox`. Builds with JDK 21 (toolchains auto-download via the foojay resolver).

## Snapshot dependencies (build prerequisite)

Subprojects depend on snapshot versions of `objectbox-java` (e.g. `5.4.3-dev-SNAPSHOT`).
These resolve from the internal ObjectBox GitLab repository, which is only added when the Gradle properties `gitlabUrl` and `gitlabToken` (or `gitlabPrivateToken`) are set — e.g. in `~/.gradle/gradle.properties`. `mavenLocal()` is a fallback (build `objectbox-java` locally with `./gradlew publishToMavenLocal`).
Without one of these, the build fails to resolve dependencies.

Setting `OBX_RELEASE=true` in the environment makes the build use and produce release versions (no `-dev-SNAPSHOT` suffix) instead.

## Commands

```bash
./gradlew build  # build everything
./gradlew check  # all tests, including TestKit integration tests (what CI runs)

# Unit tests of one subproject
./gradlew :objectbox-processor:test
./gradlew :objectbox-gradle-plugin:test

# A single test class / method
./gradlew :objectbox-processor:test --tests "io.objectbox.processor.ObjectBoxProcessorTest"
./gradlew :objectbox-gradle-plugin:test --tests "io.objectbox.gradle.PluginApplyTest.someMethod"

# Android Gradle Plugin integration tests (separate source sets in objectbox-gradle-plugin)
./gradlew :objectbox-gradle-plugin:agp73Test    # lowest supported AGP API
./gradlew :objectbox-gradle-plugin:agp81Test    # latest implemented AGP API
```

TestKit tests (`GradleTestRunner`-based tests and the `agpXYTest` tasks) build throwaway Gradle projects:
they first publish the other subprojects to a local test repository and receive the `gitlab*` properties as system properties, so they also need the GitLab credentials to be set.
Some of them build an actual Android project, which requires `ANDROID_HOME` to point to an Android SDK — if it is not set, `check` fails late with "SDK location not found" in a single `agp81Test` test.
A 401 Unauthorized when resolving `io.objectbox` snapshots means the GitLab token is expired/invalid (verify with `curl -H "Private-Token: $token" <gitlabUrl>/api/v4/user`).

Versioning/release: `versionNumber` in the root `build.gradle.kts` is the single source of truth; change it via `scripts/set-version.sh` (use `--release` to also tag).
CI (GitLab, see `.gitlab-ci.yml`) publishes; snapshot versions get `-<branch>-SNAPSHOT` appended via the `versionSuffix` property.
The `Jenkinsfile` at the repo root is an obsolete leftover from the previous CI setup — ignore it.

## Subprojects and how they relate

Dependency chain (see `settings.gradle.kts`):

- **objectbox-gradle-plugin** — the actual Gradle plugins. `ObjectBoxGradlePlugin` (id `io.objectbox`) does the work; `ObjectBoxSyncGradlePlugin` (id `io.objectbox.sync`) subclasses it to swap in the Sync-enabled native library. On apply, the plugin adds objectbox-java/native-library dependencies, wires up the annotation processor, and registers byte-code transform tasks. The library/dependency versions it injects come from BuildConfig constants generated in the root build script (`appliesObx*Version` extra properties). Depends on `objectbox-code-modifier` and `agp-wrapper-7-2`.
- **objectbox-processor** — annotation processor that reads `@Entity` classes. Depends on `objectbox-generator` and `objectbox-code-modifier`.
- **objectbox-generator** — source code generator used by the processor (generates `MyObjectBox`, underscore/`Example_` classes) plus the schema model classes and IdSync (model JSON file) logic.
- **objectbox-code-modifier** — shared code: IdSync generation, byte-code transformer base classes (Javassist-based), logging, and analytics/reporting.
- **agp-wrapper-7-2** — compatibility layer compiled against a specific Android Gradle Plugin API version (byte-code transforms only), avoiding reflection against changing AGP APIs.
- **build-logic** — included build with convention plugins: `objectbox-publish` (Maven publishing/signing, GitLab + Sonatype repos) and `objectbox-disable-analytics`.

## Compatibility constraints (critical when editing plugin code)

- The plugin supports Gradle 7.0+ (`GradleCompat`) and Android Plugin 8.1+ (`AndroidCompat`).
- Gradle runs plugins with its *embedded* Kotlin version, so all plugin projects set Kotlin `apiVersion` to 1.4 (what Gradle 7.0 bundles). Do not use newer Kotlin APIs in `objectbox-gradle-plugin`, `objectbox-code-modifier`, or `agp-wrapper-*` code — this also applies to their dependencies.
- Java targets: `objectbox-processor` and `objectbox-generator` compile to Java 8; the plugin projects target Java 11.
- Version-specific Gradle/AGP API access goes through the `util/GradleCompat`/`AndroidCompat` abstractions and `agp-wrapper-x-y` projects — follow that pattern rather than calling new APIs directly.

## Adding a new property type

There is a step-by-step checklist in `README.md` ("Adding a new property type") that spans generator, processor, and the objectbox-java repo — follow it, including updating the expected-output test files.
