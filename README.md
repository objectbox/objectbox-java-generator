# ObjectBox Gradle Plugin

This is a collection of Gradle projects:
- `objectbox-gradle-plugin` provides the actual Gradle plugins (`io.objectbox` and `io.objectbox.sync`),
  including byte-code transformers for Android and Java projects,
- `objectbox-processor` provides an annotation processor,
- `objectbox-generator` provides a source code generator used by the annotation processor,
- `objectbox-code-modifier` provides model file ("IdSync") generation used by the annotation processor.

All are published as Maven artifacts (see `gradle` folder). See the company Wiki for the release process.

## Requirements for projects applying the plugin

- Gradle `5.0`
- Android Plugin `3.3.0`
