package com.ayyappasamaaj.tattvamasi.ui.yatra

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.ayyappasamaaj.tattvamasi.R
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/*
 * The Namavali player, one for the whole app, as the website's (static/js/src/pages/sahasranamaYatra.js): Listen plays
 * one name; Play all goes through the names in order from where the listener stopped, over the tanpura drone (in C,
 * bundled: res/raw/tanpura_c.m4a, at the website's level), with a short pause between names, the same five speeds
 * (the voice keeps its pitch; the tanpura isn't sped up), and the next clips fetched ahead by the player. It keeps
 * playing in the background, with the lock screen's and earphones' controls (NamavaliService), and remembers where
 * listening stopped and the speed on the device.
 */

data class ListenState(
    /** The name playing or last played (its number), or null before anything played. */
    val n: Int? = null,
    /** Play all (over the tanpura), not a single name. */
    val all: Boolean = false,
    /** Sounding, or in the pause between names. */
    val playing: Boolean = false,
    /** The player bar is shown (from the first Listen or Play all until Close). */
    val shown: Boolean = false,
    val speed: Float = 1f,
    /** A message for the listener ("The Namavali is complete."), once. */
    val message: String? = null,
)

// Holds the application's context (not an activity's) and the players, for the app's whole run: no leak
@SuppressLint("StaticFieldLeak")
@OptIn(UnstableApi::class)
object NamavaliPlayer {
    val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

    /** Each clip has a little silence before and after (0.15 s, 0.30 s), so a short pause is enough; it shortens with the speed. */
    private const val NAME_GAP_MS = 300L
    private const val TANPURA_LEVEL = 0.70f  // the website's level, under the voice
    private const val PREFS = "namavali-player"
    private const val KEY_AT = "listen-at"
    private const val KEY_SPEED = "speed"

    private val _state = MutableStateFlow(ListenState())
    val state: StateFlow<ListenState> = _state.asStateFlow()

    private var voice: ExoPlayer? = null
    private var drone: ExoPlayer? = null
    private var controller: ListenableFuture<MediaController>? = null
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var gap: Runnable? = null
    private lateinit var app: Context
    private var lastRecorded: Int? = null

    private val prefs get() = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        val saved = prefs.getFloat(KEY_SPEED, 1f)
        _state.update { it.copy(speed = if (saved in SPEEDS) saved else 1f) }
    }

    /** Where listening stopped (a name's number), or null. */
    fun savedPosition(context: Context): Int? {
        init(context)
        return prefs.getInt(KEY_AT, 0).takeIf { it > 0 }
    }

    /** The voice, for the media session (lock screen, earphones, notification). */
    fun voice(context: Context): ExoPlayer {
        init(context)
        voice?.let { return it }
        val player = ExoPlayer.Builder(app)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                true,  // pauses for a call or another app's sound
            )
            .setHandleAudioBecomingNoisy(true)  // earphones unplugged: pause
            .build()
        player.addListener(listener)
        voice = player
        return player
    }

    private fun tanpura(): ExoPlayer = drone ?: ExoPlayer.Builder(app)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
        .build().also { player ->
            player.setMediaItem(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(R.raw.tanpura_c)))
            player.repeatMode = Player.REPEAT_MODE_ALL
            player.volume = TANPURA_LEVEL
            player.prepare()
            drone = player
        }

    private fun droneOn(on: Boolean) {
        if (on) tanpura().play() else drone?.pause()
    }

    /** Starts the media session's service (bound by a controller), so playing goes on in the background. */
    private fun connect() {
        if (controller != null) return
        val token = SessionToken(app, ComponentName(app, NamavaliService::class.java))
        controller = MediaController.Builder(app, token).buildAsync()
    }

    private fun item(n: Int, url: String, title: String): MediaItem = MediaItem.Builder()
        .setMediaId(n.toString())
        .setUri(url)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist("Sahasranama Yatra").build())
        .build()

    /**
     * Plays name [n]: on its own, or with [all] the names from it onwards over the tanpura. [recorded] is every name with a
     * recording, in order (its number, its clip's address, a title for the lock screen).
     */
    fun play(context: Context, recorded: List<Triple<Int, String, String>>, n: Int, all: Boolean) {
        val player = voice(context)
        connect()
        cancelGap()
        lastRecorded = recorded.lastOrNull()?.first
        val start = recorded.indexOfFirst { it.first >= n }.coerceAtLeast(0)
        val items = if (all) recorded.map { item(it.first, it.second, it.third) }
        else recorded.filter { it.first == n }.map { item(it.first, it.second, it.third) }
        if (items.isEmpty()) return
        player.pauseAtEndOfMediaItems = all  // Play all pauses between names (the gap), then goes on
        player.setMediaItems(items, if (all) start else 0, 0L)
        player.setPlaybackParameters(PlaybackParameters(_state.value.speed))
        player.prepare()
        player.play()
        droneOn(all)
        _state.update { it.copy(n = items[if (all) start else 0].mediaId.toInt(), all = all, playing = true, shown = true) }
        if (all) remember(_state.value.n)
    }

    /** Play / Pause in the bar: pauses everything, or goes on from the name shown (Play all if it was). */
    fun toggle(context: Context, recorded: List<Triple<Int, String, String>>) {
        val s = _state.value
        if (s.playing) pause() else play(context, recorded, s.n ?: savedPosition(context) ?: recorded.firstOrNull()?.first ?: 1, all = true)
    }

    fun pause() {
        cancelGap()
        voice?.pause()
        droneOn(false)
        _state.update { it.copy(playing = false) }
    }

    fun next(context: Context, recorded: List<Triple<Int, String, String>>) = step(context, recorded, +1)
    fun previous(context: Context, recorded: List<Triple<Int, String, String>>) = step(context, recorded, -1)

    private fun step(context: Context, recorded: List<Triple<Int, String, String>>, by: Int) {
        val at = recorded.indexOfFirst { it.first == _state.value.n }
        val to = recorded.getOrNull(at + by) ?: return
        play(context, recorded, to.first, all = true)
    }

    fun cycleSpeed() {
        val next = SPEEDS[(SPEEDS.indexOf(_state.value.speed).coerceAtLeast(0) + 1) % SPEEDS.size]
        voice?.setPlaybackParameters(PlaybackParameters(next))
        prefs.edit().putFloat(KEY_SPEED, next).apply()
        _state.update { it.copy(speed = next) }
    }

    /** Close in the bar: stops, and hides the bar (where listening stopped is kept). */
    fun close() {
        cancelGap()
        voice?.stop()
        voice?.clearMediaItems()
        droneOn(false)
        _state.update { it.copy(playing = false, shown = false, all = false) }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun remember(n: Int?) {
        if (n != null) prefs.edit().putInt(KEY_AT, n).apply()
    }

    private fun cancelGap() {
        gap?.let(handler::removeCallbacks)
        gap = null
    }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val n = mediaItem?.mediaId?.toIntOrNull() ?: return
            _state.update { it.copy(n = n) }
            if (_state.value.all) remember(n)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            val player = voice ?: return
            when {
                // The end of a name in Play all: a short pause, then the next (the tanpura goes on)
                !playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM && _state.value.all -> {
                    if (player.hasNextMediaItem()) {
                        val wait = (NAME_GAP_MS / _state.value.speed).toLong()
                        gap = Runnable { gap = null; player.seekToNextMediaItem(); player.play() }.also { handler.postDelayed(it, wait) }
                    } else finish(player)
                }
                // Paused or played from the lock screen, earphones, a call: the tanpura follows
                reason != Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM -> {
                    if (!playWhenReady) cancelGap()
                    droneOn(playWhenReady && _state.value.all)
                    _state.update { it.copy(playing = playWhenReady) }
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = voice ?: return
            if (playbackState == Player.STATE_ENDED) {
                if (_state.value.all) finish(player) else _state.update { it.copy(playing = false) }
            }
        }
    }

    /** Past the last name: the Namavali is complete (listening starts again from the first name). */
    private fun finish(player: Player) {
        val n = _state.value.n
        player.pause()
        droneOn(false)
        val complete = n != null && n == lastRecorded
        prefs.edit().apply { if (complete) remove(KEY_AT) else putInt(KEY_AT, n ?: 0) }.apply()
        _state.update {
            it.copy(playing = false, message = if (complete) "The Namavali is complete. Swamiye Saranam Ayyappa." else null)
        }
    }
}

/** Keeps the Namavali playing in the background, with the lock screen's and earphones' controls and a notification. */
@OptIn(UnstableApi::class)
class NamavaliService : MediaSessionService() {
    private var session: androidx.media3.session.MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        session = androidx.media3.session.MediaSession.Builder(this, NamavaliPlayer.voice(this)).build()
    }

    override fun onGetSession(controllerInfo: androidx.media3.session.MediaSession.ControllerInfo) = session

    /** Swiped away from the recent apps while not playing: stop. */
    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
