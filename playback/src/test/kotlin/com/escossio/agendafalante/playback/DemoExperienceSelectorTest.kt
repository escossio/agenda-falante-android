package com.escossio.agendafalante.playback

import com.escossio.agendafalante.corecontract.ChecksumManifest
import com.escossio.agendafalante.corecontract.ExperiencePackage
import com.escossio.agendafalante.corecontract.ManifestSegment
import com.escossio.agendafalante.corecontract.PackageMetadata
import com.escossio.agendafalante.corecontract.PackageSource
import com.escossio.agendafalante.corecontract.SegmentManifest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class DemoExperienceSelectorTest {
    private val selector = DemoExperienceSelector()

    @Test
    fun selects_available_segments_in_manifest_order() {
        val pkg = createPackage(
            segments = listOf(
                availableSegment("seg-1", "segments/seg-1.wav"),
                availableSegment("seg-2", "segments/seg-2.wav"),
            ),
        )

        val selected = selector.select(pkg.root, pkg.experiencePackage)

        assertEquals(listOf("seg-1", "seg-2"), selected.map { it.segmentId })
    }

    @Test
    fun ignores_unavailable_segments() {
        val pkg = createPackage(
            segments = listOf(
                unavailableSegment("seg-1", "segments/seg-1.wav"),
                availableSegment("seg-2", "segments/seg-2.wav"),
            ),
        )

        val selected = selector.select(pkg.root, pkg.experiencePackage)

        assertEquals(listOf("seg-2"), selected.map { it.segmentId })
    }

    @Test
    fun returns_empty_list_for_package_without_segments() {
        val pkg = createPackage(segments = emptyList())

        val selected = selector.select(pkg.root, pkg.experiencePackage)

        assertEquals(emptyList(), selected)
    }

    @Test
    fun ignores_missing_audio_files() {
        val pkg = createPackage(
            segments = listOf(
                availableSegment("seg-1", "segments/missing.wav"),
            ),
        )

        val selected = selector.select(pkg.root, pkg.experiencePackage)

        assertEquals(emptyList(), selected)
    }

    private fun createPackage(segments: List<ManifestSegment>): TestPackage {
        val root = createTempDirectory().toFile().resolve("pkg")
        root.mkdirs()
        root.resolve("segments").mkdirs()
        val packageId = "demo-package"
        root.resolve("manifest.json").writeText(
            buildString {
                append("""{"packageId":"$packageId","packageType":"experience_package","segments":[""")
                segments.forEachIndexed { index, segment ->
                    if (index > 0) append(',')
                    append(
                        """{"segmentId":"${segment.segmentId}","segmentType":"${segment.segmentType}","text":"${segment.text}","language":"${segment.language}","voice":"${segment.voice}","sourceContactId":"${segment.sourceContactId}","status":"${segment.status}","audioPath":${segment.audioPath?.let { "\"$it\"" } ?: "null"}}""",
                    )
                }
                append("]}")
            },
        )
        root.resolve("metadata.json").writeText(
            """{"packageId":"$packageId","packageType":"experience_package","createdAt":"2026-01-01T00:00:00Z","version":"1","source":{"resolvedSegmentCatalog":"catalog.json","audioDir":"segments"}}""",
        )
        root.resolve("checksums.json").writeText(
            """{"packageId":"$packageId","packageType":"experience_package","files":{}}""",
        )
        segments.forEach { segment ->
            segment.audioPath?.let { audioPath ->
                if (audioPath != "segments/missing.wav") {
                    root.resolve(audioPath).parentFile?.mkdirs()
                    root.resolve(audioPath).writeText("wav")
                }
            }
        }
        return TestPackage(root = root, experiencePackage = ExperiencePackage(
            manifest = SegmentManifest(packageId = packageId, packageType = "experience_package", segments = segments),
            metadata = PackageMetadata(
                packageId = packageId,
                packageType = "experience_package",
                createdAt = "2026-01-01T00:00:00Z",
                version = "1",
                source = PackageSource(resolvedSegmentCatalog = "catalog.json", audioDir = "segments"),
            ),
            checksums = ChecksumManifest(packageId = packageId, packageType = "experience_package", files = emptyMap()),
        ))
    }

    private fun availableSegment(segmentId: String, audioPath: String) = ManifestSegment(
        segmentId = segmentId,
        segmentType = "contact_name",
        text = segmentId,
        language = "pt-BR",
        voice = "default",
        sourceContactId = segmentId,
        status = "available",
        audioPath = audioPath,
    )

    private fun unavailableSegment(segmentId: String, audioPath: String) = availableSegment(segmentId, audioPath).copy(status = "missing")

    private data class TestPackage(val root: File, val experiencePackage: ExperiencePackage)
}
