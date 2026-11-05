package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val albumArt: String,
    val audioUrl: String,
    val durationFormatted: String,
    val genre: String,
    val likesCount: Int = 1420
)

data class MusicPlayerState(
    val currentTrack: MusicTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val playlist: List<MusicTrack> = emptyList(),
    val favoriteTrackId: String? = null
)

object MusicPlayerManager {
    private const val TAG = "MusicPlayerManager"
    private const val PREFS_NAME = "iddet_music_prefs"
    private const val KEY_PROFILE_SONG_ID = "profile_favorite_song_id"

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    private val _state = MutableStateFlow(MusicPlayerState())
    val state: StateFlow<MusicPlayerState> = _state.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val favId = prefs.getString(KEY_PROFILE_SONG_ID, null)
        _state.value = _state.value.copy(
            favoriteTrackId = favId
        )
    }

    fun updatePlaylist(tracks: List<MusicTrack>) {
        if (tracks.isEmpty()) {
            _state.value = _state.value.copy(playlist = emptyList())
            return
        }
        val current = _state.value.currentTrack
        val updatedCurrent = if (current != null && tracks.any { it.id == current.id }) {
            tracks.find { it.id == current.id }
        } else {
            current ?: tracks.firstOrNull()
        }
        _state.value = _state.value.copy(
            playlist = tracks,
            currentTrack = updatedCurrent
        )
    }

    fun setFavoriteTrackOnProfile(context: Context, track: MusicTrack) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROFILE_SONG_ID, track.id).apply()
        _state.value = _state.value.copy(favoriteTrackId = track.id)
    }

    fun getFavoriteTrack(context: Context): MusicTrack? {
        val favId = _state.value.favoriteTrackId
        return if (favId != null) {
            _state.value.playlist.find { it.id == favId } ?: _state.value.currentTrack
        } else {
            _state.value.currentTrack
        }
    }

    fun playTrack(track: MusicTrack) {
        if (_state.value.currentTrack?.id == track.id && mediaPlayer != null) {
            togglePlayPause()
            return
        }

        stopPlayback()
        _state.value = _state.value.copy(
            currentTrack = track,
            isPlaying = true,
            currentPositionMs = 0,
            durationMs = 0
        )

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(track.audioUrl)
                setOnPreparedListener { mp ->
                    mp.start()
                    _state.value = _state.value.copy(
                        isPlaying = true,
                        durationMs = mp.duration
                    )
                    startProgressTracking()
                }
                setOnCompletionListener {
                    if (_state.value.isRepeat) {
                        it.seekTo(0)
                        it.start()
                    } else {
                        nextTrack()
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _state.value = _state.value.copy(isPlaying = false)
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing music track", e)
            _state.value = _state.value.copy(isPlaying = false)
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player == null) {
            val current = _state.value.currentTrack ?: _state.value.playlist.firstOrNull()
            if (current != null) {
                playTrack(current)
            }
            return
        }

        try {
            if (player.isPlaying) {
                player.pause()
                _state.value = _state.value.copy(isPlaying = false)
                stopProgressTracking()
            } else {
                player.start()
                _state.value = _state.value.copy(isPlaying = true)
                startProgressTracking()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling play/pause", e)
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _state.value = _state.value.copy(currentPositionMs = positionMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking", e)
        }
    }

    fun nextTrack() {
        val playlist = _state.value.playlist
        if (playlist.isEmpty()) return
        val currentIndex = playlist.indexOfFirst { it.id == _state.value.currentTrack?.id }
        val nextIndex = if (_state.value.isShuffle) {
            (playlist.indices).filter { it != currentIndex }.randomOrNull() ?: 0
        } else {
            (currentIndex + 1) % playlist.size
        }
        playTrack(playlist[nextIndex])
    }

    fun previousTrack() {
        val playlist = _state.value.playlist
        if (playlist.isEmpty()) return
        val currentIndex = playlist.indexOfFirst { it.id == _state.value.currentTrack?.id }
        val prevIndex = if (currentIndex <= 0) playlist.size - 1 else currentIndex - 1
        playTrack(playlist[prevIndex])
    }

    fun toggleShuffle() {
        _state.value = _state.value.copy(isShuffle = !_state.value.isShuffle)
    }

    fun toggleRepeat() {
        _state.value = _state.value.copy(isRepeat = !_state.value.isRepeat)
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressRunnable = object : Runnable {
            override fun run() {
                try {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            _state.value = _state.value.copy(
                                currentPositionMs = player.currentPosition,
                                durationMs = player.duration
                            )
                            progressRunnable?.let { handler.postDelayed(it, 500) }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun stopProgressTracking() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
    }

    fun stopPlayback() {
        stopProgressTracking()
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        } finally {
            mediaPlayer = null
        }
        _state.value = _state.value.copy(isPlaying = false, currentPositionMs = 0)
    }
}
