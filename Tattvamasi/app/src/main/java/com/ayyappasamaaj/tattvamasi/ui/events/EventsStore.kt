package com.ayyappasamaaj.tattvamasi.ui.events

import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import kotlin.coroutines.resume

/** What the Home and Events screens show. */
data class EventsState(
    val events: List<AppEvent> = emptyList(),
    val loaded: Boolean = false,
    val failed: Boolean = false,
) {
    val nextEvent: AppEvent? get() = events.firstOrNull()
}

/**
 * The upcoming events, kept up to date from Firebase `/events` while the app runs: the list changes as soon as
 * the website publishes a change. Shared by Home and Events.
 */
object EventsStore {
    private const val TAG = "EventsStore"
    private val _state = MutableStateFlow(EventsState())
    val state: StateFlow<EventsState> = _state.asStateFlow()
    private var listening = false

    private val reference get() = FirebaseDatabase.getInstance().getReference("events")

    fun start() {
        if (listening) return
        listening = true
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) = show(snapshot)

            override fun onCancelled(error: DatabaseError) {
                AppLog.w(TAG, "events: " + error.message)
                _state.value = _state.value.copy(loaded = true, failed = _state.value.events.isEmpty())
            }
        })
    }

    /** Pull to refresh: reads `/events` once more from the server. */
    suspend fun refresh() {
        suspendCancellableCoroutine { continuation ->
            reference.get().addOnCompleteListener { task ->
                if (task.isSuccessful) show(task.result)
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    private fun show(snapshot: DataSnapshot) {
        _state.value = EventsState(parseEvents(snapshot.value, Instant.now()), loaded = true)
    }
}
