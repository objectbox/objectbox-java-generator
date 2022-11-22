package io.objectbox.gradle.transform

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.provider.Property

class AndroidPlugin72 : AndroidPlugin34() {

    override fun registerTransform(project: Project, debug: Property<Boolean>, hasKotlinPlugin: Boolean) {
        // For all builds and tests (on device, on dev machine),
        // uses the new Transform API for Android Plugin 7.2 and newer.
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            variant.instrumentation.transformClassesWith(
                ObjectBoxAsmClassVisitor.Factory::class.java,
                InstrumentationScope.PROJECT
            ) {
                it.debug.set(debug)
            }
            // Transformer adds field, modifies constructors and methods so compute frames for methods.
            variant.instrumentation
                .setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS)
        }
    }

}