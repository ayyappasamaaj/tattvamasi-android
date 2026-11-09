package com.ayyappasamaaj.tattvamasi.ui.about

import com.ayyappasamaaj.tattvamasi.ui.sabarimala.guides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AboutTest {
    @Test
    fun readsTheWebsitesBoardFeed() {
        val board = parseBoard(
            """{"year": 2027, "members": [{"name": "Yogesh Sharma", "role": "Chairperson"},
               {"name": "", "role": "Nobody"}, {"name": "Murali Natarajan", "role": "Board Member"}]}""",
        )!!
        assertEquals(2027, board.year)
        assertEquals(listOf("Yogesh Sharma", "Murali Natarajan"), board.members.map { it.name })
        assertEquals("YS", board.members[0].initials)
    }

    @Test
    fun readsEarlierYearsNewestFirst() {
        val board = parseBoard(javaClass.classLoader!!.getResource("board-sample.json")!!.readText())!!
        assertEquals(2026, board.year)
        assertEquals(2025, board.earlier.first().year)
        assertEquals(board.earlier.map { it.year }.sortedDescending(), board.earlier.map { it.year })
        assertEquals(emptyList<Board>(), board.earlier.flatMap { it.earlier })
        // A year without members is left out; the rest still load
        val partial = parseBoard(
            """{"year": 2026, "members": [{"name": "A", "role": "B"}],
               "earlier": [{"year": 2024, "members": []}, {"year": 2023, "members": [{"name": "C", "role": "D"}]}, {"x": 1}]}""",
        )!!
        assertEquals(listOf(2023), partial.earlier.map { it.year })
    }

    @Test
    fun keepsTheBuiltInBoardWhenTheFeedIsUnusable() {
        assertNull(parseBoard("""{"year": 2027, "members": []}"""))
        assertNull(parseBoard("<html>error</html>"))
        assertEquals(2026, builtInBoard.year)
        assertEquals("Chairperson", builtInBoard.members.first().role)
    }

    @Test
    fun guidesHaveTheirFeedSlugsAndPages() {
        assertEquals(listOf("traditional-path", "vratam", "virtual-q", "house-puja"), guides.map { it.slug })
        assertEquals(
            listOf(
                "https://ayyappasamaaj.org/traditional-path/",
                "https://ayyappasamaaj.org/vratam/",
                "https://ayyappasamaaj.org/sabarimala-virtual-q/",
                "https://ayyappasamaaj.org/house-puja-bhajan/",
            ),
            guides.map { it.url },
        )
    }
}
