package com.escossio.agendafalante.playback

import java.io.File

data class SelectedSegment(val segmentId: String, val audioFile: File)

fun firstAvailableSegment(packageDir: File): SelectedSegment? {
    val manifest = packageDir.resolve("manifest.json").takeIf { it.isFile } ?: return null
    val json = manifest.readText()
    val regex = Regex("\"segmentId\"\\s*:\\s*\"([^\"]+)\".*?\"status\"\\s*:\\s*\"available\".*?\"audioPath\"\\s*:\\s*\"([^\"]+)\"", RegexOption.DOT_MATCHES_ALL)
    val match = regex.find(json) ?: return null
    val segmentId = match.groupValues[1]
    val audioPath = match.groupValues[2]
    return SelectedSegment(segmentId = segmentId, audioFile = packageDir.resolve(audioPath))
}
