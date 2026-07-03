package com.escossio.agendafalante.corecontract

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExperiencePackageReaderTest {
    @Test
    fun reads_valid_package() {
        val dir = createTempPackage()
        val reader = ExperiencePackageReader()
        val pkg = reader.read(dir)
        assertEquals("demo-package", pkg.metadata.packageId)
        assertEquals(2, pkg.manifest.segments.size)
    }

    @Test
    fun fails_without_manifest() {
        val dir = createTempPackage()
        dir.resolve("manifest.json").delete()
        val reader = ExperiencePackageReader()
        assertFailsWith<IllegalArgumentException> { reader.read(dir) }
    }

    @Test
    fun fails_without_metadata() {
        val dir = createTempPackage()
        dir.resolve("metadata.json").delete()
        val reader = ExperiencePackageReader()
        assertFailsWith<IllegalArgumentException> { reader.read(dir) }
    }

    @Test
    fun fails_without_checksums() {
        val dir = createTempPackage()
        dir.resolve("checksums.json").delete()
        val reader = ExperiencePackageReader()
        assertFailsWith<IllegalArgumentException> { reader.read(dir) }
    }

    @Test
    fun fails_without_segments_dir() {
        val dir = createTempPackage()
        dir.resolve("segments").deleteRecursively()
        val reader = ExperiencePackageReader()
        assertFailsWith<IllegalArgumentException> { reader.read(dir) }
    }

    private fun createTempPackage(): File {
        val root = kotlin.io.path.createTempDirectory().toFile()
        val pkg = root.resolve("pkg")
        pkg.mkdirs()
        pkg.resolve("segments").mkdirs()
        pkg.resolve("manifest.json").writeText(
            """{"packageId":"demo-package","packageType":"experience_package","segments":[{"segmentId":"a","segmentType":"contact_name","text":"A","language":"pt-BR","voice":"default","sourceContactId":"x","status":"available","audioPath":"segments/a.wav"},{"segmentId":"b","segmentType":"contact_name","text":"B","language":"pt-BR","voice":"default","sourceContactId":"y","status":"available","audioPath":"segments/b.wav"}]}""",
        )
        pkg.resolve("metadata.json").writeText(
            """{"packageId":"demo-package","packageType":"experience_package","createdAt":"2026-07-03T00:00:00Z","version":"0.1.0-alpha","source":{"resolvedSegmentCatalog":"resolved","audioDir":"audio"}}""",
        )
        pkg.resolve("checksums.json").writeText(
            """{"packageId":"demo-package","packageType":"experience_package","files":{"segments/a.wav":"x","segments/b.wav":"y"}}""",
        )
        pkg.resolve("segments/a.wav").writeText("a")
        pkg.resolve("segments/b.wav").writeText("b")
        return pkg
    }
}
