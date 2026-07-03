package com.escossio.agendafalante.corecontract

import kotlinx.serialization.Serializable

@Serializable
data class ExperiencePackage(
    val manifest: SegmentManifest,
    val metadata: PackageMetadata,
    val checksums: ChecksumManifest,
)

@Serializable
data class PackageMetadata(
    val packageId: String,
    val packageType: String,
    val createdAt: String,
    val version: String,
    val source: PackageSource,
)

@Serializable
data class PackageSource(
    val resolvedSegmentCatalog: String,
    val audioDir: String,
)

@Serializable
data class SegmentManifest(
    val packageId: String,
    val packageType: String,
    val segments: List<ManifestSegment>,
)

@Serializable
data class ManifestSegment(
    val segmentId: String,
    val segmentType: String,
    val text: String,
    val language: String,
    val voice: String,
    val sourceContactId: String,
    val status: String,
    val audioPath: String? = null,
)

@Serializable
data class ChecksumManifest(
    val packageId: String,
    val packageType: String,
    val files: Map<String, String>,
)

data class ExperiencePackageValidation(
    val valid: Boolean,
    val warnings: List<String>,
    val errors: List<String>,
)
