package com.companion.yogawellness.data

import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.scoring.PoseSegmentStats

/** Where [SessionAutoRecorder] checkpoints the live session (implemented by [WellnessRepository]). */
interface LiveSessionStore {
    suspend fun upsertLiveSession(
        sessionId: Long?,
        segments: List<PoseSegmentStats>,
        poseStartedAtMs: Map<AsanaLabel, Long>,
        startedAtMs: Long,
        endedAtMs: Long,
        epochDay: Long,
        toleranceProfile: String
    ): Long?
}

/**
 * In-memory live motor-emotion state for the current practice (Motor-to-Emotion feature).
 * Written from the pipeline coroutine, read by the UI; values are immutable snapshots, so readers
 * never block the camera analysis thread.
 */
object LiveMindState {
    private val _motor = kotlinx.coroutines.flow.MutableStateFlow(com.companion.yogawellness.motor.MotorFeatureSnapshot.EMPTY)
    val motor: kotlinx.coroutines.flow.StateFlow<com.companion.yogawellness.motor.MotorFeatureSnapshot> = _motor

    private val _estimate = kotlinx.coroutines.flow.MutableStateFlow<com.companion.yogawellness.motor.EmotionEstimate?>(null)
    val estimate: kotlinx.coroutines.flow.StateFlow<com.companion.yogawellness.motor.EmotionEstimate?> = _estimate

    /** The most recent finished session's report ("View Last Wellness Report"). */
    private val _latestReport = kotlinx.coroutines.flow.MutableStateFlow<com.companion.yogawellness.motor.MentalWellnessReport?>(null)
    val latestReport: kotlinx.coroutines.flow.StateFlow<com.companion.yogawellness.motor.MentalWellnessReport?> = _latestReport

    fun publish(motor: com.companion.yogawellness.motor.MotorFeatureSnapshot, estimate: com.companion.yogawellness.motor.EmotionEstimate?) {
        _motor.value = motor
        _estimate.value = estimate
    }

    fun clearLive() {
        _motor.value = com.companion.yogawellness.motor.MotorFeatureSnapshot.EMPTY
        _estimate.value = null
    }

    fun setLatestReport(report: com.companion.yogawellness.motor.MentalWellnessReport) {
        _latestReport.value = report
    }
}
