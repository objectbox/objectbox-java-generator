/*
 * Copyright (C) 2017-2018 ObjectBox Ltd.
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

package io.objectbox.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;

/**
 * Abstraction for output used by {@link io.objectbox.generator.BoxGenerator}.
 * Can refer to a directory or a annotation processor Filer.
 */
class GeneratorOutput {
    final Filer filer;
    final File outDirFile;

    GeneratorOutput(Filer filer) {
        this.filer = filer;
        this.outDirFile = null;
    }

    GeneratorOutput(File outDirFile) {
        this.filer = null;
        this.outDirFile = outDirFile;
    }

    GeneratorOutput(String outDir) throws IOException {
        this(toFileForceExists(outDir));
    }

    Writer createWriter(String javaPackage, String javaClassName) throws IOException {
        if (outDirFile != null) {
            return new FileWriter(getFileOrNull(javaPackage, javaClassName));
        } else if (filer != null) {
            String fileName = javaPackage + "." + javaClassName;
            JavaFileObject sourceFile = filer.createSourceFile(fileName);
            return sourceFile.openWriter();
        } else {
            throw new IllegalStateException("Info missing");
        }
    }

    File getFileOrNull(String javaPackage, String javaClassName) {
        if (outDirFile != null) {
            File file = toJavaFilename(outDirFile, javaPackage, javaClassName);
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();
            return file;
        } else {
            return null;
        }
    }

    private static File toFileForceExists(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new IOException(String.format("%s does not exist. " +
                            "This check is to prevent accidental file generation into a wrong path. (resolved path=%s)",
                    filename, file.getAbsolutePath()));
        }
        return file;
    }

    private File toJavaFilename(File outDirFile, String javaPackage, String javaClassName) {
        String packageSubPath = javaPackage.replace('.', '/');
        File packagePath = new File(outDirFile, packageSubPath);
        return new File(packagePath, javaClassName + ".java");
    }

}
