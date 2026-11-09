@file:OptIn(ExperimentalLayoutApi::class)

package com.ayyappasamaaj.tattvamasi.ui.events

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ayyappasamaaj.tattvamasi.ui.AddressFooter
import com.ayyappasamaaj.tattvamasi.ui.DateBlock
import com.ayyappasamaaj.tattvamasi.ui.ReadableWidth
import com.ayyappasamaaj.tattvamasi.ui.addToCalendar
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.openDirections
import com.ayyappasamaaj.tattvamasi.ui.openWebPage
import com.ayyappasamaaj.tattvamasi.ui.text.RichText
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import java.time.Instant

/**
 * One event, in the app: its picture, when and where, Directions and Add to calendar, Register (the website's
 * registration form, opened only for that), and the event's text. Replaces opening the website's event page.
 */
@Composable
fun EventScreen(event: AppEvent, onBack: () -> Unit, now: Instant = Instant.now()) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
        ReadableWidth {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = AppColors.clayDark)
                        Text("Events", color = AppColors.clayDark, modifier = Modifier.padding(start = 6.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    event.pageUrl?.let { url ->
                        IconButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_SUBJECT, event.name).putExtra(Intent.EXTRA_TEXT, "${event.name}\n$url")
                            context.startActivity(Intent.createChooser(send, "Share"))
                        }) { Icon(Icons.Outlined.Share, contentDescription = "Share", tint = AppColors.ink) }
                    }
                }

                event.imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        // The whole picture (event covers are often posters with text), at the screen's width
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).clip(RoundedCornerShape(16.dp))
                            .background(AppColors.claySoft),
                    )
                }

                Text(event.name, style = MaterialTheme.typography.headlineMedium, color = AppColors.ink,
                    modifier = Modifier.semantics { heading() })

                // When and where
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth().card(14.dp)) {
                    DateBlock(event.start)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(dateLine(event), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            color = AppColors.ink)
                        // The hours; a longer event's time line is its dates, already said above
                        if (EventTime.format(event.start, "yyyyMMdd") == EventTime.format(event.end, "yyyyMMdd")) {
                            Text(event.timeText, style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
                        }
                        if (event.venue.isNotBlank()) {
                            Text(event.venue, style = MaterialTheme.typography.bodyMedium, color = AppColors.muted)
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { addToCalendar(context, event) }) {
                        Icon(Icons.Outlined.EditCalendar, contentDescription = null, tint = AppColors.clay, modifier = Modifier.size(18.dp))
                        Text("Add to calendar", color = AppColors.ink, modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(onClick = { openDirections(context, event) }) {
                        Icon(Icons.Outlined.Directions, contentDescription = null, tint = AppColors.clay, modifier = Modifier.size(18.dp))
                        Text("Directions", color = AppColors.ink, modifier = Modifier.padding(start = 6.dp))
                    }
                }

                event.registration?.let { RegistrationCard(it, now) { openWebPage(context, it.url) } }

                if (event.details.isNotBlank()) {
                    RichText(event.details)
                } else {
                    event.extraSummary?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink) }
                }
                AddressFooter()
            }
        }
    }
}

/** "Saturday, November 14", or "Saturday, November 14 to Saturday, December 26" for a longer event. */
fun dateLine(event: AppEvent): String {
    val first = EventTime.format(event.start, "EEEE, MMMM d")
    val last = EventTime.format(event.end, "EEEE, MMMM d")
    return if (first == last) first else "$first to $last"
}

/** "Sat, Dec 5, 9:00 AM" in the Bay Area's time. */
fun registrationTime(at: Instant): String = EventTime.format(at, "EEE, MMM d, h:mm a")

@Composable
private fun RegistrationCard(registration: Registration, now: Instant, register: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().card(14.dp)) {
        Text("Registration", style = MaterialTheme.typography.titleMedium, color = AppColors.ink,
            modifier = Modifier.semantics { heading() })
        when (val status = registration.status(now)) {
            Registration.Status.Open -> {
                Button(onClick = register, colors = ButtonDefaults.buttonColors(containerColor = AppColors.forest)) {
                    Text("Register", fontWeight = FontWeight(700))
                }
                Text(
                    listOfNotNull(
                        registration.closesAt?.let { "Open until ${registrationTime(it)}." },
                        "The form opens on ayyappasamaaj.org.",
                    ).joinToString(" "),
                    style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft,
                )
            }
            is Registration.Status.OpensOn -> Text("Registration opens ${registrationTime(status.at)}.",
                style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
            Registration.Status.Closed -> Text("Registration is closed.",
                style = MaterialTheme.typography.bodyLarge, color = AppColors.inkSoft)
        }
    }
}
