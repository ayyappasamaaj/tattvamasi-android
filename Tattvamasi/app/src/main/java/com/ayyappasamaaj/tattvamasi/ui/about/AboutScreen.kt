package com.ayyappasamaaj.tattvamasi.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayyappasamaaj.tattvamasi.ui.AddressFooter
import com.ayyappasamaaj.tattvamasi.ui.ReadableWidth
import com.ayyappasamaaj.tattvamasi.ui.ScreenTitle
import com.ayyappasamaaj.tattvamasi.ui.card
import com.ayyappasamaaj.tattvamasi.ui.openWebPage
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors

/** The tab's three parts. */
enum class AboutPart(val title: String) { About("About us"), Board("Our Board"), Contact("Contact") }

/**
 * About us: About us, Our Board and Contact, switched with segmented buttons. The text is the website's
 * (ayyappasamaaj.org/about/ and /contact/); the Board comes from the website. As on the iOS app (AboutView.swift).
 */
@Composable
fun AboutScreen() {
    var part by rememberSaveable { mutableStateOf(AboutPart.About) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ReadableWidth {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ScreenTitle(part.title)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    AboutPart.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = option == part,
                            onClick = { part = option },
                            shape = SegmentedButtonDefaults.itemShape(index, AboutPart.entries.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = AppColors.claySoft, activeContentColor = AppColors.clayDark,
                                inactiveContainerColor = AppColors.card,
                            ),
                        ) { Text(option.title, maxLines = 1) }
                    }
                }
                when (part) {
                    AboutPart.About -> AboutUs()
                    AboutPart.Board -> OurBoard()
                    AboutPart.Contact -> Contact()
                }
                AddressFooter()
            }
        }
    }
}

private val mission = listOf(
    "Operate a Cultural & Spiritual Center to propagate and perpetuate the religious and cultural aspects of the " +
        "worship of Lord Ayyappa.",
    "Publish electronic and print publications to promote the awareness of Lord Ayyappa and associated deities to " +
        "the community.",
    "Organize spiritual events and religious events associated with the worship of Lord Ayyappa and associated " +
        "deities.",
    "Promote the construction of a permanent shrine for Lord Ayyappa and deities associated with Lord Ayyappa in " +
        "one of the temples in the San Francisco Bay Area or adjoining locations.",
    "Provide assistance to other communities (local and worldwide) in the area of human, religious and community " +
        "assistance services.",
)

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = AppColors.ink,
        modifier = Modifier.semantics { heading() })
}

@Composable
private fun AboutUs() {
    val body = MaterialTheme.typography.bodyLarge
    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Swamiye Saranam Ayyappa", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = AppColors.muted)
        Text(
            "Ayyappa Samaaj is a registered 501(c)(3) nonprofit organization of devotees of Lord Ayyappa, based in " +
                "the San Francisco Bay Area in California, USA. The group has hundreds of members and volunteers, " +
                "and has been conducting pujas and bhajans in praise of Lord Ayyappa since 2005: first as an " +
                "informal group called Ayyappa Samaaj, registered as a nonprofit in 2011.",
            style = body, color = AppColors.ink,
        )
        Text("Tax ID (EIN) 45-1061428", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight(600)),
            color = AppColors.inkSoft)
    }
    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Heading("Our mission")
        mission.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Text("•", style = body, color = AppColors.clay)
                Text(item, style = body.copy(fontSize = 16.sp), color = AppColors.inkSoft)
            }
        }
    }
    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Heading("Our history")
        Text(
            "Ayyappa Samaaj was formed by a group of active devotees in the San Francisco Bay Area to bring the " +
                "area's many Ayyappa devotees together to serve Lord Ayyappa and build His permanent shrine. We " +
                "began with an online mailing group that grew to hundreds of active members, along with a separate " +
                "volunteer group.",
            style = body.copy(fontSize = 16.sp), color = AppColors.inkSoft,
        )
        Text(
            "Today, Ayyappa Samaaj is an IRS-recognized 501(c)(3) nonprofit organization and a gathering point for " +
                "Ayyappa devotees. While our focus remains service and devotion to Lord Ayyappa, we have broadened " +
                "our mission to serve the wider spiritual and cultural needs of the Bay Area community.",
            style = body.copy(fontSize = 16.sp), color = AppColors.inkSoft,
        )
    }
}

@Composable
private fun OurBoard() {
    val context = LocalContext.current
    val board by BoardStore.state
    LaunchedEffect(Unit) { BoardStore.load() }
    Column(Modifier.fillMaxWidth().card(16.dp)) {
        Text("${board.year} board", style = MaterialTheme.typography.titleLarge, color = AppColors.ink,
            modifier = Modifier.padding(vertical = 8.dp).semantics { heading() })
        board.members.forEachIndexed { index, member ->
            if (index > 0) HorizontalDivider(color = AppColors.line)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).semantics(mergeDescendants = true) {},
            ) {
                Box(Modifier.size(40.dp).background(AppColors.claySoft, CircleShape), contentAlignment = Alignment.Center) {
                    Text(member.initials, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight(700)),
                        color = AppColors.clayDark)
                }
                Column {
                    Text(member.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(600)),
                        color = AppColors.ink)
                    Text(member.role, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = AppColors.muted)
                }
            }
        }
        Text(
            "Earlier years' boards on ayyappasamaaj.org",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight(600),
                textDecoration = TextDecoration.Underline),
            color = AppColors.clayDark,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .clickable(role = Role.Button) { openWebPage(context, "https://ayyappasamaaj.org/board/") }
                .padding(vertical = 8.dp),
        )
    }
}

private data class ContactLink(val title: String, val icon: ImageVector, val url: String)

private val contactLinks = listOf(
    ContactLink("Write to us", Icons.Outlined.Drafts, "https://ayyappasamaaj.org/contact/"),
    ContactLink("Join the mailing list", Icons.Outlined.Newspaper, "https://ayyappasamaaj.org/mailing-list/"),
    ContactLink("Facebook", Icons.Outlined.Group, "https://www.facebook.com/ayyappa.samaaj.us"),
    ContactLink("YouTube", Icons.Outlined.SmartDisplay, "https://www.youtube.com/@AyyappasamaajOrg"),
)

private const val EMAIL = "info@ayyappasamaaj.org"

@Composable
private fun Contact() {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().card(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Whether you are wearing the mala for the first time, sponsoring a puja, or want to volunteer during " +
                "Mandalam, we are happy to help.",
            style = MaterialTheme.typography.bodyLarge, color = AppColors.ink,
        )
        Button(
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$EMAIL")))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, "No email app found. Write to $EMAIL", Toast.LENGTH_LONG).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.forest),
        ) {
            Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(EMAIL, fontWeight = FontWeight(700), modifier = Modifier.padding(start = 8.dp))
        }
        Text("A Virtual Organization: no office or phone; we answer by email.",
            style = MaterialTheme.typography.bodyMedium, color = AppColors.inkSoft)
    }
    Column(Modifier.fillMaxWidth().card(14.dp)) {
        contactLinks.forEachIndexed { index, link ->
            if (index > 0) HorizontalDivider(color = AppColors.line)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clickable(role = Role.Button) { openWebPage(context, link.url) },
            ) {
                Icon(link.icon, contentDescription = null, tint = AppColors.clay)
                Text(link.title, style = MaterialTheme.typography.bodyLarge, color = AppColors.ink,
                    modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = AppColors.muted)
            }
        }
    }
}
