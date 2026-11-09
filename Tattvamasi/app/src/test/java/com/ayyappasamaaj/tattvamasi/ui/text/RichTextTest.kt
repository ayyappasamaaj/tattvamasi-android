package com.ayyappasamaaj.tattvamasi.ui.text

import org.junit.Assert.assertEquals
import org.junit.Test

class RichTextTest {
    @Test
    fun paragraphsHeadingsAndInlineStyles() {
        val blocks = parseRichText(
            "<h3><b>Grand Irumudi Puja</b><br>Saturday, December 26</h3>" +
                "<p>Starts at <b>5:30 AM</b> with <i>Sankalpam</i> &amp; <u>Puja</u>.</p>",
        )
        assertEquals(
            listOf(
                TextBlock.Heading(3, listOf(Run("Grand Irumudi Puja", bold = true), Run("\nSaturday, December 26"))),
                TextBlock.Paragraph(listOf(
                    Run("Starts at "), Run("5:30 AM", bold = true), Run(" with "), Run("Sankalpam", italic = true),
                    Run(" & "), Run("Puja", underline = true), Run("."),
                )),
            ),
            blocks,
        )
    }

    @Test
    fun listsAndLinks() {
        val blocks = parseRichText(
            "<p>Bring:</p><ul><li>Rice</li><li>See <a href=\"https://ayyappasamaaj.org/vratam/#food\">food</a></li></ul>" +
                "<ol><li>One</li><li>Two</li></ol><p><a href=\"javascript:x\">bad</a> <a href=\"mailto:info@ayyappasamaaj.org\">mail</a></p>",
        )
        assertEquals(TextBlock.Paragraph(listOf(Run("Bring:"))), blocks[0])
        assertEquals(
            TextBlock.ListBlock(false, listOf(
                listOf(Run("Rice")),
                listOf(Run("See "), Run("food", link = "https://ayyappasamaaj.org/vratam/#food")),
            )),
            blocks[1],
        )
        assertEquals(TextBlock.ListBlock(true, listOf(listOf(Run("One")), listOf(Run("Two")))), blocks[2])
        // Only web and mail links; anything else stays plain text
        assertEquals(TextBlock.Paragraph(listOf(Run("bad "), Run("mail", link = "mailto:info@ayyappasamaaj.org"))), blocks[3])
    }

    @Test
    fun looseTextSpacesAndEntities() {
        assertEquals(listOf(TextBlock.Paragraph(listOf(Run("Kumkum")))), parseRichText("Kumkum"))
        assertEquals(listOf(TextBlock.Paragraph(listOf(Run("a b \"c\" 'd' <e>")))),
            parseRichText("<p>  a \n  b &quot;c&quot; &#39;d&#39; &lt;e&gt; </p>"))
        assertEquals(emptyList<TextBlock>(), parseRichText("<p> </p><ul></ul>"))
    }
}
