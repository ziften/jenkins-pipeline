package com.ziften.jenkins

import java.nio.file.Paths

class FileUtils {
    static def basename(filepath) {
        Paths.get(filepath).getFileName().toString()
    }

    static def filename(filepath) {
        basename(filepath).tokenize('\\.')[0]
    }

    static def extension(filepath) {
        basename(filepath).tokenize('\\.')[1] ?: ''
    }
}
