package io.objectbox.processor;

import com.google.auto.service.AutoService;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.NameInDb;
import io.objectbox.annotation.Transient;
import io.objectbox.generator.model.Property;
import io.objectbox.generator.model.Property.PropertyBuilder;
import io.objectbox.generator.model.PropertyType;
import io.objectbox.generator.model.Schema;

import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

            parseEntity(schema, element);
        }

        // can't use Filer + only Gradle knows the build directory... :/
//        File outDir = new File(project.buildDir, "generated/source/objectbox");
//        try {
//            new BoxGenerator(false).generateAll(schema, outDir.getPath());
//        } catch (Exception e) {
//            printMessage(Diagnostic.Kind.ERROR, "Code generation failed: %s", e.getMessage());
//        }
    }

    private void parseEntity(Schema schema, Element element) {
        io.objectbox.generator.model.Entity entity = schema.addEntity(element.getSimpleName().toString());

        List<VariableElement> fields = ElementFilter.fieldsIn(element.getEnclosedElements());
        for (VariableElement field : fields) {
            parseProperty(entity, field);
        }
    }

    private void parseProperty(io.objectbox.generator.model.Entity entity, VariableElement field) {

        // Compare with EntityClassASTVisitor.endVisit()
        // and GreendaoModelTranslator.convertProperty()

        TypeElement enclosingElement = (TypeElement) field.getEnclosingElement();
        Name fieldName = field.getSimpleName();

        // ignore static, transient or @Transient fields
        Set<Modifier> modifiers = field.getModifiers();
        if (modifiers.contains(Modifier.STATIC)
                || modifiers.contains(Modifier.TRANSIENT)
                || field.getAnnotation(Transient.class) != null) {
            note(field, "Ignoring transient field. (%s.%s)", enclosingElement.getQualifiedName(), fieldName);
            return;
        }

        // verify field is accessible
        if (modifiers.contains(Modifier.PRIVATE)) {
            error(field, "Field must not be private. (%s.%s)", enclosingElement.getQualifiedName(), fieldName);
            return;
        }

        Property.PropertyBuilder propertyBuilder;

        Convert convertAnnotation = field.getAnnotation(Convert.class);
        if (convertAnnotation != null) {
            // verify @Convert custom type
            propertyBuilder = parseCustomProperty(entity, field, enclosingElement);
        } else {
            // verify that supported type is used
            propertyBuilder = parseSupportedProperty(entity, field, enclosingElement);
        }
        if (propertyBuilder == null) {
            return;
        }

        // checks above ensure field is NOT private
        propertyBuilder.fieldAccessible();

        // @Id
        Id idAnnotation = field.getAnnotation(Id.class);
        if (idAnnotation != null) {
            propertyBuilder.primaryKey();
            if (idAnnotation.assignable()) {
                propertyBuilder.idAssignable();
            }
        }

        // @NameInDb
        NameInDb nameInDbAnnotation = field.getAnnotation(NameInDb.class);
        if (nameInDbAnnotation != null) {
            String name = nameInDbAnnotation.value();
            if (name.length() > 0) {
                propertyBuilder.dbName(name);
            }
        } else if (idAnnotation != null && propertyBuilder.getProperty().getPropertyType() == PropertyType.Long) {
            // use special name for @Id column if type is Long
            propertyBuilder.dbName("_id");
        }

        // @Index
        Index indexAnnotation = field.getAnnotation(Index.class);
        if (indexAnnotation != null) {
            propertyBuilder.indexAsc(null, false);
        }

        // TODO ut: add remaining property build steps
    }

    @Nullable
    private Property.PropertyBuilder parseCustomProperty(io.objectbox.generator.model.Entity entity,
            VariableElement field, TypeElement enclosingElement) {
        // extract @Convert annotation member values
        // as they are types, need to access them via annotation mirrors
        AnnotationMirror annotationMirror = getAnnotationMirror(field, Convert.class);
        if (annotationMirror == null) {
            return null; // did not find @Convert mirror
        }

        TypeMirror converter = getAnnotationValueType(annotationMirror, "converter");
        if (converter == null) {
            error(field, "@Convert requires a value for converter. (%s.%s)",
                    enclosingElement.getQualifiedName(), field.getSimpleName());
            return null;
        }

        TypeMirror dbType = getAnnotationValueType(annotationMirror, "dbType");
        if (dbType == null) {
            error(field, "@Convert requires a value for dbType. (%s.%s)",
                    enclosingElement.getQualifiedName(), field.getSimpleName());
            return null;
        }
        PropertyType propertyType = getPropertyType(dbType);
        if (propertyType == null) {
            error(field, "@Convert dbType type is not supported. (%s.%s)",
                    enclosingElement.getQualifiedName(), field.getSimpleName());
            return null;
        }

        Property.PropertyBuilder propertyBuilder = entity.addProperty(propertyType, field.getSimpleName().toString());
        propertyBuilder.customType(field.asType().toString(), converter.toString());
        // note: custom types are already assumed non-primitive by Property#isNonPrimitiveType()
        return propertyBuilder;
    }

    @Nullable
    private PropertyBuilder parseSupportedProperty(io.objectbox.generator.model.Entity entity, VariableElement field,
            TypeElement enclosingElement) {
        TypeMirror typeMirror = field.asType();
        PropertyType propertyType = getPropertyType(typeMirror);
        if (propertyType == null) {
            error(field, "Field type is not supported. (%s.%s)", enclosingElement.getQualifiedName(),
                    field.getSimpleName());
            return null;
        }

        PropertyBuilder propertyBuilder = entity.addProperty(propertyType, field.getSimpleName().toString());

        boolean isPrimitive = typeMirror.getKind().isPrimitive();
        if (isPrimitive) {
            // treat primitive types as non-null
            propertyBuilder.notNull();
        } else if (propertyType.isScalar()) {
            // treat wrapper types (Long, Integer, ...) of scalar types as non-primitive
            propertyBuilder.nonPrimitiveType();
        }

        return propertyBuilder;
    }

    @Nullable
    private AnnotationMirror getAnnotationMirror(Element element, Class annotationClass) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            DeclaredType annotationType = annotationMirror.getAnnotationType();
            TypeMirror convertType = elementUtils.getTypeElement(annotationClass.getCanonicalName()).asType();
            if (typeUtils.isSameType(annotationType, convertType)) {
                return annotationMirror;
            }
        }
        return null;
    }

    @Nullable
    private TypeMirror getAnnotationValueType(AnnotationMirror annotationMirror, String memberName) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues =
                annotationMirror.getElementValues();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
            String elementName = entry.getKey().getSimpleName().toString();
            if (elementName.equals(memberName)) {
                // this is a shortcut instead of using entry.getValue().accept(visitor, null)
                return (TypeMirror) entry.getValue().getValue();
            }
        }
        return null;
    }

    private PropertyType getPropertyType(TypeMirror typeMirror) {
        if (isTypeEqual(typeMirror, Short.class.getName()) || typeMirror.getKind() == TypeKind.SHORT) {
            return PropertyType.Short;
        }
        if (isTypeEqual(typeMirror, Integer.class.getName()) || typeMirror.getKind() == TypeKind.INT) {
            return PropertyType.Int;
        }
        if (isTypeEqual(typeMirror, Long.class.getName()) || typeMirror.getKind() == TypeKind.LONG) {
            return PropertyType.Long;
        }

        if (isTypeEqual(typeMirror, Float.class.getName()) || typeMirror.getKind() == TypeKind.FLOAT) {
            return PropertyType.Float;
        }
        if (isTypeEqual(typeMirror, Double.class.getName()) || typeMirror.getKind() == TypeKind.DOUBLE) {
            return PropertyType.Double;
        }

        if (isTypeEqual(typeMirror, Boolean.class.getName()) || typeMirror.getKind() == TypeKind.BOOLEAN) {
            return PropertyType.Boolean;
        }
        if (isTypeEqual(typeMirror, Byte.class.getName()) || typeMirror.getKind() == TypeKind.BYTE) {
            return PropertyType.Byte;
        }
        if (isTypeEqual(typeMirror, Date.class.getName())) {
            return PropertyType.Date;
        }
        if (isTypeEqual(typeMirror, String.class.getName())) {
            return PropertyType.String;
        }

        if (typeMirror.getKind() == TypeKind.ARRAY) {
            ArrayType arrayType = (ArrayType) typeMirror;
            if (arrayType.getComponentType().getKind() == TypeKind.BYTE) {
                return PropertyType.ByteArray;
            }
        }

        return null;
    }

    private static boolean isTypeEqual(TypeMirror typeMirror, String otherType) {
        return otherType.equals(typeMirror.toString());
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
