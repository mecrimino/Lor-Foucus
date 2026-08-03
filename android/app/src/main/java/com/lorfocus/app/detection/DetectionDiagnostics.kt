package com.lorfocus.app.detection

/**
 * Last thing the detection engine saw on a monitored app — surfaced in the in-app Diagnostics
 * screen so real YouTube view-ids can be captured without adb. Not persisted; volatile is fine.
 */
object DetectionDiagnostics {
    @Volatile var lastPackage: String = "—"
    @Volatile var lastShorts: Boolean = false
    @Volatile var lastReelCount: Int = 0
    @Volatile var lastChannel: String? = null
    @Volatile var interestingIds: List<String> = emptyList()
    @Volatile var updatedAt: Long = 0L
    /** reel_* ids from the most recent YouTube screen — used by the "teach Shorts" button. */
    @Volatile var lastYtReelIds: List<String> = emptyList()
}
