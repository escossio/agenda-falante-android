package com.escossio.agendafalante.playback

import android.content.Context
import android.media.MediaPlayer
import java.io.File

class AudioPlaybackController(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun play(wavFile: File, onPlaying: () -> Unit = {}, onCompleted: () -> Unit = {}, onFailed: (Throwable) -> Unit = {}) {
        stop()
        try {
            val player = MediaPlayer().apply {
                setDataSource(wavFile.absolutePath)
                setOnPreparedListener {
                    onPlaying()
                    start()
                }
                setOnCompletionListener {
                    onCompleted()
                    release()
                    mediaPlayer = null
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    mediaPlayer = null
                    onFailed(IllegalStateException("MediaPlayer error"))
                    true
                }
                prepare()
            }
            mediaPlayer = player
        } catch (t: Throwable) {
            mediaPlayer?.release()
            mediaPlayer = null
            onFailed(t)
        }
    }

    fun stop() {
        mediaPlayer?.run {
            try {
                if (isPlaying) stop()
            } catch (_: Throwable) {
            } finally {
                release()
            }
        }
        mediaPlayer = null
    }

    fun release() {
        stop()
    }
}
