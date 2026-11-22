package com.ayyappasamaaj.tattvamasi.ui.shruti

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.ayyappasamaaj.tattvamasi.R
import com.ayyappasamaaj.tattvamasi.ui.yatra.NamavaliPlayer
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/*
 * The tanpura (shruti) player, one for the app, to sing along with while reading the publications (the maintainer's
 * request, Nov 22, 2026; the canvas prototype https://claude.ai/artifact/8gcAHq92Jxy8nzg86EZdGC#play-Shruti). A
 * continuous drone in any of 12 keys (A to G#, each with its kattai), Sa–Pa or Sa–Ma, looped from the bundled files
 * res/raw/tanpura_<key>_<pa|ma>.m4a. The key, the tuning and the volume are remembered on the phone. It keeps playing
 * with the screen off and in the background (ShrutiService), and it and the Namavali pause each other: starting one
 * pauses the other (the maintainer's decision).
 */

/** A key (the Sa) for the tanpura: its note, its kattai, its files' part of the name, and how a screen reader says it. */
data class ShrutiKey(val note: String, val kattai: String, val file: String, val spoken: String, val spokenKattai: String)

data class ShrutiSettings(val key: ShrutiKey, val tuning: String, val volume: Float)

object Shruti {
    const val PA = "pa"
    const val MA = "ma"
    const val VOLUME = 0.8f

    /** In the order the sheet shows them (the maintainer's): A to G#. */
    val KEYS = listOf(
        ShrutiKey("A", "6", "a", "A", "six"),
        ShrutiKey("A#", "6½", "as", "A sharp", "six and a half"),
        ShrutiKey("B", "7", "b", "B", "seven"),
        ShrutiKey("C", "1", "c", "C", "one"),
        ShrutiKey("C#", "1½", "cs", "C sharp", "one and a half"),
        ShrutiKey("D", "2", "d", "D", "two"),
        ShrutiKey("D#", "2½", "ds", "D sharp", "two and a half"),
        ShrutiKey("E", "3", "e", "E", "three"),
        ShrutiKey("F", "4", "f", "F", "four"),
        ShrutiKey("F#", "4½", "fs", "F sharp", "four and a half"),
        ShrutiKey("G", "5", "g", "G", "five"),
        ShrutiKey("G#", "5½", "gs", "G sharp", "five and a half"),
    )
    val DEFAULT_KEY = KEYS.first { it.note == "C" }

    fun key(note: String?): ShrutiKey = KEYS.firstOrNull { it.note == note } ?: DEFAULT_KEY

    /** The loop's resource name: "tanpura_cs_pa" for C# Sa–Pa. */
    fun fileName(key: ShrutiKey, tuning: String): String = "tanpura_${key.file}_${if (tuning == MA) MA else PA}"

    /** "Sa–Pa" or "Sa–Ma". */
    fun tuningName(tuning: String): String = if (tuning == MA) "Sa–Ma" else "Sa–Pa"

    /** The badge and the bar: "C# · 1½ · Pa". */
    fun label(key: ShrutiKey, tuning: String): String = "${key.note} · ${key.kattai} · ${if (tuning == MA) "Ma" else "Pa"}"

    /** The notification and lock screen: "Tanpura · C# (1½) · Sa–Pa". */
    fun title(key: ShrutiKey, tuning: String): String = "Tanpura · ${key.note} (${key.kattai}) · ${tuningName(tuning)}"

    /** "C sharp, kattai one and a half". */
    fun spoken(key: ShrutiKey): String = "${key.spoken}, kattai ${key.spokenKattai}"

    /** What the phone remembered, each value checked (anything unknown falls back to C, Sa–Pa, 80%). */
    fun settings(note: String?, tuning: String?, volume: Float?): ShrutiSettings = ShrutiSettings(
        key(note),
        if (tuning == MA) MA else PA,
        volume?.takeIf { !it.isNaN() }?.coerceIn(0f, 1f) ?: VOLUME,
    )

    /** The 24 loops (named so the build keeps them all). */
    val RAW = mapOf(
        "tanpura_a_pa" to R.raw.tanpura_a_pa, "tanpura_a_ma" to R.raw.tanpura_a_ma,
        "tanpura_as_pa" to R.raw.tanpura_as_pa, "tanpura_as_ma" to R.raw.tanpura_as_ma,
        "tanpura_b_pa" to R.raw.tanpura_b_pa, "tanpura_b_ma" to R.raw.tanpura_b_ma,
        "tanpura_c_pa" to R.raw.tanpura_c_pa, "tanpura_c_ma" to R.raw.tanpura_c_ma,
        "tanpura_cs_pa" to R.raw.tanpura_cs_pa, "tanpura_cs_ma" to R.raw.tanpura_cs_ma,
        "tanpura_d_pa" to R.raw.tanpura_d_pa, "tanpura_d_ma" to R.raw.tanpura_d_ma,
        "tanpura_ds_pa" to R.raw.tanpura_ds_pa, "tanpura_ds_ma" to R.raw.tanpura_ds_ma,
        "tanpura_e_pa" to R.raw.tanpura_e_pa, "tanpura_e_ma" to R.raw.tanpura_e_ma,
        "tanpura_f_pa" to R.raw.tanpura_f_pa, "tanpura_f_ma" to R.raw.tanpura_f_ma,
        "tanpura_fs_pa" to R.raw.tanpura_fs_pa, "tanpura_fs_ma" to R.raw.tanpura_fs_ma,
        "tanpura_g_pa" to R.raw.tanpura_g_pa, "tanpura_g_ma" to R.raw.tanpura_g_ma,
        "tanpura_gs_pa" to R.raw.tanpura_gs_pa, "tanpura_gs_ma" to R.raw.tanpura_gs_ma,
    )
}

data class ShrutiState(
    val key: ShrutiKey = Shruti.DEFAULT_KEY,
    val tuning: String = Shruti.PA,
    val volume: Float = Shruti.VOLUME,
    /** Sounding. */
    val playing: Boolean = false,
    /** The tanpura bar is shown (from the first Play until ×). */
    val shown: Boolean = false,
    /** The Tanpura sheet is open. */
    val sheet: Boolean = false,
)

// Holds the application's context (not an activity's) and the player, for the app's whole run: no leak
@SuppressLint("StaticFieldLeak")
@OptIn(UnstableApi::class)
object ShrutiPlayer {
    private const val PREFS = "shruti-player"
    private const val KEY_NOTE = "key"
    private const val KEY_TUNING = "tuning"
    private const val KEY_VOLUME = "volume"

    private val _state = MutableStateFlow(ShrutiState())
    val state: StateFlow<ShrutiState> = _state.asStateFlow()

    private var player: ExoPlayer? = null
    private var loaded: String? = null
    private var controller: ListenableFuture<MediaController>? = null
    private lateinit var app: Context

    private val prefs get() = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        val s = Shruti.settings(prefs.getString(KEY_NOTE, null), prefs.getString(KEY_TUNING, null),
            if (prefs.contains(KEY_VOLUME)) prefs.getFloat(KEY_VOLUME, Shruti.VOLUME) else null)
        _state.update { it.copy(key = s.key, tuning = s.tuning, volume = s.volume) }
    }

    /** The drone, for the media session (notification, lock screen, earphones). */
    fun player(context: Context): ExoPlayer {
        init(context)
        player?.let { return it }
        return ExoPlayer.Builder(app)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true,  // pauses for a call or another app's sound
            )
            .setHandleAudioBecomingNoisy(true)  // earphones unplugged: pause
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build().also {
                it.repeatMode = Player.REPEAT_MODE_ONE
                it.volume = _state.value.volume
                it.addListener(listener)
                player = it
            }
    }

    /** Puts the chosen key and tuning's loop in the player (straight away, if it was playing). */
    private fun load(p: ExoPlayer) {
        val s = _state.value
        val name = Shruti.fileName(s.key, s.tuning)
        if (loaded == name) return
        val raw = Shruti.RAW[name] ?: return
        p.setMediaItem(MediaItem.Builder()
            .setMediaId(name)
            .setUri(RawResourceDataSource.buildRawResourceUri(raw))
            .setMediaMetadata(MediaMetadata.Builder().setTitle(Shruti.title(s.key, s.tuning)).setArtist("Tattvamasi").build())
            .build())
        p.prepare()
        loaded = name
    }

    /** Starts the media session's service (bound by a controller), so the tanpura goes on in the background. */
    private fun connect() {
        if (controller != null) return
        controller = MediaController.Builder(app, SessionToken(app, ComponentName(app, ShrutiService::class.java))).buildAsync()
    }

    fun play(context: Context) {
        val p = player(context)
        NamavaliPlayer.pause()  // the two pause each other
        connect()
        load(p)
        p.play()
        _state.update { it.copy(playing = true, shown = true) }
    }

    /** Pauses (the bar stays). Safe before anything played: the Namavali calls it when it starts. */
    fun pause() {
        player?.pause()
        _state.update { it.copy(playing = false) }
    }

    fun toggle(context: Context) = if (_state.value.playing) pause() else play(context)

    /** × in the bar: stops and hides the bar. */
    fun stop() {
        player?.pause()
        _state.update { it.copy(playing = false, shown = false) }
    }

    /** A key from the sheet: plays it (straight away, or starting if it wasn't playing). */
    fun choose(context: Context, key: ShrutiKey) {
        init(context)
        prefs.edit().putString(KEY_NOTE, key.note).apply()
        _state.update { it.copy(key = key) }
        player?.let(::load)
        if (!_state.value.playing) play(context)
    }

    fun tuning(context: Context, tuning: String) {
        init(context)
        val t = if (tuning == Shruti.MA) Shruti.MA else Shruti.PA
        prefs.edit().putString(KEY_TUNING, t).apply()
        _state.update { it.copy(tuning = t) }
        player?.let(::load)
    }

    fun volume(context: Context, volume: Float) {
        init(context)
        val v = volume.coerceIn(0f, 1f)
        prefs.edit().putFloat(KEY_VOLUME, v).apply()
        player?.volume = v
        _state.update { it.copy(volume = v) }
    }

    fun openSheet() = _state.update { it.copy(sheet = true) }
    fun closeSheet() = _state.update { it.copy(sheet = false) }

    private val listener = object : Player.Listener {
        // Played or paused from the notification, earphones, a call: the screen follows; playing pauses the Namavali
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (playWhenReady) {
                NamavaliPlayer.pause()
                _state.update { it.copy(playing = true, shown = true) }
            } else {
                _state.update { it.copy(playing = false) }
            }
        }
    }
}

/** Keeps the tanpura playing in the background, with its notification and the lock screen's and earphones' controls. */
@OptIn(UnstableApi::class)
class ShrutiService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        // Its own id: the Namavali has the app's other session
        session = MediaSession.Builder(this, ShrutiPlayer.player(this)).setId("tanpura").build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    /** Swiped away from the recent apps while not playing: stop. */
    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady) stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
