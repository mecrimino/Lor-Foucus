package com.lorfocus.app.detection

/**
 * Versioned, bundled detection config (§10). Isolated here so it can be corrected and
 * shipped in a normal app update without touching the engine. NO remote fetch.
 *
 * TODO(signatures): the view-id / text fragments below are PLACEHOLDERS. Real signatures
 * must be captured from each target app's current build by dumping the accessibility node
 * tree while a feed is on screen, then bumped with [VERSION]. When a signature stops
 * matching, the engine must degrade gracefully (see FeedDetectionService) rather than
 * block unrelated screens.
 */
object DetectionRules {
    const val VERSION = 1

    data class Signature(
        val feedId: String,
        val viewIdContains: List<String> = emptyList(),
        val textContains: List<String> = emptyList(),
    ) {
        /** A signature with no requirements means the whole app is the feed (e.g. TikTok). */
        val wholeApp: Boolean get() = viewIdContains.isEmpty() && textContains.isEmpty()
    }

    // YouTube Shorts uses a "reel_*" player surface; normal full-video watch pages use
    // "watch_*"/"player_*" ids and do NOT contain these, so this fires on Shorts only.
    // If it stops matching after a YouTube update, adjust these fragments and bump VERSION.
    // Player-only ids: these exist ONLY inside the immersive Shorts player, never on the home
    // feed, a normal watch page, or the bottom-nav "Shorts" button — so no false positives
    // during ordinary YouTube use. (The bottom "Shorts" tab is why matching bare "shorts" fired
    // everywhere.) If Shorts still isn't caught, Settings → Diagnostics shows the live ids to add.
    val youtubeShorts = listOf(
        "reel_player_page", "reel_watch_player", "reel_watch_fragment",
        "reel_progress_bar", "reel_time_bar",
    )

    val byPackage: Map<String, List<Signature>> = mapOf(
        "com.google.android.youtube" to listOf(Signature("ys", viewIdContains = youtubeShorts)),
        "com.instagram.android" to listOf(Signature("ir", viewIdContains = listOf("clips_viewer", "reel_viewer"))),
        "com.facebook.katana" to listOf(Signature("fr", viewIdContains = listOf("reels_"))),
        "com.zhiliaoapp.musically" to listOf(Signature("tt")),
        "com.ss.android.ugc.trill" to listOf(Signature("tt")),
        "com.snapchat.android" to listOf(Signature("ss", viewIdContains = listOf("spotlight"))),
    )

    fun isMonitored(pkg: String?) = pkg != null && byPackage.containsKey(pkg)
}
