package io.objectbox.processor;

import com.google.auto.service.AutoService;
import io.objectbox.annotation.Entity;
import io.objectbox.generator.model.Schema;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.LinkedHashSet;
import java.util.Set;

@AutoService(Processor.class)
public final class ObjectBoxProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        // TODO ut: remove unused utils
        elementUtils = env.getElementUtils();
        typeUtils = env.getTypeUtils();
        filer = env.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(Entity.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        findAndParse(env);
        return false;
    }

    private void findAndParse(RoundEnvironment env) {
        Schema schema = null;

        for (Element element : env.getElementsAnnotatedWith(Entity.class)) {
            note(element, "Processing @Entity annotation.");

            if (schema == null) {
                PackageElement elementPackage = elementUtils.getPackageOf(element);
                Name packageName = elementPackage.getQualifiedName();
                schema = new Schema(1, packageName.toString());
            }

            io.objectbox.generator.model.Entity entity = schema.addEntity(element.getSimpleName().toString());
        }

        // can't use Filer + only Gradle knows the build directory... :/
//        File outDir = new File(project.buildDir, "generated/source/objectbox");
//        try {
//            new BoxGenerator(false).generateAll(schema, outDir.getPath());
//        } catch (Exception e) {
//            printMessage(Diagnostic.Kind.ERROR, "Code generation failed: %s", e.getMessage());
//        }
    }

    private void error(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.ERROR, element, message, args);
    }

    private void note(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.NOTE, element, message, args);
    }

    private void printMessage(Diagnostic.Kind kind, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }

        processingEnv.getMessager().printMessage(kind, message);
    }

    private void printMessage(Diagnostic.Kind kind, Element element, String message, Object[] args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }

        processingEnv.getMessager().printMessage(kind, message, element);
    }
}
