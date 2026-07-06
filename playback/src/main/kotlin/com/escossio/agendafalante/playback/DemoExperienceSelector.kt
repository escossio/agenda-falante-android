package com.escossio.agendafalante.playback

import com.escossio.agendafalante.corecontract.ExperiencePackage
import java.io.File

data class SelectedSegment(val segmentId: String, val audioFile: File)

class DemoExperienceSelector {
    fun select(packageDir: File, experiencePackage: ExperiencePackage?): List<SelectedSegment> {
        val manifestSegments = experiencePackage?.manifest?.segments.orEmpty()
        if (manifestSegments.isEmpty()) return emptyList()

        return manifestSegments.mapNotNull { segment ->
            if (segment.status != "available") return@mapNotNull null
            val audioPath = segment.audioPath?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val audioFile = packageDir.resolve(audioPath)
            if (!audioFile.isFile) return@mapNotNull null
            SelectedSegment(segmentId = segment.segmentId, audioFile = audioFile)
        }
    }
}
