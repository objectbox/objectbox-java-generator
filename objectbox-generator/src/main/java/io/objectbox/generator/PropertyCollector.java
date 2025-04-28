/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2025 ObjectBox Ltd.
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

package io.objectbox.generator;

import org.greenrobot.essentials.collections.Multimap;

import java.util.List;

import io.objectbox.generator.model.Entity;
import io.objectbox.generator.model.Property;
import io.objectbox.generator.model.PropertyType;

/**
 * Builds code string to efficiently collect the properties of an entity.
 * The generated code string contains collect method calls matching those in {@link io.objectbox.Cursor}.
 */
class PropertyCollector {
    private final static String INDENT = "        ";
    private final static String INDENT_EX = "                ";
    private final static String BR_INDENT_EX = "\n" + INDENT_EX;
    private final static String SEP_BR = ',' + BR_INDENT_EX;

    /**
     * Contains all properties of the entity by type.
     */
    private final Multimap<PropertyType, Property> propertiesByType;
    private final Property idProperty;

    public PropertyCollector(Entity entity) {
        propertiesByType = Multimap.create();
        for (Property property : entity.getProperties()) {
            if (!property.isPrimaryKey()) {
                propertiesByType.putElement(property.getPropertyType(), property);
            }
        }
        idProperty = entity.getPkProperty();
        if (idProperty == null) {
            throw new IllegalStateException("No ID property found for \"" + entity + "\" " +
                    "(use @Id on a property of type long)");
        }
    }

    /**
     * Has at least one property of given type.
     */
    private boolean hasPropertyOf(PropertyType type) {
        return propertiesByType.countElements(type) > 0;
    }

    String createPropertyCollector() {
        StringBuilder preCall = new StringBuilder();
        StringBuilder properties = new StringBuilder();
        StringBuilder all = new StringBuilder();

        boolean first = true;
        int previousPropertyCount = propertiesByType.countElements();
        boolean last = previousPropertyCount == 0;
        while (first || !last) {
            int countByteArrays = propertiesByType.countElements(PropertyType.ByteArray) +
                    propertiesByType.countElements(PropertyType.Flex);
            int countScalarsNonFP = countScalarsNonFP();
            int countFloats = propertiesByType.countElements(PropertyType.Float);
            int countDoubles = propertiesByType.countElements(PropertyType.Double);
            int maxCountFP = Math.max(countFloats, countDoubles);
            int countStrings = propertiesByType.countElements(PropertyType.String);

            String collectSignature;
            if (hasPropertyOf(PropertyType.BooleanArray)) {
                collectSignature = appendPropertyScalarArray(properties, preCall, PropertyType.BooleanArray);
            } else if (hasPropertyOf(PropertyType.ShortArray)) {
                collectSignature = appendPropertyScalarArray(properties, preCall, PropertyType.ShortArray);
            } else if (hasPropertyOf(PropertyType.CharArray)) {
                collectSignature = appendPropertyScalarArray(properties, preCall, PropertyType.CharArray);
            } else if (hasPropertyOf(PropertyType.IntArray)) {
                collectSignature = appendPropertyScalarArray(properties, preCall, PropertyType.IntArray);
            } else if (hasPropertyOf(PropertyType.LongArray)) {
                collectSignature = appendPropertyScalarArray(properties, preCall, PropertyType.LongArray);
            } else if (hasPropertyOf(PropertyType.FloatArray)) {
                collectSignature = appendPropertyScalarArray(properties, preCall, PropertyType.FloatArray);
            } else if (hasPropertyOf(PropertyType.DoubleArray)) {
                collectSignature = appendPropertyScalarArray(properties, preCall, PropertyType.DoubleArray);
            } else if (hasPropertyOf(PropertyType.StringArray)) {
                collectSignature = appendPropertyStringArrayOrList(properties, preCall);
            } else if (countStrings > 3 || countByteArrays > 1) {
                // If there are more non-primitive properties than we can process with one call, we must first collect
                // them before mixing with primitives
                collectSignature = countByteArrays > 0 ? appendProperties430000(properties, preCall) :
                        appendProperties400000(properties, preCall);
            } else {
                if (countStrings == 0 && countByteArrays == 0 && maxCountFP > 1 &&
                        // Calls needed for FPs in 313311 > calls needed for non-FPs in 002033
                        maxCountFP > (countScalarsNonFP + 1) / 2) {
                    collectSignature = appendProperties002033(properties, preCall);
                } else {
                    if (propertiesByType.countElements() == countScalarsNonFP && (countScalarsNonFP <= 4 ||
                            (countScalarsNonFP > 6 && countScalarsNonFP < 9))) {
                        collectSignature = appendProperties004000(properties, preCall);
                    } else {
                        collectSignature = appendProperties313311(properties, preCall);
                    }
                }
            }
            int propertyCount = propertiesByType.countElements();
            if (!first && propertyCount == previousPropertyCount) {
                throw new RuntimeException("Could not collect properties: " + propertiesByType.valuesElements());
            }
            last = propertyCount == 0;

            appendCollectCall(collectSignature, all, preCall, properties, first, last);

            first = false;
            previousPropertyCount = propertyCount;

            properties.setLength(0);
            preCall.setLength(0);
        }
        return all.toString();
    }

    private int countScalarsNonFP() {
        return propertiesByType.countElements(PropertyType.Date) + propertiesByType.countElements(PropertyType.Long) +
                propertiesByType.countElements(PropertyType.Int) + propertiesByType.countElements(PropertyType.Short) +
                propertiesByType.countElements(PropertyType.Char) + propertiesByType.countElements(PropertyType.Byte) +
                propertiesByType.countElements(PropertyType.Boolean) + propertiesByType.countElements(PropertyType.DateNano);
    }

    private String appendProperties313311(StringBuilder properties, StringBuilder preCall) {
        appendProperty(preCall, properties, PropertyType.String, false).append(", ");
        appendProperty(preCall, properties, PropertyType.String, false).append(SEP_BR);
        appendProperty(preCall, properties, PropertyType.String, false).append(", ");
        appendProperty(preCall, properties, PropertyType.ByteArray, false).append(SEP_BR);
        appendProperty(preCall, properties, PropertyType.Long, true).append(", ");
        appendProperty(preCall, properties, PropertyType.Long, true).append(SEP_BR);
        appendProperty(preCall, properties, PropertyType.Long, true).append(", ");
        appendProperty(preCall, properties, PropertyType.Int, true).append(SEP_BR);
        appendProperty(preCall, properties, PropertyType.Int, true).append(", ");
        appendProperty(preCall, properties, PropertyType.Int, true).append(SEP_BR);
        appendProperty(preCall, properties, PropertyType.Float, true).append(", ");
        appendProperty(preCall, properties, PropertyType.Double, true).append(");\n\n");
        return "313311";
    }

    private String appendProperties430000(StringBuilder properties, StringBuilder preCall) {
        appendProperty(preCall, properties, PropertyType.String, false).append(", ");
        appendProperty(preCall, properties, PropertyType.String, false).append(SEP_BR);
        appendProperty(preCall, properties, PropertyType.String, false).append(", ");
        appendProperty(preCall, properties, PropertyType.String, false).append(SEP_BR);
        appendProperty(preCall, properties, PropertyType.ByteArray, false).append(", ");
        appendProperty(preCall, properties, PropertyType.ByteArray, false).append(SEP_BR);
        appendProperty(preCall, properties, PropertyType.ByteArray, false).append(");\n\n");
        return "430000";
    }

    private String appendProperties400000(StringBuilder properties, StringBuilder preCall) {
        appendProperty(preCall, properties, PropertyType.String, false).append(", ");
        appendProperty(preCall, properties, PropertyType.String, false).append(SEP_BR);
        appendProperty(preCall, properties, PropertyType.String, false).append(", ");
        appendProperty(preCall, properties, PropertyType.String, false).append(");\n\n");
        return "400000";
    }

    private String appendProperties002033(StringBuilder properties, StringBuilder preCall) {
        appendProperty(preCall, properties, PropertyType.Long, true).append(", ");
        appendProperty(preCall, properties, PropertyType.Long, true).append(SEP_BR);
        appendProperty(preCall, properties, PropertyType.Float, true).append(", ");
        appendProperty(preCall, properties, PropertyType.Float, true).append(SEP_BR);
        appendProperty(preCall, properties, PropertyType.Float, true).append(", ");
        appendProperty(preCall, properties, PropertyType.Double, true).append(SEP_BR);
        appendProperty(preCall, properties, PropertyType.Double, true).append(", ");
        appendProperty(preCall, properties, PropertyType.Double, true).append(");\n\n");
        return "002033";
    }

    private String appendProperties004000(StringBuilder properties, StringBuilder preCall) {
        appendProperty(preCall, properties, PropertyType.Long, true).append(", ");
        appendProperty(preCall, properties, PropertyType.Long, true).append(SEP_BR);
        appendProperty(preCall, properties, PropertyType.Long, true).append(", ");
        appendProperty(preCall, properties, PropertyType.Long, true).append(");\n\n");
        return "004000";
    }

    private String appendPropertyStringArrayOrList(StringBuilder sb, StringBuilder preCall) {
        List<Property> properties = propertiesByType.get(PropertyType.StringArray);
        Property nextProperty = properties.get(0);
        appendProperty(preCall, sb, PropertyType.StringArray, false).append(");\n\n");
        if (nextProperty.isList()) {
            return "StringList";
        } else {
            return "StringArray";
        }
    }

    /**
     * Appends collect call for a single scalar array property.
     */
    private String appendPropertyScalarArray(StringBuilder properties, StringBuilder preCall,
            PropertyType propertyType) {
        appendProperty(preCall, properties, propertyType, false).append(");\n\n");
        return propertyType.name();
    }

    /**
     * Based on the {@code type} appends code strings to {@code preCall} (null handling for nullable property)
     * and {@code sb} (adds ID and value collect call parameters).
     * <p>
     * If no property with the given type is found, tries to find a property with a compatible type (e.g. instead of
     * Long a RelationId). If none is found, appends a zero/null value.
     */
    private StringBuilder appendProperty(StringBuilder preCall, StringBuilder sb, PropertyType type, boolean isScalar) {
        // TODO improve null values -> don't pass them
        List<Property> properties = propertiesByType.get(type);
        if (properties == null || properties.isEmpty()) {
            // No property with exactly this type found, look for a compatible type to pass to the collect call.
            if (type == PropertyType.Long) {
                return appendProperty(preCall, sb, PropertyType.RelationId, isScalar);
            } else if (type == PropertyType.RelationId) {
                return appendProperty(preCall, sb, PropertyType.DateNano, isScalar);
            } else if (type == PropertyType.DateNano) {
                return appendProperty(preCall, sb, PropertyType.Date, isScalar);
            } else if (type == PropertyType.Date) {
                return appendProperty(preCall, sb, PropertyType.Int, isScalar);
            } else if (type == PropertyType.Int) {
                return appendProperty(preCall, sb, PropertyType.Short, isScalar);
            } else if (type == PropertyType.Short) {
                return appendProperty(preCall, sb, PropertyType.Char, isScalar);
            } else if (type == PropertyType.Char) {
                return appendProperty(preCall, sb, PropertyType.Byte, isScalar);
            } else if (type == PropertyType.Byte) {
                return appendProperty(preCall, sb, PropertyType.Boolean, isScalar);
            } else if (type == PropertyType.ByteArray) {
                return appendProperty(preCall, sb, PropertyType.Flex, isScalar);
            }

            // All compatible types checked, append a zero/null value
            // (and let property get added with the next collect call).
            if (isScalar) {
                sb.append("0, 0");
            } else {
                sb.append("0, null");
            }
        } else {
            Property property = properties.remove(0);
            String name = property.getPropertyName();
            String propertyId = "__ID_" + name;
            String propertyIdLocal = "__id" + property.getOrdinal();
            if (!property.isTypeNotNull()) {
                // Nullable type: if null pass zero ID and zero/null value instead.
                preCall.append(INDENT).append(property.getJavaTypeInEntity()).append(' ').append(name)
                        .append(" = ").append(getValue(property)).append(";\n");
                preCall.append(INDENT).append("int ").append(propertyIdLocal).append(" = ").append(name)
                        .append(" != null ? ").append(propertyId).append(" : 0;\n");
                sb.append(propertyIdLocal).append(", ");
                if (isScalar || property.getCustomType() != null) {
                    sb.append(propertyIdLocal).append(" != 0 ? ").append(property.getDatabaseValueExpression(name))
                            .append(isScalar ? " : 0" : " : null");
                } else {
                    sb.append(property.getDatabaseValueExpression(name));
                }
            } else {
                // Not null type
                sb.append(propertyId).append(", ").append("entity.");
                if (property.isVirtual()) {
                    // TODO this is hard-coded for to-ones, not really a generic "virtual property"
                    if (property.getVirtualTargetValueExpression() != null) {
                        sb.append(property.getVirtualTargetValueExpression());
                    } else {
                        sb.append(property.getVirtualTargetName());
                    }
                    sb.append(".getTargetId()");
                } else {
                    sb.append(property.getDatabaseValueExpression());
                }
            }
        }
        return sb;
    }

    private void appendCollectCall(String collectSignature, StringBuilder all, StringBuilder preCall,
            StringBuilder call, boolean first, boolean last) {
        // ID property before preCall for non-primitives
        // TODO check if we can use fields directly
        if (last && !idProperty.isTypeNotNull()) {
            all.append(INDENT).append(idProperty.getJavaTypeInEntity()).append(' ')
                    .append(idProperty.getPropertyName()).append(" = ").append(getValue(idProperty)).append(";\n");
        }
        if (preCall.length() > 0) {
            all.append(preCall).append('\n');
        }
        all.append(INDENT);
        if (last) {
            all.append("long __assignedId = ");
        }
        all.append("collect").append(collectSignature).append("(cursor, ");
        if (last) {
            if (!idProperty.isTypeNotNull()) {
                all.append(idProperty.getPropertyName()).append(" != null ? ").append(idProperty.getPropertyName())
                        .append(": 0");
            } else {
                all.append(getValue(idProperty));
            }
            all.append(", ");
        } else {
            all.append("0, ");
        }

        String flags = first && last ? "PUT_FLAG_FIRST | PUT_FLAG_COMPLETE" :
                first ? "PUT_FLAG_FIRST" : last ? "PUT_FLAG_COMPLETE" : "0";
        all.append(flags).append(',').append(BR_INDENT_EX);

        all.append(call);
        if (last) {
            all.append(INDENT).append("entity.").append(idProperty.getSetValueExpression("__assignedId")).append(";\n");
        }
    }

    private String getValue(Property property) {
        return "entity." + property.getValueExpression();
    }

}
