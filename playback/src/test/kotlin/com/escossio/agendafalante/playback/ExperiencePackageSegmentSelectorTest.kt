package com.escossio.agendafalante.playback

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ExperiencePackageSegmentSelectorTest {
    @Test
    fun selects_first_available_segment() {
        val dir = createTempPackage()
        val selected = firstAvailableSegment(dir)
        assertNotNull(selected)
        assertEquals("seg-1", selected.segmentId)
    }

    @Test
    fun returns_null_without_manifest() {
        val dir = createTempDir()
        assertNull(firstAvailableSegment(dir))
    }

    private fun createTempPackage(): File {
        val root = kotlin.io.path.createTempDirectory().toFile()
        val pkg = root.resolve("pkg")
        pkg.mkdirs()
        pkg.resolve("segments").mkdirs()
        pkg.resolve("manifest.json").writeText(
            """{"packageId":"demo-package","packageType":"experience_package","segments":[{"segmentId":"seg-1","segmentType":"contact_name","text":"A","language":"pt-BR","voice":"default","sourceContactId":"x","status":"available","audioPath":"segments/seg-1.wav"},{"segmentId":"seg-2","segmentType":"contact_name","text":"B","language":"pt-BR","voice":"default","sourceContactId":"y","status":"available","audioPath":"segments/seg-2.wav"}]}""",
        )
        pkg.resolve("segments/seg-1.wav").writeText("a")
        pkg.resolve("segments/seg-2.wav").writeText("b")
        return pkg
    }
}
