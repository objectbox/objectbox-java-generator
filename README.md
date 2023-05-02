# ObjectBox Gradle Plugin

This is a collection of Gradle projects:
- `objectbox-gradle-plugin` provides the actual Gradle plugins (`io.objectbox` and `io.objectbox.sync`),
- `objectbox-processor` provides an annotation processor,
- `objectbox-generator` provides a source code generator used by the annotation processor,
- `objectbox-code-modifier` provides model file ("IdSync") generation used by the annotation processor,
  and byte-code transformers for Android and Java projects.
  - Multiple `agp-wrapper-x-y` projects provide a compatibility layer for specific versions 
  of the Android Plugin API, currently for byte-code transforms only.

All are published as Maven artifacts (see `gradle` folder). See the company Wiki for the release process.

## Requirements for projects applying the plugin

- Gradle `6.1` (see [GradleCompat](objectbox-gradle-plugin/src/main/kotlin/io/objectbox/gradle/util/GradleCompat.kt)).
- Android Plugin `3.4` (see [AndroidCompat](objectbox-gradle-plugin/src/main/kotlin/io/objectbox/gradle/util/AndroidCompat.kt)).

Note: In our examples project, there is a `lowest-supported-tools` branch that uses these.

## Development

This project is known to build with JDK 11.
