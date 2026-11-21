@file:OptIn(ExperimentalTextApi::class)

package com.ayyappasamaaj.tattvamasi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.R

/** True while the app is drawn dark: the phone's setting, or the reader's choice (Appearance). */
internal val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
@ReadOnlyComposable
private fun pick(light: Long, dark: Long) = Color(if (LocalDarkTheme.current) dark else light)

/**
 * The website's look (ayyappasamaaj.org): its colors, as on the iOS app. Each has a light and a dark value; the
 * dark ones are the website's dark mode (approved Nov 20, 2026: https://claude.ai/artifact/B9qi6JgytFRXvu28HPeBV3,
 * _base.scss's dark-colours), every text color 4.5:1 or better.
 */
object AppColors {
    val cream: Color @Composable @ReadOnlyComposable get() = pick(0xFFF6F2EA, 0xFF17140F)       // page background
    val ink: Color @Composable @ReadOnlyComposable get() = pick(0xFF2A2620, 0xFFF1EBDF)         // text
    val inkSoft: Color @Composable @ReadOnlyComposable get() = pick(0xFF4A4538, 0xFFD2C8B8)     // secondary text
    val muted: Color @Composable @ReadOnlyComposable get() = pick(0xFF6B6457, 0xFFA89E8E)       // captions
    val forest: Color @Composable @ReadOnlyComposable get() = pick(0xFF1E2A22, 0xFF22342A)      // dark green bands
    val clay: Color @Composable @ReadOnlyComposable get() = pick(0xFFB4410F, 0xFFE8875A)        // accent
    val clayDark: Color @Composable @ReadOnlyComposable get() = pick(0xFF7C2D0B, 0xFFF0A47A)    // links
    val saffron = Color(0xFFF2A65A)                                                            // on dark green
    val claySoft: Color @Composable @ReadOnlyComposable get() = pick(0xFFFBEDE3, 0xFF3A2418)    // light accent
    val line: Color @Composable @ReadOnlyComposable get() = pick(0xFFE3DCCD, 0xFF3A342B)        // dividers
    val lineStrong: Color @Composable @ReadOnlyComposable get() = pick(0xFFC9BFAE, 0xFF7A7060)  // field borders
    // Cards; also the text on clay and gold (white, and dark on dark mode's lighter clay)
    val card: Color @Composable @ReadOnlyComposable get() = pick(0xFFFFFFFF, 0xFF221E18)

    /** Text on the dark green: cream in both modes (the page's cream turns dark) */
    val onForest = Color(0xFFF6F2EA)
    /** The dark green as text or a line on a page or card: light in dark mode */
    val forestInk: Color @Composable @ReadOnlyComposable get() = pick(0xFF1E2A22, 0xFFF1EBDF)
    /** Main buttons: the dark green with white text; saffron with dark text in dark mode, as on the website */
    val button: Color @Composable @ReadOnlyComposable get() = pick(0xFF1E2A22, 0xFFF2A65A)
    val onButton: Color @Composable @ReadOnlyComposable get() = pick(0xFFFFFFFF, 0xFF1B1712)
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

private val lightColors = lightColorScheme(
    primary = Color(0xFF1E2A22),
    onPrimary = Color.White,
    secondary = Color(0xFFB4410F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBEDE3),      // the selected item's pill in the navigation bar and rail
    onSecondaryContainer = Color(0xFF7C2D0B),
    tertiary = Color(0xFFF2A65A),
    background = Color(0xFFF6F2EA),
    onBackground = Color(0xFF2A2620),
    surface = Color(0xFFF6F2EA),
    onSurface = Color(0xFF2A2620),
    onSurfaceVariant = Color(0xFF4A4538),
    surfaceContainer = Color.White,              // the navigation bar and rail
    outline = Color(0xFFC9BFAE),
    outlineVariant = Color(0xFFE3DCCD),
)

// The same roles in the website's dark colors; main buttons (primary) turn saffron with dark text
private val darkColors = darkColorScheme(
    primary = Color(0xFFF2A65A),
    onPrimary = Color(0xFF1B1712),
    secondary = Color(0xFFE8875A),
    onSecondary = Color(0xFF1B1712),
    secondaryContainer = Color(0xFF3A2418),
    onSecondaryContainer = Color(0xFFF0A47A),
    tertiary = Color(0xFFF2A65A),
    background = Color(0xFF17140F),
    onBackground = Color(0xFFF1EBDF),
    surface = Color(0xFF17140F),
    onSurface = Color(0xFFF1EBDF),
    onSurfaceVariant = Color(0xFFD2C8B8),
    surfaceContainer = Color(0xFF221E18),
    surfaceContainerHigh = Color(0xFF2C2720),
    surfaceContainerHighest = Color(0xFF2C2720),
    outline = Color(0xFF7A7060),
    outlineVariant = Color(0xFF3A342B),
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

/** Whether the app is dark now: the reader's choice under Contact (Appearance), else the phone's setting. */
@Composable
fun appIsDark(): Boolean = when (Appearance.choice) {
    Appearance.Choice.SYSTEM -> isSystemInDarkTheme()
    Appearance.Choice.LIGHT -> false
    Appearance.Choice.DARK -> true
}

@Composable
fun TattvamasiTheme(content: @Composable () -> Unit) {
    // The website's colors rather than the phone's wallpaper colors (Material You): the app is the
    // Samaaj's, and looks the same on every phone.
    val dark = appIsDark()
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(colorScheme = if (dark) darkColors else lightColors, typography = typography, content = content)
    }
}
