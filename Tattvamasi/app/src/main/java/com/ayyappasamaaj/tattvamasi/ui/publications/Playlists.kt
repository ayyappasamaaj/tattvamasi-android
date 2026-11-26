package com.ayyappasamaaj.tattvamasi.ui.publications

import com.ayyappasamaaj.tattvamasi.ui.account.AccountApi
import com.ayyappasamaaj.tattvamasi.ui.account.AccountStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/*
 * The account's playlists, the website's (bhajans/playlists.py there): the same ones the website's PDF viewer saves and
 * the dashboard's My playlists lists. Up to 5 playlists of 25 PDFs each (the website says how many: max_playlists,
 * max_pdfs); a new one is named "Playlist 1", "Playlist 2"... by the website. Signed in only (the X-Session-Token of
 * ui/account); signed out, the phone's own Saved list (SavedTexts.kt) stays as it is.
 *
 *   GET    /api/bhajans/playlists/        {"playlists": [...], "max_playlists": 5, "max_pdfs": 25}
 *   POST   /api/bhajans/playlists/        {"name"?, "pdfs"?: [pdf id, ...]}  -> the new playlist (201)
 *   PATCH  /api/bhajans/playlists/<id>/   {"name"?, "pdfs"?}                 -> the playlist
 *   DELETE /api/bhajans/playlists/<id>/   -> 204
 *
 * A playlist holds the website's PDF ids, one language version each. The app knows a PDF by its version's pageUrl in
 * `/publications` (https://ayyappasamaaj.org/bhajans/pdf/<id>/), so a playlist's PDFs are found there to open in the
 * reader. As on the iOS app (Playlists.swift). The plain functions are unit-tested (PlaylistsTest).
 */

/** A PDF in a playlist: its website id, title, and its language when the text has more than one ("" otherwise). */
data class PlaylistPdf(val id: Int, val title: String, val language: String) {
    /** "Aragarogara Swami (Tamil)", as the website lists it */
    val label: String get() = if (language.isBlank()) title else "$title ($language)"
}

data class Playlist(val id: Int, val name: String, val pdfs: List<PlaylistPdf>, val shareUrl: String) {
    val pdfIds: List<Int> get() = pdfs.map { it.id }
}

data class Playlists(val playlists: List<Playlist>, val maxPlaylists: Int, val maxPdfs: Int) {
    val full: Boolean get() = playlists.size >= maxPlaylists
}

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

fun parsePlaylist(json: JSONObject): Playlist? {
    val id = json.optInt("id", -1).takeIf { it >= 0 } ?: return null
    val pdfs = json.optJSONArray("pdfs").objects().mapNotNull { pdf ->
        val pdfId = pdf.optInt("id", -1).takeIf { it >= 0 } ?: return@mapNotNull null
        PlaylistPdf(pdfId, pdf.optString("title"), pdf.optString("language"))
    }
    return Playlist(id, json.optString("name"), pdfs, json.optString("share_url"))
}

/** GET /api/bhajans/playlists/; null when it isn't the website's answer. */
fun parsePlaylists(body: String): Playlists? = runCatching {
    val json = JSONObject(body)
    val list = json.getJSONArray("playlists")
    Playlists(list.objects().mapNotNull(::parsePlaylist), json.optInt("max_playlists", 5), json.optInt("max_pdfs", 25))
}.getOrNull()

/** The website's reason (its "error"), or ours when it gave none. */
fun playlistError(code: Int, body: String, fallback: String): String {
    if (code == 0) return "Couldn't reach ayyappasamaaj.org. Check your connection and try again."
    val error = runCatching { JSONObject(body).optString("error") }.getOrNull().orEmpty()
    return error.ifBlank { fallback }
}

/** The website's id of a PDF, from its page's address: ".../bhajans/pdf/27/" -> 27. */
fun pdfIdOf(pageUrl: String): Int? = Regex("/bhajans/pdf/(\\d+)/?").find(pageUrl)?.groupValues?.get(1)?.toIntOrNull()

/** Where a playlist's PDF is in the app: its text, category and the language to open. */
data class PlaylistEntry(val pdf: PlaylistPdf, val item: ReadingItem?)

/** A playlist's PDFs, each found in `/publications` (null item: not in the app, e.g. a text the website hides). */
fun resolvePlaylist(playlist: Playlist, sections: List<PublicationSection>): List<PlaylistEntry> {
    val found = mutableMapOf<Int, ReadingItem>()
    sections.forEach { section ->
        section.categories.forEach { category ->
            category.texts.forEach { text ->
                text.versions.forEach { version ->
                    pdfIdOf(version.pageUrl)?.let { id ->
                        found.putIfAbsent(id, ReadingItem(text, category.name, version.language, category.slug))
                    }
                }
            }
        }
    }
    return playlist.pdfs.map { PlaylistEntry(it, found[it.id]) }
}

/** The PDF ids of the phone's Saved texts (newest first), in each text's reading language, for a new playlist. */
fun savedPdfIds(saved: List<PublicationHit>, preferred: String, max: Int): List<Int> =
    saved.mapNotNull { hit ->
        val language = openingLanguage(hit.text, preferred)
        val version = hit.text.versions.firstOrNull { it.language == language } ?: hit.text.versions.firstOrNull()
        version?.let { pdfIdOf(it.pageUrl) }
    }.distinct().take(max)

/** The ids with one moved from [from] to [to] (Up / Down). */
fun movedPdf(ids: List<Int>, from: Int, to: Int): List<Int> {
    if (from !in ids.indices || to !in ids.indices || from == to) return ids
    return ids.toMutableList().apply { add(to, removeAt(from)) }
}

/** What adding a PDF does: added at the end, already there, or the playlist is full. */
sealed interface AddToPlaylist {
    data class Added(val ids: List<Int>) : AddToPlaylist
    data object AlreadyThere : AddToPlaylist
    data object Full : AddToPlaylist
}

fun addingPdf(ids: List<Int>, id: Int, max: Int): AddToPlaylist = when {
    id in ids -> AddToPlaylist.AlreadyThere
    ids.size >= max -> AddToPlaylist.Full
    else -> AddToPlaylist.Added(ids + id)
}

/** The reader playing a playlist: its name, its texts that are in the app, and the one open. */
data class PlaylistPlay(val name: String, val items: List<ReadingItem>, val at: Int) {
    val previous: ReadingItem? get() = items.getOrNull(at - 1)?.withPlay(copy(at = at - 1))
    val next: ReadingItem? get() = items.getOrNull(at + 1)?.withPlay(copy(at = at + 1))
}

fun ReadingItem.withPlay(play: PlaylistPlay) = copy(playlist = play)

/** Play a playlist from its [start]th PDF (its texts not in the app are skipped); null when there's nothing to play. */
fun playFrom(playlist: Playlist, entries: List<PlaylistEntry>, start: Int = 0): ReadingItem? {
    val items = entries.mapNotNull { it.item }
    if (items.isEmpty()) return null
    val at = entries.take(start.coerceAtLeast(0)).count { it.item != null }.coerceAtMost(items.lastIndex)
    return items[at].withPlay(PlaylistPlay(playlist.name, items, at))
}

/** The account's playlists while signed in: loaded when Playlists opens, changed through the website. */
object PlaylistStore {
    data class State(
        val data: Playlists? = null,
        val loading: Boolean = false,
        val failed: Boolean = false,
        val message: String? = null,   // the last change's problem, from the website
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private const val API = "/api/bhajans/playlists/"

    private fun token(): String? = AccountStore.sessionToken

    fun clearMessage() = _state.update { it.copy(message = null) }

    /** The website's answer was 401: signed out elsewhere. */
    private fun ended() {
        _state.value = State()
        AccountStore.ended()
    }

    suspend fun load() {
        val token = token() ?: return run { _state.value = State() }
        _state.update { it.copy(loading = true, failed = false) }
        val reply = try { AccountApi.call("GET", API, token) } finally { _state.update { it.copy(loading = false) } }
        if (reply.code == 401) return ended()
        val data = if (reply.code == 200) parsePlaylists(reply.body) else null
        _state.update { it.copy(data = data ?: it.data, failed = data == null) }
    }

    private fun replace(playlist: Playlist) = _state.update { s ->
        s.copy(data = s.data?.copy(playlists = s.data.playlists.map { if (it.id == playlist.id) playlist else it }))
    }

    /** A new playlist ([name] blank: the website names it), with these PDFs. The new one, or null (message set). */
    suspend fun create(name: String = "", pdfs: List<Int> = emptyList()): Playlist? {
        val token = token() ?: return null
        val body = JSONObject().put("pdfs", JSONArray(pdfs))
        if (name.isNotBlank()) body.put("name", name.trim())
        val reply = AccountApi.call("POST", API, token, body)
        if (reply.code == 401) return null.also { ended() }
        val playlist = if (reply.code == 201) runCatching { parsePlaylist(JSONObject(reply.body)) }.getOrNull() else null
        if (playlist == null) {
            _state.update { it.copy(message = playlistError(reply.code, reply.body, "The playlist couldn't be made. Please try again.")) }
            return null
        }
        _state.update { s -> s.copy(data = s.data?.copy(playlists = s.data.playlists + playlist), message = null) }
        return playlist
    }

    private suspend fun patch(id: Int, body: JSONObject, fallback: String): Playlist? {
        val token = token() ?: return null
        val reply = AccountApi.call("PATCH", "$API$id/", token, body)
        if (reply.code == 401) return null.also { ended() }
        val playlist = if (reply.code == 200) runCatching { parsePlaylist(JSONObject(reply.body)) }.getOrNull() else null
        if (playlist == null) {
            _state.update { it.copy(message = playlistError(reply.code, reply.body, fallback)) }
            if (reply.code == 404) load()  // deleted elsewhere (the website, the other app)
            return null
        }
        replace(playlist)
        _state.update { it.copy(message = null) }
        return playlist
    }

    suspend fun rename(id: Int, name: String): Playlist? =
        patch(id, JSONObject().put("name", name), "The playlist couldn't be renamed. Please try again.")

    /** The PDFs in this order (reordered, one removed or added). */
    suspend fun setPdfs(id: Int, pdfs: List<Int>): Playlist? =
        patch(id, JSONObject().put("pdfs", JSONArray(pdfs)), "The playlist couldn't be changed. Please try again.")

    suspend fun delete(id: Int): Boolean {
        val token = token() ?: return false
        val reply = AccountApi.call("DELETE", "$API$id/", token)
        if (reply.code == 401) return false.also { ended() }
        if (reply.code != 204 && reply.code != 404) {
            _state.update { it.copy(message = playlistError(reply.code, reply.body, "The playlist couldn't be deleted. Please try again.")) }
            return false
        }
        _state.update { s -> s.copy(data = s.data?.copy(playlists = s.data.playlists.filterNot { it.id == id }), message = null) }
        return true
    }
}
