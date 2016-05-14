package org.greenrobot.greendao.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import java.io.File

open class DetectCandidatesTask : DefaultTask() {

    @InputFiles
    var sourceFiles: FileCollection? = null

    @OutputFile
    var candidatesFile: File? = null

    @Input
    var version: String = "unknown"

    private val token = "org.greenrobot.greendao.annotation".toByteArray()

    @TaskAction
    fun execute(inputs: IncrementalTaskInputs) {
        val candidatesFile = this.candidatesFile ?: throw IllegalStateException("candidates should be defined")

        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val oldCandidates = if (candidatesFile.exists()) candidatesFile.readLines().toSet() else emptySet()
        val newCandidates = oldCandidates.toMutableSet()

        var modifiedExisting = false

        // check each modified file and add/remove candidates
        inputs.outOfDate { change: InputFileDetails ->
            val file = change.file
            if (file.isFile) {
                if (file.isCandidate(token, buffer)) {
                    val listChanged = newCandidates.add(file.path)
                    modifiedExisting = !listChanged or modifiedExisting
                } else {
                    newCandidates.remove(file.path)
                }
            }
        }

        // removed candidates for removed files
        inputs.removed { change: InputFileDetails ->
            newCandidates.remove(change.file.path)
        }

        // only if list is changed or previously added candidate is changed
        // the latter one is a way to notify existing task about changed file
        if (newCandidates != oldCandidates || modifiedExisting) {
            // write candidates into file line by line
            candidatesFile.printWriter().use { writer ->
                newCandidates.forEach {
                    writer.println(it)
                }
            }
        }
    }

    /** Search for token ignoring whitespaces. Supports only ASCI characters (works on raw bytes) */
    fun File.isCandidate(token: ByteArray, buffer: ByteArray): Boolean {
        val SPACE = ' '.toByte()
        val TAB = '\t'.toByte()
        val CR = '\r'.toByte()
        val LF = '\n'.toByte()
        val tokenSize = token.size
        inputStream().buffered().use { stream ->
            var index = 0
            do {
                val read = stream.read(buffer)
                for (i in 0..read - 1) {
                    val b = buffer[i]
                    if (index == 0) {
                        if (b == token[0]) {
                            index = 1
                        }
                    } else if (b == token[index]){
                        index++
                        if (index == tokenSize) {
                            return true
                        }
                    } else if (!(b == SPACE || b == TAB || b == CR || b == LF)) {
                        index = 0
                    }
                }
            } while (read >= 0)
        }
        return false
    }
}