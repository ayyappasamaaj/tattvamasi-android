package com.ayyappasamaaj.tattvamasi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.events.EventTime
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import java.time.Instant

/** A white card with the website's thin border and rounded corners. */
fun Modifier.card(padding: Dp = 16.dp): Modifier = this
    .background(AppColors.card, RoundedCornerShape(16.dp))
    .border(1.dp, AppColors.line, RoundedCornerShape(16.dp))
    .padding(padding)

/** Keeps text a readable width on a tablet, centered. */
@Composable
fun ReadableWidth(maxWidth: Dp = 820.dp, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = maxWidth).fillMaxWidth()) { content() }
    }
}

/** A screen's title: "Events". */
@Composable
fun ScreenTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.displaySmall,
        color = AppColors.ink,
        modifier = Modifier.semantics { heading() },
    )
}

/** A small capitals heading: "NOVEMBER 2026". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight(700),
            letterSpacing = 0.8.sp),
        color = AppColors.muted,
        modifier = modifier.semantics { heading() },
    )
}

/** The clay date block: "NOV / 14 / Sat". Read out as "Saturday, November 14". */
@Composable
fun DateBlock(date: Instant, size: Dp = 56.dp) {
    val spoken = EventTime.format(date, "EEEE, MMMM d")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = size)
            .background(AppColors.clay, RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = spoken },
    ) {
        val small = MaterialTheme.typography.labelSmall
        Text(EventTime.format(date, "MMM").uppercase(), style = small.copy(letterSpacing = 0.8.sp),
            color = AppColors.card)
        Text(EventTime.format(date, "d"), style = MaterialTheme.typography.headlineSmall.copy(
            fontSize = (size.value * 0.43f).sp), color = AppColors.card)
        Text(EventTime.format(date, "EEE"), style = small.copy(fontWeight = FontWeight(400)), color = AppColors.card)
    }
}

/** The Samaaj's address line, at the bottom of a screen. As on the iOS app. */
@Composable
fun AddressFooter() {
    Text(
        "Ayyappa Samaaj · A Virtual Organization\nSan Jose, CA 95120",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.muted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
    )
}
