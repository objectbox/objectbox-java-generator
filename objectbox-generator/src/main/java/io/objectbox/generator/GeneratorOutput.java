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
public class GeneratorOutput {
    public static GeneratorOutput create(String outDir) throws IOException {
        return create(toFileForceExists(outDir));
    }

    public static GeneratorOutput create(File outDirFile) {
        return new GeneratorOutput(null, outDirFile);
    }

    public static GeneratorOutput create(Filer filer) {
        return new GeneratorOutput(filer, null);
    }

    final Filer filer;
    final File outDirFile;

    private GeneratorOutput(Filer filer, File outDirFile) {
        this.filer = filer;
        this.outDirFile = outDirFile;
    }

    protected Writer createWriter(String javaPackage, String fileOrJavaClassName,
            String fileExtension) throws IOException {
        if (outDirFile != null) {
            return new FileWriter(getFileOrNull(javaPackage, fileOrJavaClassName, fileExtension));
        } else if (filer != null && ".java".equals(fileExtension)) {
            // note: filer could write to files with other extensions, but warns about it, so we do not
            String fileName;
            if (javaPackage != null && javaPackage.length() != 0) {
                fileName = javaPackage + "." + fileOrJavaClassName;
            } else {
                fileName = fileOrJavaClassName; // no package
            }
            JavaFileObject sourceFile = filer.createSourceFile(fileName);
            return sourceFile.openWriter();
        } else {
            throw new IllegalStateException("Info missing");
        }
    }

    protected File getFileOrNull(String javaPackage, String fileName, String fileExtension) {
        if (outDirFile != null) {
            File file = toFile(outDirFile, javaPackage, fileName, fileExtension);
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

    private File toFile(File outDirFile, String javaPackage, String fileName, String fileExtension) {
        String packageSubPath = javaPackage.replace('.', '/');
        File packagePath = new File(outDirFile, packageSubPath);
        return new File(packagePath, fileName + fileExtension);
    }

}
