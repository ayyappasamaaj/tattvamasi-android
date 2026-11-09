package com.ayyappasamaaj.tattvamasi.ui.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ayyappasamaaj.tattvamasi.ui.openWebPage
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors

/*
 * The website's text for the apps (events' details, the guides) comes in a small HTML subset (website:
 * events/firebase.py app_html): <p>, <br>, <b>, <i>, <u>, <ul>/<ol>/<li>, <h3>/<h4> and <a href>. It's turned
 * into native text here: blocks of styled runs, then Compose text with links that open in the in-app browser.
 */

/** A run of text with its style; [link] is the address it goes to, if it's a link. */
data class Run(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val link: String? = null,
)

sealed interface TextBlock {
    data class Heading(val level: Int, val runs: List<Run>) : TextBlock
    data class Paragraph(val runs: List<Run>) : TextBlock
    data class ListBlock(val ordered: Boolean, val items: List<List<Run>>) : TextBlock
}

private val TAG = Regex("""<(/?)([a-zA-Z0-9]+)([^>]*)>""")
private val HREF = Regex("""href\s*=\s*"([^"]*)"""")

private fun unescape(text: String): String = text
    .replace(Regex("&#(\\d+);")) { it.groupValues[1].toInt().toChar().toString() }
    .replace(Regex("&#x([0-9a-fA-F]+);")) { it.groupValues[1].toInt(16).toChar().toString() }
    .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
    .replace("&nbsp;", " ").replace("&amp;", "&")

/** The subset's HTML as blocks. Anything outside a block becomes a paragraph; unknown tags are ignored. */
fun parseRichText(html: String): List<TextBlock> {
    val blocks = mutableListOf<TextBlock>()
    var runs = mutableListOf<Run>()
    var heading = 0
    var listOrdered: Boolean? = null
    var items = mutableListOf<List<Run>>()
    var bold = 0
    var italic = 0
    var underline = 0
    val links = ArrayDeque<String?>()

    fun addText(text: String) {
        if (text.isEmpty()) return
        val run = Run(text, bold > 0, italic > 0, underline > 0, links.lastOrNull())
        val last = runs.lastOrNull()
        if (last != null && last.copy(text = "") == run.copy(text = "")) {
            runs[runs.lastIndex] = last.copy(text = last.text + text)
        } else {
            runs += run
        }
    }

    fun tidy(list: List<Run>): List<Run> {
        // Collapse spaces, and trim the ends of the block (not of a line break inside it)
        // (the page's own line ends are already spaces: a "\n" here is a <br>)
        val out = list.map { it.copy(text = it.text.replace(Regex(" {2,}"), " ").replace(Regex(" ?\n ?"), "\n")) }
            .toMutableList()
        while (out.isNotEmpty() && out.first().text.isBlank() && !out.first().text.contains('\n')) out.removeAt(0)
        while (out.isNotEmpty() && out.last().text.isBlank()) out.removeAt(out.lastIndex)
        if (out.isNotEmpty()) {
            out[0] = out[0].copy(text = out[0].text.trimStart(' '))
            out[out.lastIndex] = out.last().copy(text = out.last().text.trimEnd(' ', '\n'))
        }
        return out.filter { it.text.isNotEmpty() }
    }

    fun endBlock() {
        val done = tidy(runs)
        runs = mutableListOf()
        if (done.isEmpty()) return
        when {
            listOrdered != null -> items += done
            heading > 0 -> blocks += TextBlock.Heading(heading, done)
            else -> blocks += TextBlock.Paragraph(done)
        }
    }

    fun endList() {
        endBlock()
        if (listOrdered != null && items.isNotEmpty()) blocks += TextBlock.ListBlock(listOrdered!!, items)
        listOrdered = null
        items = mutableListOf()
    }

    // The page's own line ends and tabs are spaces; only <br> makes a new line
    fun source(text: String) = addText(unescape(text).replace(Regex("[\\t\\r\\n]"), " "))

    var at = 0
    for (match in TAG.findAll(html)) {
        source(html.substring(at, match.range.first))
        at = match.range.last + 1
        val closing = match.groupValues[1] == "/"
        when (val tag = match.groupValues[2].lowercase()) {
            "br" -> addText("\n")
            "b", "strong" -> bold += if (closing) -1 else 1
            "i", "em" -> italic += if (closing) -1 else 1
            "u" -> underline += if (closing) -1 else 1
            "a" -> if (closing) links.removeLastOrNull() else links.addLast(HREF.find(match.groupValues[3])
                ?.groupValues?.get(1)?.let(::unescape)?.takeIf { it.startsWith("http") || it.startsWith("mailto:") })
            "p" -> endBlock()
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                endBlock()
                heading = if (closing) 0 else if (tag <= "h3") 3 else 4
            }
            "ul", "ol" -> if (closing) endList() else {
                endList()
                listOrdered = tag == "ol"
            }
            "li" -> endBlock()
        }
        bold = bold.coerceAtLeast(0); italic = italic.coerceAtLeast(0); underline = underline.coerceAtLeast(0)
    }
    source(html.substring(at))
    endList()
    return blocks
}

/** The runs as Compose text: styles, and links that open in the in-app browser. */
@Composable
private fun annotated(runs: List<Run>): AnnotatedString {
    val context = LocalContext.current
    return remember(runs) {
        buildAnnotatedString {
            for (run in runs) {
                val style = SpanStyle(
                    fontWeight = if (run.bold) FontWeight.Bold else null,
                    fontStyle = if (run.italic) FontStyle.Italic else null,
                    textDecoration = if (run.underline) TextDecoration.Underline else null,
                )
                if (run.link != null) {
                    val url = run.link
                    withLink(LinkAnnotation.Clickable(url, TextLinkStyles(SpanStyle(
                        color = AppColors.clayDark, textDecoration = TextDecoration.Underline))) {
                        openWebPage(context, url)
                    }) { withStyle(style) { append(run.text) } }
                } else {
                    withStyle(style) { append(run.text) }
                }
            }
        }
    }
}

/** The website's text, natively. */
@Composable
fun RichText(html: String, modifier: Modifier = Modifier) {
    val blocks = remember(html) { parseRichText(html) }
    val body = MaterialTheme.typography.bodyLarge
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        for (block in blocks) {
            when (block) {
                is TextBlock.Heading -> Text(
                    annotated(block.runs),
                    style = if (block.level <= 3) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    color = AppColors.ink,
                    modifier = Modifier.padding(top = 4.dp).semantics { heading() },
                )
                is TextBlock.Paragraph -> Text(annotated(block.runs), style = body, color = AppColors.ink)
                is TextBlock.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    block.items.forEachIndexed { index, item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (block.ordered) "${index + 1}." else "•", style = body, color = AppColors.clay,
                                modifier = Modifier.widthIn(min = 18.dp))
                            Text(annotated(item), style = body, color = AppColors.ink)
                        }
                    }
                }
            }
        }
    }
}
