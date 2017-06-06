package io.objectbox.gradle

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestExtension
import com.android.build.gradle.TestPlugin
import io.objectbox.annotation.Entity
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import javassist.bytecode.FieldInfo
import javassist.bytecode.annotation.Annotation
import org.gradle.api.Project
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File


class ObjectBoxAndroidTransform(val project: Project) : Transform() {
    object Const {
        val entityAnnotationName = Entity::class.qualifiedName

        val toOne = "io/objectbox/relation/ToOne"
        val toOneDescriptor = "L$toOne;"

        val toMany = "io/objectbox/relation/ToMany"
        val toManyDescriptor = "L$toMany;"

        val boxStoreFieldName = "__boxStore"
        val boxStoreDescriptor = "Lio.objectbox.BoxStore;"
    }

    object Registration {
        fun to(project: Project) {
            val transform = ObjectBoxAndroidTransform(project)
            getExtension(project).registerTransform(transform)
        }

        fun getExtension(project: Project): BaseExtension {
            val clazz: Class<out BaseExtension> =
                    when {
                        project.plugins.hasPlugin(LibraryPlugin::class.java) -> LibraryExtension::class.java
                        project.plugins.hasPlugin(TestPlugin::class.java) -> TestExtension::class.java
                        project.plugins.hasPlugin(AppPlugin::class.java) -> AppExtension::class.java
                        else -> throw RuntimeException(
                                "No Android plugin found - please apply ObjectBox plugins after the Android plugin")
                    }
            return project.extensions.getByType(clazz)
        }
    }

    override fun getName(): String {
        return "ObjectBoxAndroidTransform"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return mutableSetOf(QualifiedContent.DefaultContentType.CLASSES)
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return mutableSetOf(QualifiedContent.Scope.PROJECT)
    }

    override fun isIncremental(): Boolean {
        return false
    }

    override fun transform(info: TransformInvocation) {
        super.transform(info)
        val allClassFiles = mutableSetOf<File>()
        for (input in info.inputs) {
            for (directoryInput in input.directoryInputs) {
                // TODO incremental: directoryInput.changedFiles

                allClassFiles.addAll(directoryInput.file.walk().filter { it.isFile })
            }
        }

        val probedEntities = allClassFiles.map { probeClassAsEntity(it) }.filterNotNull()
        val outDir: File by lazy {
            info.outputProvider.getContentLocation("objectbox", inputTypes, scopes, Format.DIRECTORY)
        }
    }

    internal fun probeClassAsEntity(file: File): ProbedEntity? {
        DataInputStream(BufferedInputStream(file.inputStream())).use {
            val classFile = ClassFile(it)
            if (!classFile.isAbstract) {
                var annotation = getEntityAnnotation(classFile)
                if (annotation != null) {
                    val fields = classFile.fields as List<FieldInfo>
                    return ProbedEntity(
                            file = file,
                            name = classFile.name,
                            hasBoxStoreField = fields.any { it.name == Const.boxStoreFieldName },
                            hasToOne = hasClassRef(classFile, Const.toOne, Const.toOneDescriptor),
                            hasToMany = hasClassRef(classFile, Const.toMany, Const.toManyDescriptor)
                    )
                }
            }
        }
        return null
    }

    private fun getEntityAnnotation(classFile: ClassFile): Annotation? {
        var annotationsAttribute = classFile.getAttribute(AnnotationsAttribute.visibleTag) as AnnotationsAttribute?
        var annotation = annotationsAttribute?.getAnnotation(Const.entityAnnotationName)
        if (annotation == null) {
            annotationsAttribute = classFile.getAttribute(AnnotationsAttribute.invisibleTag) as AnnotationsAttribute?
            annotation = annotationsAttribute?.getAnnotation(Const.entityAnnotationName)
        }
        return annotation
    }

    private fun hasClassRef(classFile: ClassFile, className: String, classDescriptorName: String): Boolean {
        // Fields may be of type List, so also check class names (was OK for Customer test entity at least)
        return classFile.constPool.classNames.any { it is String && it == className }
                || (classFile.fields as List<FieldInfo>).any { it.descriptor == classDescriptorName }
    }
}
