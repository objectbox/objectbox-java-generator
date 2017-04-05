/*
 * Copyright (C) 2016 Markus Junginger, greenrobot (http://greenrobot.org)
 *
 * This file is part of ObjectBox Generator.
 *
 * ObjectBox Generator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Generator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Generator.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.generator;

import org.greenrobot.essentials.collections.Multimap;

import io.objectbox.generator.model.Entity;
import io.objectbox.generator.model.Property;
import io.objectbox.generator.model.PropertyType;

import java.util.List;

/**
 * Created by Markus on 20.09.2016.
 */
class PropertyCollector {
    private final static String INDENT = "        ";
    private final static String INDENT_EX = "                ";
    private final static String BR_INDENT_EX = "\n" + INDENT_EX;
    private final static String SEP_BR = ',' + BR_INDENT_EX;

    private final Entity entity;
    private final Multimap<PropertyType, Property> propertiesByType;
    private final Property idProperty;

    public PropertyCollector(Entity entity) {
        this.entity = entity;
        propertiesByType = Multimap.create();
        for (Property property : entity.getProperties()) {
            if (!property.isPrimaryKey()) {
                propertiesByType.putElement(property.getPropertyType(), property);
            }
        }
        idProperty = entity.getPkProperty();
        if (idProperty == null) {
            throw new IllegalStateException("No ID property found for " + entity);
        }
    }

    String createPropertyCollector() {
        StringBuilder preCall = new StringBuilder();
        StringBuilder properties = new StringBuilder();
        StringBuilder all = new StringBuilder();

        boolean first = true;
        int previousPropertyCount = propertiesByType.countElements();
        boolean last = previousPropertyCount == 0;
        while (first || !last) {
            String collectSignature;
            int countByteArrays = propertiesByType.countElements(PropertyType.ByteArray);
            int countScalarsNonFP = countScalarsNonFP();
            int countFloats = propertiesByType.countElements(PropertyType.Float);
            int countDoubles = propertiesByType.countElements(PropertyType.Double);
            int maxCountFP = Math.max(countFloats, countDoubles);
            int countStrings = propertiesByType.countElements(PropertyType.String);
            if (countStrings > 3 || countByteArrays > 1) {
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
                propertiesByType.countElements(PropertyType.Boolean);
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

    private StringBuilder appendProperty(StringBuilder preCall, StringBuilder sb, PropertyType type, boolean isScalar) {
        // TODO improve null values -> don't pass them
        List<Property> properties = propertiesByType.get(type);
        if (properties == null || properties.isEmpty()) {
            // No match, check if we have a another fitting type instead available
            if (type == PropertyType.Long) {
                return appendProperty(preCall, sb, PropertyType.RelationId, isScalar);
            } else if (type == PropertyType.RelationId) {
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
            }

            // All smaller types checked, nothing found
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
            if (entity.isProtobuf()) {
                // TODO Test
                preCall.append(INDENT).append("int ").append(propertyIdLocal).append(" = entity.has")
                        .append(TextUtil.capFirst(name)).append("() ? ").append(propertyId).append(" : 0;\n");
                sb.append(propertyIdLocal).append(", ");
                sb.append(propertyIdLocal).append(" != 0 ? ").append(property.getDatabaseValueExpressionNotNull())
                        .append(isScalar ? " : 0" : " : null");
            } else if (property.isNonPrimitiveType()) {
                preCall.append(INDENT).append(property.getJavaTypeInEntity()).append(' ').append(name)
                        .append(" = ").append(getValue(property)).append(";\n");
                preCall.append(INDENT).append("int ").append(propertyIdLocal).append(" = ").append(name)
                        .append(" != null ? ").append(propertyId).append(" : 0;\n");
                sb.append(propertyIdLocal).append(", ");
                if (isScalar || property.getCustomType() != null) {
                    sb.append(propertyIdLocal).append(" != 0 ? ").append(property.getDatabaseValueExpression())
                            .append(isScalar ? " : 0" : " : null");
                } else {
                    sb.append(property.getDatabaseValueExpression());
                }
            } else {
                sb.append(propertyId).append(", ").append(property.getDatabaseValueExpressionNotNull());
            }
        }
        return sb;
    }

    private void appendCollectCall(String collectSignature, StringBuilder all, StringBuilder preCall,
                                   StringBuilder call, boolean first, boolean last) {
        // ID property before preCall for non-primitives
        // TODO check if we can use fields directly
        if (last && idProperty.isNonPrimitiveType() && !entity.isProtobuf()) {
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
            if (entity.isProtobuf()) {
                all.append("entity.has").append(nameCapFirst(idProperty)).append("()? ").append(getValue(idProperty))
                        .append(": 0");
            } else if (idProperty.isNonPrimitiveType()) {
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
            if (!entity.isProtobuf()) {
                all.append(INDENT).append("entity.set").append(nameCapFirst(idProperty)).append("(__assignedId);\n");
                if (Boolean.TRUE.equals(entity.getActive())) {
                    all.append(INDENT).append("entity.__boxStore = boxStoreForEntities;\n");
                }
            }
            all.append(INDENT).append("return __assignedId;");
        }
    }

    private String getValue(Property property) {
        return "entity." + (property.isFieldAccessible() ? property.getPropertyName()
                : "get" + nameCapFirst(property) + "()");
    }

    private String nameCapFirst(Property property) {
        return TextUtil.capFirst(property.getPropertyName());
    }
}
