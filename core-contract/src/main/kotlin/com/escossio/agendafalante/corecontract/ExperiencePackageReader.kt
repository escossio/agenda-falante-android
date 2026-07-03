package com.escossio.agendafalante.corecontract

import java.io.File
import kotlinx.serialization.json.Json

class ExperiencePackageReader(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun read(packageDir: File): ExperiencePackage {
        val manifestFile = packageDir.resolve("manifest.json")
        val metadataFile = packageDir.resolve("metadata.json")
        val checksumsFile = packageDir.resolve("checksums.json")
        val segmentsDir = packageDir.resolve("segments")

        require(manifestFile.isFile) { "manifest.json not found" }
        require(metadataFile.isFile) { "metadata.json not found" }
        require(checksumsFile.isFile) { "checksums.json not found" }
        require(segmentsDir.isDirectory) { "segments directory not found" }

        val manifest = json.decodeFromString(SegmentManifest.serializer(), manifestFile.readText())
        val metadata = json.decodeFromString(PackageMetadata.serializer(), metadataFile.readText())
        val checksums = json.decodeFromString(ChecksumManifest.serializer(), checksumsFile.readText())

        return ExperiencePackage(manifest = manifest, metadata = metadata, checksums = checksums)
    }
}
