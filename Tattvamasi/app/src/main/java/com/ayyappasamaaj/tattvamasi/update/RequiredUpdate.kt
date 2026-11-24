package com.ayyappasamaaj.tattvamasi.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ayyappasamaaj.tattvamasi.BuildConfig
import com.ayyappasamaaj.tattvamasi.ui.theme.AppColors
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * A required update, as on iOS (RequiredUpdate.swift): the app reads the oldest version still allowed from Firebase
 * (/app_config/android_min_version, set by the maintainer in the Firebase console) and, when this copy is older, shows
 * only "Please update" with a button to Google Play. Nothing set, the read refused or the phone offline: the app works
 * as usual.
 */

/** True when [version] is older than [minimum]: "4.0" < "4.0.1" < "4.1" < "10.0". An empty minimum is no minimum. */
fun isOlder(version: String, minimum: String): Boolean {
    val have = versionParts(version)
    val need = versionParts(minimum)
    if (need.isEmpty()) return false
    for (index in 0 until maxOf(have.size, need.size)) {
        val a = have.getOrElse(index) { 0 }
        val b = need.getOrElse(index) { 0 }
        if (a != b) return a < b
    }
    return false
}

/** "4.0.1" → [4, 0, 1]; a part that isn't a number counts as 0. */
fun versionParts(text: String): List<Int> {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return emptyList()
    return trimmed.split('.').map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
}

/** The console's value as text: "4.0.1", or a number typed without quotes (4.1). */
fun minimumText(value: Any?): String = when (value) {
    is String -> value
    is Number -> value.toString()
    else -> ""
}

object RequiredUpdate {
    private const val TAG = "RequiredUpdate"
    private val _required = MutableStateFlow(false)
    val required: StateFlow<Boolean> = _required.asStateFlow()
    private var listening = false

    fun start() {
        if (listening) return
        listening = true
        try {
            FirebaseDatabase.getInstance().getReference("app_config").child("android_min_version")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        _required.value = isOlder(BuildConfig.VERSION_NAME, minimumText(snapshot.value))
                    }

                    // Not readable (no rule yet) or failed: never blocks the app
                    override fun onCancelled(error: DatabaseError) = AppLog.w(TAG, "app_config: " + error.message)
                })
        } catch (e: IllegalStateException) {
            AppLog.w(TAG, "Firebase isn't set up: " + e.message)
        }
    }
}

private const val PACKAGE = "com.ayyappasamaaj.tattvamasi"

private fun openStore(context: Context) {
    val play = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(play)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PACKAGE"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** The whole screen while an update is required. */
@Composable
fun RequiredUpdateScreen() {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(AppColors.cream).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                "Please update Tattvamasi",
                style = MaterialTheme.typography.headlineMedium,
                color = AppColors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "This version of the app is no longer supported. Update to the latest version from Google Play to keep using it.",
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.inkSoft,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = { openStore(context) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.button, contentColor = AppColors.onButton),
            ) { Text("Update", fontWeight = FontWeight(700)) }
        }
    }
}
