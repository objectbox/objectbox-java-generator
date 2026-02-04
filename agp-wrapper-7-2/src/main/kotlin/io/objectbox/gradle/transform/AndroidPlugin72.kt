/*
 * ObjectBox Build Tools
 * Copyright (C) 2022-2024 ObjectBox Ltd.
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
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.provider.Property

class AndroidPlugin72 : AndroidPluginCompat() {

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

    // APIs exist up to Android Plugin 9.0, but as of 9.0 the extension types are deprecated.
    override fun getFirstApplicationId(project: Project): String? {
        return when (val androidExtension = project.extensions.findByType(BaseExtension::class.java)) {
            is AppExtension -> androidExtension.applicationVariants.firstOrNull()?.applicationId
            // FeatureExtension is deprecated as of Android Plugin 3.4.0 (April 2019) and was never tracked before,
            // so continue to not track.
            // is FeatureExtension -> androidExtension.featureVariants.firstOrNull()?.applicationId
            is LibraryExtension -> androidExtension.libraryVariants.firstOrNull()?.applicationId
            // Note: TestExtension is only used to create a separate instrumentation test module,
            // it does not have an application ID.
            // https://developer.android.com/studio/test/advanced-test-setup#use-separate-test-modules-for-instrumented-tests
            // is TestExtension ->
            else -> null
        }
    }

}