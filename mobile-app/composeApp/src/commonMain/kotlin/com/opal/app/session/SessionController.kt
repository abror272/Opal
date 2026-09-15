package com.opal.app.session

import com.opal.app.data.OpalRepository
import com.opal.app.data.SessionPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/**
 * Faol fokus sessiyasi — har sekundda tick bo'ladi, tugagach avtomatik "completed".
 */
class SessionController(private val repo: OpalRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    data class Active(
        val preset: SessionPreset,
        val totalSeconds: Long,
        val startMark: TimeSource.Monotonic.ValueTimeMark,
        val remoteId: String? = null
    )

    data class Completion(
        val label: String,
        val emoji: String,
        val savedMinutes: Int,
        val completedFully: Boolean,
        val streakAfter: Int,
        val sessionId: String? = null
    )

    val active = MutableStateFlow<Active?>(null)
    val elapsedSeconds = MutableStateFlow(0L)
    val completion = MutableStateFlow<Completion?>(null)

    val remainingSeconds: Long
        get() {
            val a = active.value ?: return 0
            return (a.totalSeconds - elapsedSeconds.value).coerceAtLeast(0)
        }

    val progressFraction: Float
        get() {
            val a = active.value ?: return 0f
            if (a.totalSeconds <= 0) return 0f
            return (elapsedSeconds.value.toDouble() / a.totalSeconds.toDouble()).toFloat().coerceIn(0f, 1f)
        }

    fun start(preset: SessionPreset) {
        if (active.value != null) return
        val created = repo.startSession(preset)
        active.value = Active(
            preset = preset,
            totalSeconds = preset.minutes * 60L,
            startMark = TimeSource.Monotonic.markNow(),
            remoteId = created.id
        )
        elapsedSeconds.value = 0

        // Sekundlik ticker
        scope.launch {
            while (isActive) {
                delay(1000)
                val a = active.value ?: break
                val elapsed = a.startMark.elapsedNow().inWholeSeconds
                elapsedSeconds.value = elapsed
                if (elapsed >= a.totalSeconds) {
                    finish(early = false)
                    break
                }
            }
        }
    }

    fun finish(early: Boolean) {
        val a = active.value ?: return
        active.value = null

        val elapsedMin = a.startMark.elapsedNow().inWholeMinutes.toInt()
        val saved = if (early) maxOf(elapsedMin - 2, 0) else a.preset.minutes

        // HAQIQIY natijani qurilmaga yozamiz (streak/statistika shundan hisoblanadi)
        val id = a.remoteId
        if (id != null) {
            repo.completeSession(id, early, saved)
        } else {
            repo.refreshRealStats(force = true)
        }

        completion.value = Completion(
            label = a.preset.label,
            emoji = a.preset.emoji,
            savedMinutes = saved,
            completedFully = !early,
            streakAfter = repo.profile.value.streakDays,
            sessionId = id
        )

        scope.launch {
            delay(3600)
            completion.value = null
        }
    }
}
