package com.ayyappasamaaj.tattvamasi.ui.about

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.ayyappasamaaj.tattvamasi.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class BoardMember(val name: String, val role: String) {
    /** "Yogesh Sharma" -> "YS", for the round badge. */
    val initials: String get() = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }
}

/** A year's board; [earlier] is the earlier years' boards, newest first (only on this year's). */
data class Board(val year: Int, val members: List<BoardMember>, val earlier: List<Board> = emptyList())

/** Shown until the website's list loads (or if it can't be reached): the 2026 board, as in the iOS app. */
val builtInBoard = Board(
    2026,
    listOf(
        BoardMember("Yogesh Sharma", "Chairperson"),
        BoardMember("Parthasarathy Anandaraj", "President"),
        BoardMember("Nirmala Pelluri", "Secretary"),
        BoardMember("Venkataraman Balasubramaniam", "Treasurer"),
        BoardMember("Murali Natarajan", "Board Member"),
        BoardMember("Anupama Ganapathy", "Board Member"),
        BoardMember("Nagaraj Uddhandi", "Board Member"),
        BoardMember("Gokula Rangarajan", "Board Member"),
        BoardMember("Nathan Ganesan", "Board Member"),
        BoardMember("Rajesh Chamarthy", "Board Member"),
        BoardMember("Ramakrishnan Ganesan", "BOD Advisor"),
        BoardMember("Prasad Ramakrishnan", "Co-founder"),
    ),
)

private fun parseYear(root: JSONObject): Board? {
    val list = root.optJSONArray("members") ?: return null
    val members = (0 until list.length()).mapNotNull { list.optJSONObject(it) }
        .map { BoardMember(it.optString("name"), it.optString("role")) }
        .filter { it.name.isNotBlank() }
    return Board(root.getInt("year"), members).takeIf { members.isNotEmpty() }
}

/**
 * The website's /board/app.json ({"year": 2026, "members": [{"name", "role"}], "earlier": [{"year", "members"}]}),
 * or null if it isn't usable.
 */
fun parseBoard(json: String): Board? = runCatching {
    val root = JSONObject(json)
    val earlier = root.optJSONArray("earlier")
    val years = (0 until (earlier?.length() ?: 0)).mapNotNull { index ->
        earlier!!.optJSONObject(index)?.let { runCatching { parseYear(it) }.getOrNull() }
    }.sortedByDescending { it.year }
    parseYear(root)?.copy(earlier = years)
}.getOrNull()

/** This year's board from the website (ayyappasamaaj.org/board/app.json), loaded once per app run. */
object BoardStore {
    private val _state = mutableStateOf(builtInBoard)
    val state: State<Board> = _state
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val board = withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL("https://ayyappasamaaj.org/board/app.json").openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                try {
                    if (connection.responseCode == 200) parseBoard(connection.inputStream.reader().readText()) else null
                } finally {
                    connection.disconnect()
                }
            }.onFailure { AppLog.w("BoardStore", "board: " + it.message) }.getOrNull()
        }
        if (board != null) {
            _state.value = board
            loaded = true
        }
    }
}
