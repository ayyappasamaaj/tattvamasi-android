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
    fun keepsTheBuiltInBoardWhenTheFeedIsUnusable() {
        assertNull(parseBoard("""{"year": 2027, "members": []}"""))
        assertNull(parseBoard("<html>error</html>"))
        assertEquals(2026, builtInBoard.year)
        assertEquals("Chairperson", builtInBoard.members.first().role)
    }

    @Test
    fun guidesOpenTheWebsitesPages() {
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
