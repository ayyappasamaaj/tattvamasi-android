@file:OptIn(ExperimentalTextApi::class)

package com.ayyappasamaaj.tattvamasi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.R

/** The website's look (ayyappasamaaj.org): its colors, as on the iOS app. */
object AppColors {
    val cream = Color(0xFFF6F2EA)       // page background
    val ink = Color(0xFF2A2620)         // text
    val inkSoft = Color(0xFF4A4538)     // secondary text
    val muted = Color(0xFF6B6457)       // captions
    val forest = Color(0xFF1E2A22)      // dark green: main buttons, the next-event card
    val clay = Color(0xFFB4410F)        // accent: selected tab, icons
    val clayDark = Color(0xFF7C2D0B)    // links
    val saffron = Color(0xFFF2A65A)     // on dark green
    val claySoft = Color(0xFFFBEDE3)    // light accent background
    val line = Color(0xFFE3DCCD)        // dividers
    val lineStrong = Color(0xFFC9BFAE)  // field borders
    val card = Color.White
}

/**
 * Bricolage Grotesque for headings and Source Sans 3 for text, as on the website. Both ship unmodified as
 * variable fonts (licenses in Tattvamasi/licenses); each weight is a setting of the font's weight axis.
 * Sizes are in sp, so they follow the phone's font size setting.
 */
object AppFonts {
    private fun bricolage(weight: Int) = Font(
        R.font.bricolage_grotesque, FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

    private fun sourceSans(weight: Int) = Font(
        R.font.source_sans_3, FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

    val heading = FontFamily(bricolage(600), bricolage(700), bricolage(800))
    val body = FontFamily(sourceSans(400), sourceSans(600), sourceSans(700))
}

private val colors = lightColorScheme(
    primary = AppColors.forest,
    onPrimary = Color.White,
    secondary = AppColors.clay,
    onSecondary = Color.White,
    secondaryContainer = AppColors.claySoft,      // the selected item's pill in the navigation bar and rail
    onSecondaryContainer = AppColors.clayDark,
    tertiary = AppColors.saffron,
    background = AppColors.cream,
    onBackground = AppColors.ink,
    surface = AppColors.cream,
    onSurface = AppColors.ink,
    onSurfaceVariant = AppColors.inkSoft,
    surfaceContainer = Color.White,                // the navigation bar and rail
    outline = AppColors.lineStrong,
    outlineVariant = AppColors.line,
)

private fun heading(size: Int, weight: Int = 700) =
    TextStyle(fontFamily = AppFonts.heading, fontWeight = FontWeight(weight), fontSize = size.sp)

private fun body(size: Int, weight: Int = 400) =
    TextStyle(fontFamily = AppFonts.body, fontWeight = FontWeight(weight), fontSize = size.sp)

private val typography = Typography(
    displaySmall = heading(34),      // a screen's title: "Events"
    headlineMedium = heading(28),
    headlineSmall = heading(22),     // a section: "Bhajans"
    titleLarge = heading(20),
    titleMedium = heading(17),
    titleSmall = heading(15),
    bodyLarge = body(17),
    bodyMedium = body(15),
    bodySmall = body(13),
    labelLarge = body(16, 600),
    labelMedium = body(13, 700),     // the navigation bar's labels
    labelSmall = body(11, 700),
)

@Composable
fun TattvamasiTheme(content: @Composable () -> Unit) {
    // The website's colors rather than the phone's wallpaper colors (Material You): the app is the
    // Samaaj's, and looks the same on every phone.
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
