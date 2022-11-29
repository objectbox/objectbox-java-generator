/**
 * Configures test tasks to disable ObjectBox Analytics.
 */

import org.gradle.api.tasks.testing.Test

afterEvaluate {
    tasks.all {
        if (this is Test) {
            // Do not send events, only log them (e.g. builds run frequently; errors are triggered on purpose).
            println("Set OBX_DISABLE_ANALYTICS=true for ${this.name}")
            this.environment("OBX_DISABLE_ANALYTICS", "true")
        }
    }
}
