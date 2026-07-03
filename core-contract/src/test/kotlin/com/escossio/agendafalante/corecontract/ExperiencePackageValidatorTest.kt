package com.escossio.agendafalante.corecontract

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExperiencePackageValidatorTest {
    @Test
    fun validates_valid_package() {
        val pkg = createTempPackage()
        val result = ExperiencePackageValidator().validate(pkg)
        assertTrue(result.valid)
    }

    @Test
    fun detects_segment_count_mismatch() {
        val pkg = createTempPackage()
        pkg.resolve("segments/b.wav").delete()
        val result = ExperiencePackageValidator().validate(pkg)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("segment count mismatch") })
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
