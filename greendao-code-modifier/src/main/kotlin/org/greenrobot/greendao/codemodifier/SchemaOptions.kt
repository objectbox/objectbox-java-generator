package org.greenrobot.greendao.codemodifier

import java.io.File

data class SchemaOptions(
    val name: String,
    val version: Int,
    val daoPackage: String?,
    val outputDir: File,
    val testsOutputDir: File?
)