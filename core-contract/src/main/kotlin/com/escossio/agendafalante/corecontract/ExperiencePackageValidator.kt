package com.escossio.agendafalante.corecontract

import java.io.File

class ExperiencePackageValidator {
    fun validate(packageDir: File): ExperiencePackageValidation {
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        val manifestFile = packageDir.resolve("manifest.json")
        val metadataFile = packageDir.resolve("metadata.json")
        val checksumsFile = packageDir.resolve("checksums.json")
        val segmentsDir = packageDir.resolve("segments")

        if (!manifestFile.isFile) errors += "manifest.json missing"
        if (!metadataFile.isFile) errors += "metadata.json missing"
        if (!checksumsFile.isFile) errors += "checksums.json missing"
        if (!segmentsDir.isDirectory) errors += "segments directory missing"

        if (errors.isEmpty()) {
            val manifest = ExperiencePackageReader().read(packageDir).manifest
            val expectedCount = manifest.segments.size
            val actualCount = segmentsDir.listFiles { file -> file.isFile && file.extension == "wav" }?.size ?: 0
            if (expectedCount != actualCount) {
                errors += "segment count mismatch: manifest=$expectedCount files=$actualCount"
            }
        }

        return ExperiencePackageValidation(
            valid = errors.isEmpty(),
            warnings = warnings,
            errors = errors,
        )
    }
}
