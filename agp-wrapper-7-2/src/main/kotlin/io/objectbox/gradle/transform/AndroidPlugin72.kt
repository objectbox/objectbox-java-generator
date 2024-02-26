/*
 * Copyright (C) 2022-2024 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

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