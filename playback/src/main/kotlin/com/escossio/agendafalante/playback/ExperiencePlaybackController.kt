package com.escossio.agendafalante.playback

import android.content.Context
import android.media.MediaPlayer
import java.io.File

enum class ExperiencePlaybackState {
    Idle,
    Playing,
    Completed,
    Failed,
}

class ExperiencePlaybackController(private val context: Context) {
    private val lock = Any()
    private var mediaPlayer: MediaPlayer? = null
    private var currentQueue: List<File> = emptyList()
    private var currentIndex: Int = 0
    private var currentState: ExperiencePlaybackState = ExperiencePlaybackState.Idle

    fun play(wavFiles: List<File>, onStateChanged: (ExperiencePlaybackState) -> Unit = {}) {
        stop()
        if (wavFiles.isEmpty()) {
            updateState(ExperiencePlaybackState.Completed, onStateChanged)
            return
        }

        synchronized(lock) {
            currentQueue = wavFiles
            currentIndex = 0
        }
        playCurrent(onStateChanged)
    }

    fun stop() {
        synchronized(lock) {
            currentQueue = emptyList()
            currentIndex = 0
        }
        mediaPlayer?.run {
            try {
                setOnCompletionListener(null)
                setOnErrorListener(null)
                if (isPlaying) stop()
            } catch (_: Throwable) {
            } finally {
                release()
            }
        }
        mediaPlayer = null
        currentState = ExperiencePlaybackState.Idle
    }

    fun release() {
        stop()
    }

    fun state(): ExperiencePlaybackState = currentState

    private fun playCurrent(onStateChanged: (ExperiencePlaybackState) -> Unit) {
        val wavFile = synchronized(lock) { currentQueue.getOrNull(currentIndex) }
        if (wavFile == null) {
            updateState(ExperiencePlaybackState.Completed, onStateChanged)
            return
        }
        if (!wavFile.isFile) {
            updateState(ExperiencePlaybackState.Failed, onStateChanged)
            return
        }

        try {
            val player = MediaPlayer().apply {
                setDataSource(wavFile.absolutePath)
                setOnPreparedListener {
                    updateState(ExperiencePlaybackState.Playing, onStateChanged)
                    start()
                }
                setOnCompletionListener {
                    releasePlayer()
                    val nextIndex = synchronized(lock) {
                        currentIndex += 1
                        currentIndex
                    }
                    if (nextIndex >= synchronized(lock) { currentQueue.size }) {
                        updateState(ExperiencePlaybackState.Completed, onStateChanged)
                    } else {
                        playCurrent(onStateChanged)
                    }
                }
                setOnErrorListener { _, _, _ ->
                    releasePlayer()
                    updateState(ExperiencePlaybackState.Failed, onStateChanged)
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (_: Throwable) {
            releasePlayer()
            updateState(ExperiencePlaybackState.Failed, onStateChanged)
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.run {
            try {
                setOnCompletionListener(null)
                setOnErrorListener(null)
                if (isPlaying) stop()
            } catch (_: Throwable) {
            } finally {
                release()
            }
        }
        mediaPlayer = null
    }

    private fun updateState(
        state: ExperiencePlaybackState,
        onStateChanged: (ExperiencePlaybackState) -> Unit,
    ) {
        currentState = state
        onStateChanged(state)
    }
}
