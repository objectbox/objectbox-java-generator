# Repository Guidelines

Read `README.md` first: it describes what the plugin does, the minimum supported Gradle, Android
Plugin and JDK versions, and has the checklist for adding a new property type
(follow it, including updating the expected-output test files).
This file only adds what is not covered there.

## Snapshot dependencies (build prerequisite)

Subprojects depend on snapshot versions of `objectbox-java` (`<versionNumber>-dev-SNAPSHOT`, the
Sync native library uses `-sync-SNAPSHOT`).
These resolve from the internal ObjectBox GitLab repository, which is only added when the Gradle
properties `gitlabUrl` and `gitlabToken` (or `gitlabPrivateToken`) are set — e.g. in
`~/.gradle/gradle.properties`.
`mavenLocal()` is a fallback (build `objectbox-java` locally with `./gradlew publishToMavenLocal`).
Without one of these, the build fails to resolve dependencies.
`OBX_RELEASE=true` in the environment switches to release versions (no snapshot suffix).

## Commands

```bash
./gradlew build  # build everything
./gradlew check  # all tests, including TestKit integration tests (what CI runs)

# Unit tests of one subproject
./gradlew :objectbox-processor:test
./gradlew :objectbox-gradle-plugin:test

# A single test class / method
./gradlew :objectbox-processor:test --tests "io.objectbox.processor.SimpleEntityTest"
./gradlew :objectbox-gradle-plugin:test --tests "io.objectbox.gradle.PluginApplyTest.someMethod"

# Android Gradle Plugin integration tests (separate source sets in objectbox-gradle-plugin)
# Too-old AGP fails with a helpful error (TestKit)
./gradlew :objectbox-gradle-plugin:agp73Test
# Lowest supported AGP, Android project build (TestKit)
./gradlew :objectbox-gradle-plugin:agp81Test
# AGP 8.10 (lowest supporting KSP), in-process plugin apply tests (incl. kotlin-android with
# kapt or KSP) + Kotlin Android project build with KSP (TestKit)
./gradlew :objectbox-gradle-plugin:agp810Test
# Apply tests (built-in Kotlin) + Android project builds (lowest and latest Gradle)
./gradlew :objectbox-gradle-plugin:agp9Test
```

TestKit tests (`GradleTestRunner`-based tests and the `agpXYTest` tasks) build throwaway Gradle
projects (fixtures in `objectbox-gradle-plugin/test-gradle-projects`): they first publish the other
subprojects to a local test repository and receive the `gitlab*` properties as system properties,
so they also need the GitLab credentials to be set.
Some of them build an actual Android project, which requires `ANDROID_HOME` to point to an Android
SDK — if it is not set, `check` fails late with "SDK location not found" in `agp81Test`/`agp9Test`
tests.
A 401 Unauthorized when resolving `io.objectbox` snapshots means the GitLab token is
expired/invalid (verify with `curl -H "Private-Token: $token" <gitlabUrl>/api/v4/user`).

Processor tests compare against expected files in `objectbox-processor/src/test/resources`:
generated Java sources in `expected-source/`, model JSON files in `objectbox-models/`.

## Workflow

- Work happens on the `dev` branch; `main` is the main branch.
- Commit messages use `<Area>: <summary>` or `<Area> (<aspect>): <summary>`, e.g.
  `Plugin (tests): de-duplicate Java/Kotlin dependency asserts`.
- In the body, briefly explain the motivation for the change: why it is needed, not what the diff 
  already shows. Trivial commits don't need a body.
- Don't add attribution lines (like Co-Authored-By) to commit messages.
- Changes that affect users need an entry in the objectbox-java changelog — point this out.
- `versionNumber` in the root `build.gradle.kts` is the single source of truth for the version;
  change it only via `scripts/set-version.sh`.
  Releases follow the release checklist in the objectbox repository.
- The `Jenkinsfile` at the repo root is an obsolete leftover from the previous CI setup (CI is
  GitLab, see `.gitlab-ci.yml`) — ignore it.

## Conventions for changes

These are general working principles for this project; the repo-specific pointers are examples, apply the principle to whatever is being changed.

- **No duplicate logic.** When adding a variant of existing code (a second API, backend, format, ...), extracting the shared core — via base classes, delegates, or a small abstraction over the differing APIs — is part of the task, not an optional follow-up. A good order: implement the variant, prove behavior parity with tests, then generalize until each piece of logic exists once. Example: the two processors share `ObjectBoxProcessorCore`/`Properties`/`Relations` through the `ProcessingApi.kt` abstraction — extend that rather than adding parallel javax/KSP code paths.
- **The same applies to tests.** Adapt or parameterize existing tests to also cover a new variant, so all variants are held to identical expectations — do not write a separate, diverging suite. Example: tests extending `BaseProcessorTest` (environments via `env(...)`) run against both processors automatically; end-to-end (TestKit) coverage alone is not acceptable for processor behavior.
- **Pin behavior before changing it.** For code that produces output, characterization tests comparing against expected output come first, then the port/refactor is verified against them. Example: generated sources and the model JSON file are byte-compatible across all processor setups, pinned by `objectbox-processor/src/test/resources/` (`expected-source/`, `objectbox-models/`) and `objectbox-gradle-plugin/src/test/resources/kotlin-project/`. Only change expected files when the output is meant to change.
- **Prefer coexistence over removal.** When introducing a replacement, keep existing consumer setups working (the old way may become opt-in); dropping support requires explicit agreement. Example: kapt remains the default for Kotlin projects, KSP is opt-in by applying its plugin.
- **Prefer slim in-repo helpers over new dependencies.** Build on official/first-party APIs instead of adding third-party libraries, also for test-only needs; adding a dependency requires explicit agreement. Example: `KspCompiler` runs the KSP processor in tests via the standalone KSP implementation instead of a compile-testing fork.
- **Verify claims empirically.** Version-compatibility assumptions (Kotlin/KSP/AGP/Gradle) and behavior claims are checked with tests or experiments; documentation is often wrong about minimum versions. When results contradict expectations, present the evidence (error output, version checks), not just the conclusion.
- **Commits tell a clean story.** Split work into logical, self-contained steps that each build and pass tests; for message style see Workflow above. Never add AI/tool attribution or co-author trailers. Avoid within-branch churn (one commit adding what a later one removes or renames) — restructure the commits instead.
- **Done means `./gradlew check` is green.**

## Subprojects and how they relate

Subprojects (see `settings.gradle.kts`), dependency chain:

- `objectbox-gradle-plugin` → `objectbox-code-modifier`, `agp-wrapper-7-2`
- `agp-wrapper-7-2` → `objectbox-code-modifier`
- `objectbox-processor` → `objectbox-generator`, `objectbox-code-modifier`

Contents:

- **objectbox-gradle-plugin** — the actual Gradle plugins.
  `ObjectBoxGradlePlugin` (id `io.objectbox`) does the work; `ObjectBoxSyncGradlePlugin`
  (id `io.objectbox.sync`) subclasses it to swap in the Sync-enabled native library.
  On apply, the plugin adds objectbox-java/native-library dependencies, wires up the processor
  (Java projects: `annotationProcessor`; Kotlin projects: applies the kapt plugin and uses the
  `kapt` configuration, unless the project has applied the KSP plugin, then the `ksp`
  configuration is used), and registers byte-code transform tasks.
  The library/dependency versions it injects come from BuildConfig constants generated in the root
  build script (`appliesObx*Version` extra properties).
- **objectbox-processor** — reads `@Entity` classes: a javax annotation processor and a KSP
  `SymbolProcessor` (`io.objectbox.processor.ksp` package) that produce identical output and share
  their implementation: parsing works on an abstraction of the two processor APIs
  (`ProcessingApi.kt`, implemented by `JavaxProcessing`/`KspProcessing`), so
  `ObjectBoxProcessorCore`, `Properties` and `Relations` exist only once.
  The KSP processor is compiled against an older KSP API the build's Kotlin version can read, but
  runs under the consumer's (newer) KSP version.
  Unit tests run against both processors (parameterized `BaseProcessorTest`; KSP runs without
  Gradle via the standalone KSP found in `KspCompiler`).
- **objectbox-generator** — source code generator used by the processor (generates `MyObjectBox`,
  underscore/`Example_` classes from FreeMarker templates, `*.ftl` in
  `src/main/resources/io/objectbox/generator`) plus the schema model classes (`generator.model`)
  and IdSync, the model JSON file sync (`generator.idsync`).
- **objectbox-code-modifier** — shared code: byte-code transformer base classes (Javassist-based,
  `gradle.transform`), logging, and analytics/reporting.
- **agp-wrapper-7-2** — compatibility layer compiled against a specific Android Plugin API version
  (byte-code transforms only), avoiding reflection against changing AGP APIs.
- **build-logic** — included build with convention plugins: `objectbox-publish` (Maven
  publishing/signing, GitLab + Sonatype repos) and `objectbox-disable-analytics`.

## Compatibility constraints (critical when editing plugin code)

- Version-specific Gradle/AGP API access goes through the `util/GradleCompat`/`AndroidCompat`
  abstractions and `agp-wrapper-x-y` projects — follow that pattern rather than calling new APIs
  directly, so the minimum versions listed in `README.md` keep working.
- Gradle runs plugins with its *embedded* Kotlin version, so all plugin projects set Kotlin
  `apiVersion` to 1.9 (what Gradle 8.3 bundles).
  Do not use newer Kotlin APIs in `objectbox-gradle-plugin`, `objectbox-code-modifier`, or
  `agp-wrapper-*` code — this also applies to their dependencies.
- The wrapper intentionally stays on Gradle 9.5.x: newer Gradle versions can no longer run Android
  Plugin 8, which the in-process (ProjectBuilder-based) `agp810Test` tests still cover.
  TestKit tests run other Gradle versions in a subprocess (e.g. `agp9Test`).
- Java targets: everything compiles to Java 8, except `objectbox-gradle-plugin` (Java 11).
- KSP version coupling: the KSP API artifacts this build compiles against must be readable by
  `kotlinVersion` of this build (a Kotlin compiler can not read much newer metadata):
  `symbol-processing-api` (processor) and `symbol-processing-aa` (test infrastructure).
  The KSP Gradle plugin version (`kspVersion`, 2.3.x, versioned independently of Kotlin) is both
  recommended to consumers and used for the internal Moshi code gen.
  Current KSP versions require AGP 8.10+ for Android projects (with older versions, Kotlin
  Android projects use the kapt default).
