package com.rk.xed

import com.rk.file.FileObject

object XedManager {
    /** Returns the project runner configuration directory (.sscode or legacy .xed) within the project root. */
    suspend fun getXedDir(projectRoot: FileObject): FileObject? {
        val sscodeDir = projectRoot.getChild(".sscode")
        if (sscodeDir != null && sscodeDir.exists() && sscodeDir.isDirectory()) {
            return sscodeDir
        }
        return projectRoot.getChild(".xed")
    }

    /** Returns the `runner.sh` file if it exists in `.sscode` or `.xed`. */
    suspend fun getRunScript(projectRoot: FileObject): FileObject? {
        val configDir = getXedDir(projectRoot)
        if (configDir != null && configDir.exists() && configDir.isDirectory()) {
            val runScript = configDir.getChild("runner.sh")
            if (runScript != null && runScript.exists() && runScript.isFile()) {
                return runScript
            }
        }
        return null
    }
}
