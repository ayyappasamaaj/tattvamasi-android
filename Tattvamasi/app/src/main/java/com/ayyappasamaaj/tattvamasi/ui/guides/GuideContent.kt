package com.ayyappasamaaj.tattvamasi.ui.guides

import org.json.JSONArray
import org.json.JSONObject

/*
 * The website's five pilgrimage guides, as its /guides/app.json sends them (website: pages/app_guides.py): each
 * guide's introduction, then its sections (the page's cards; cards inside cards are sub-sections), each with its
 * content as blocks in page order: text in the small HTML subset ui/text/RichText.kt reads, a picture, or a list to
 * tick off (House Puja's items).
 */

sealed interface GuideBlock {
    data class Text(val html: String) : GuideBlock
    data class Picture(val url: String, val alt: String) : GuideBlock
    data class Checklist(val items: List<CheckItem>) : GuideBlock
}

/** An item to tick off; [id] is the website's checkbox id (unique on the site), [image] its picture or "". */
data class CheckItem(val id: String, val text: String, val image: String)

/** A section of a guide; [id] is the website's #id for it (a link to ayyappasamaaj.org/vratam/#food opens "food"). */
data class GuideSection(
    val id: String,
    val title: String,
    val blocks: List<GuideBlock>,
    val sections: List<GuideSection>,
) {
    /** This section and every section inside it. */
    fun all(): List<GuideSection> = listOf(this) + sections.flatMap { it.all() }
}

data class GuideContent(
    val slug: String,
    val title: String,
    val url: String,
    val intro: List<GuideBlock>,
    val sections: List<GuideSection>,
) {
    /** The ids of the sections from the top of the guide down to [id] (empty if it isn't in this guide). */
    fun pathTo(id: String): List<String> {
        fun find(sections: List<GuideSection>, trail: List<String>): List<String>? {
            for (section in sections) {
                val here = trail + section.id
                if (section.id == id) return here
                find(section.sections, here)?.let { return it }
            }
            return null
        }
        return find(sections, emptyList()) ?: emptyList()
    }

    /** Every picture in the guide, to keep for offline reading. */
    val pictures: List<String>
        get() = (intro + sections.flatMap { it.all() }.flatMap { it.blocks })
            .flatMap { block ->
                when (block) {
                    is GuideBlock.Picture -> listOf(block.url)
                    is GuideBlock.Checklist -> block.items.map { it.image }.filter { it.isNotEmpty() }
                    is GuideBlock.Text -> emptyList()
                }
            }
}

private fun parseBlocks(list: JSONArray?): List<GuideBlock> =
    (0 until (list?.length() ?: 0)).mapNotNull { index ->
        val block = list!!.optJSONObject(index) ?: return@mapNotNull null
        when {
            block.has("image") -> block.optString("image").takeIf { it.startsWith("https://") }
                ?.let { GuideBlock.Picture(it, block.optString("alt")) }
            block.has("checklist") -> block.optJSONArray("checklist")?.let { list ->
                (0 until list.length()).mapNotNull { list.optJSONObject(it) }.map {
                    CheckItem(it.optString("id"), it.optString("text"), it.optString("image").takeIf { url -> url.startsWith("https://") } ?: "")
                }.filter { it.id.isNotBlank() && it.text.isNotBlank() }
            }?.takeIf { it.isNotEmpty() }?.let { GuideBlock.Checklist(it) }
            block.optString("html").isNotBlank() -> GuideBlock.Text(block.optString("html"))
            else -> null
        }
    }

private fun parseSections(list: JSONArray?): List<GuideSection> =
    (0 until (list?.length() ?: 0)).mapNotNull { index ->
        val section = list!!.optJSONObject(index) ?: return@mapNotNull null
        GuideSection(
            id = section.optString("id").ifBlank { section.optString("title") },
            title = section.optString("title"),
            blocks = parseBlocks(section.optJSONArray("blocks")),
            sections = parseSections(section.optJSONArray("sections")),
        ).takeIf { it.title.isNotBlank() && (it.blocks.isNotEmpty() || it.sections.isNotEmpty()) }
    }

/** The website's /guides/app.json, or null if it isn't usable (another version, or no guides in it). */
fun parseGuides(json: String): List<GuideContent>? = runCatching {
    val root = JSONObject(json)
    if (root.optInt("version") != 1) return@runCatching null
    val list = root.getJSONArray("guides")
    (0 until list.length()).map { list.getJSONObject(it) }.map {
        GuideContent(
            slug = it.getString("slug"),
            title = it.optString("title"),
            url = it.optString("url"),
            intro = parseBlocks(it.optJSONArray("intro")),
            sections = parseSections(it.optJSONArray("sections")),
        )
    }.filter { it.sections.isNotEmpty() }.takeIf { it.isNotEmpty() }
}.getOrNull()

/** Where a link inside the app goes when it's one of the guides: the guide, and the section if the link names one. */
data class GuideTarget(val slug: String, val section: String? = null) {
    /** "vratam#food", to keep in saved state. */
    fun encode(): String = if (section == null) slug else "$slug#$section"

    companion object {
        fun decode(text: String): GuideTarget =
            text.split('#', limit = 2).let { GuideTarget(it[0], it.getOrNull(1)?.takeIf(String::isNotBlank)) }
    }
}

private fun pagePath(url: String): String? =
    Regex("""^https?://(www\.)?ayyappasamaaj\.org(/[^?#]*)?""").find(url)
        ?.groupValues?.get(2)?.trimEnd('/')?.lowercase()

/** The guide (and section) a website link points to, or null for any other page. */
fun guideTarget(url: String, guides: List<GuideContent>): GuideTarget? {
    val path = pagePath(url) ?: return null
    val guide = guides.firstOrNull { pagePath(it.url) == path } ?: return null
    val section = url.substringAfter('#', "").takeIf { it.isNotBlank() }
    return GuideTarget(guide.slug, section?.takeIf { guide.pathTo(it).isNotEmpty() })
}
