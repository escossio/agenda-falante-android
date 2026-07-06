package com.escossio.agendafalante.bridge

import com.escossio.agendafalante.corecontract.ExperiencePackage
import com.escossio.agendafalante.playback.DemoExperienceSelector
import com.escossio.agendafalante.playback.ExperiencePlaybackController
import com.escossio.agendafalante.playback.ExperiencePlaybackState

class EventDispatcher(
    private val demoExperienceSelector: DemoExperienceSelector,
    private val playbackController: ExperiencePlaybackController,
    private val callbacks: Callbacks,
) {
    fun dispatch(event: IncomingCallEvent, experiencePackage: ExperiencePackage) {
        callbacks.onDispatching(event)
        val packageDir = callbacks.packageDirectory()
        val selectedSegments = demoExperienceSelector.select(packageDir, experiencePackage)
        if (selectedSegments.isEmpty()) {
            callbacks.onDispatchFailed(event, "No playable segments found.")
            return
        }

        playbackController.play(selectedSegments.map { it.audioFile }) { state ->
            callbacks.onPlaybackStateChanged(state)
        }
        callbacks.onDispatched(event)
    }

    interface Callbacks {
        fun packageDirectory(): java.io.File
        fun onDispatching(event: IncomingCallEvent)
        fun onDispatched(event: IncomingCallEvent)
        fun onDispatchFailed(event: IncomingCallEvent, reason: String)
        fun onPlaybackStateChanged(state: ExperiencePlaybackState)
    }
}
