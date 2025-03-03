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

/** Internal API */
public class TextUtil {
    private final static boolean OBJECTBOX = true;

    // TODO make this flexible (e.g. DbNameProvider class);
    public static String dbName(String javaName) {
        if (OBJECTBOX) {
            return javaName;
        }
        StringBuilder builder = new StringBuilder(javaName);
        for (int i = 1; i < builder.length(); i++) {
            boolean lastWasUpper = Character.isUpperCase(builder.charAt(i - 1));
            boolean isUpper = Character.isUpperCase(builder.charAt(i));
            if (isUpper && !lastWasUpper) {
                builder.insert(i, '_');
                i++;
            }
        }
        return builder.toString().toUpperCase();
    }

    public static String getClassnameFromFullyQualified(String clazz) {
        int index = clazz.lastIndexOf('.');
        if (index != -1) {
            return clazz.substring(index + 1);
        } else {
            return clazz;
        }
    }

    public static String capFirst(String string) {
        return Character.toUpperCase(string.charAt(0)) + (string.length() > 1 ? string.substring(1) : "");
    }

    public static String getPackageFromFullyQualified(String clazz) {
        int index = clazz.lastIndexOf('.');
        if (index != -1) {
            return clazz.substring(0, index);
        } else {
            return null;
        }
    }

}
