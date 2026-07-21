/*
 * ObjectBox Build Tools
 * Copyright (C) 2022-2026 ObjectBox Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.objectbox.gradle.transform

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.LibraryVariant
import com.android.build.api.variant.Variant
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

class AndroidPlugin72 : AndroidPluginCompat() {

    override fun registerTransform(project: Project, debug: Property<Boolean>, hasKotlinPlugin: Boolean) {
        // For all builds and tests (on device, on dev machine),
        // uses the new Transform API for Android Plugin 7.2 and newer.
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        // Note: pass a selector explicitly instead of relying on the default parameter so the byte code of this
        // class, compiled against an older version of the Android Plugin, does not call Kotlin synthetic methods
        // (for default parameters) that a newer version may no longer contain.
        androidComponents.onVariants(androidComponents.selector().all()) { variant ->
            captureFirstApplicationId(project, variant)
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

    /**
     * Stores the application ID of the first variant in an extra property of the project
     * so [getFirstApplicationId] can return it.
     *
     * The old variant API this was previously based on is deprecated as of Android Plugin 9.0 (and its extension
     * types no longer accessible by default), so get the application ID from the new variant API instead.
     */
    private fun captureFirstApplicationId(project: Project, variant: Variant) {
        val extraProperties = project.extensions.extraProperties
        if (extraProperties.has(FIRST_APPLICATION_ID_PROPERTY)) {
            return // Already captured for the first variant.
        }
        val applicationId: Provider<String> = when (variant) {
            is ApplicationVariant -> variant.applicationId
            // Like the old variant API, use the package name for libraries (the namespace, it used to be taken
            // from the manifest).
            is LibraryVariant -> variant.namespace
            // Note: variants of test-only modules (com.android.test) were never tracked, keep it that way.
            else -> return
        }
        extraProperties.set(FIRST_APPLICATION_ID_PROPERTY, applicationId)
    }

    override fun getFirstApplicationId(project: Project): String? {
        val extraProperties = project.extensions.extraProperties
        if (!extraProperties.has(FIRST_APPLICATION_ID_PROPERTY)) {
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return (extraProperties.get(FIRST_APPLICATION_ID_PROPERTY) as? Provider<String>)?.orNull
    }

    companion object {
        /**
         * Extra property of a project this plugin is applied to that stores a `Provider<String>` of the application
         * ID (or library package name) of the first variant.
         */
        private const val FIRST_APPLICATION_ID_PROPERTY = "io.objectbox.internal.firstApplicationId"
    }

}
