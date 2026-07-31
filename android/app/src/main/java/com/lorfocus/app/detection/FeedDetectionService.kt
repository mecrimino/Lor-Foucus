package com.lorfocus.app.detection

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.lorfocus.app.LorFocusApp
import com.lorfocus.app.data.BlockedApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * F2 — the engine.
 *  - YouTube Shorts: short popup then Back; short-form URLs in any browser are always blocked.
 *  - Each blocked app has a mode: "always" (strict), "focus" (only in a session), or "limit"
 *    (usable N minutes/day via real usage stats, then blocked). Their websites are blocked in
 *    browsers on the same terms.
 * State is cached in memory (flow collectors) so each event is cheap.
 */
class FeedDetectionService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())
    private var lastPkg = ""
    private var lastAt = 0L
    private var cooldownUntil = 0L

    @Volatile private var blockList: List<BlockedApp> = emptyList()
    @Volatile private var blockPkgs: Set<String> = emptySet()
    @Volatile private var alwaysDomains: List<String> = emptyList()
    @Volatile private var focusDomains: List<String> = emptyList()
    @Volatile private var focusEndsAt: Long = 0L
    @Volatile private var shortsEnabled: Boolean = true
    private val usageCache = HashMap<String, Pair<Long, Long>>()

    private data class Scan(val isShorts: Boolean, val url: String?, val ids: List<String>)
    private enum class Kind { YOUTUBE, BLOCKED, BROWSER }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app = applicationContext as LorFocusApp
        scope.launch {
            app.db.blockedAppDao().all().collect { list ->
                blockList = list
                blockPkgs = list.map { it.pkg }.toHashSet()
                alwaysDomains = domainsFor(list.filter { it.mode == "always" }.map { it.pkg })
                focusDomains = domainsFor(list.filter { it.mode != "always" }.map { it.pkg })
            }
        }
        scope.launch { app.prefs.flow.collect { focusEndsAt = it.focusEndsAt } }
        scope.launch { app.db.feedDao().all().collect { feeds -> shortsEnabled = feeds.any { it.id == "ys" && it.enabled } } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        val kind = kindOf(pkg) ?: return

        val now = SystemClock.uptimeMillis()
        if (pkg == lastPkg && now - lastAt < DEBOUNCE_MS) return
        lastPkg = pkg; lastAt = now
        val focusActive = focusEndsAt > System.currentTimeMillis()

        if (kind == Kind.BLOCKED) {
            if (now < cooldownUntil) return
            val entry = blockList.firstOrNull { it.pkg == pkg } ?: return
            scope.launch {
                val block = when (entry.mode) {
                    "always" -> true
                    "limit" -> focusActive || usageMinutesToday(pkg) >= entry.limitMinutes
                    else -> focusActive
                }
                if (block) {
                    cooldownUntil = SystemClock.uptimeMillis() + 4000L
                    val msg = when {
                        focusActive -> "Paused during Focus — ${remaining()} min left."
                        entry.mode == "limit" -> "You've used your ${entry.limitMinutes} minutes today."
                        else -> "You set this app to always off."
                    }
                    blockWhole(msg)
                }
            }
            return
        }

        val root = rootInActiveWindow ?: return
        val s = try { scan(pkg, root) } catch (t: Throwable) { Scan(false, null, emptyList()) }

        DetectionDiagnostics.lastPackage = pkg
        DetectionDiagnostics.lastShorts = s.isShorts
        DetectionDiagnostics.lastChannel = s.url
        DetectionDiagnostics.interestingIds = s.ids
        DetectionDiagnostics.updatedAt = System.currentTimeMillis()

        if (now < cooldownUntil) return

        scope.launch {
            when (kind) {
                Kind.YOUTUBE -> if (s.isShorts && (shortsEnabled || focusActive)) {
                    cooldownUntil = SystemClock.uptimeMillis() + 6000L
                    shortsPopup()
                }
                Kind.BROWSER -> {
                    val u = s.url?.lowercase() ?: return@launch
                    when {
                        isShortFormUrl(u) -> { cooldownUntil = SystemClock.uptimeMillis() + 4000L; blockWhole("That's a short-form feed — blocked.") }
                        alwaysDomains.any { u.contains(it) } -> { cooldownUntil = SystemClock.uptimeMillis() + 4000L; blockWhole("You set this site to always off.") }
                        focusActive && focusDomains.any { u.contains(it) } -> { cooldownUntil = SystemClock.uptimeMillis() + 4000L; blockWhole("Paused during Focus — ${remaining()} min left.") }
                    }
                }
                else -> {}
            }
        }
    }

    private suspend fun shortsPopup() {
        val app = applicationContext as LorFocusApp
        val goal = app.prefs.flow.first().goal
        overlay(OverlayService.KIND_SHORTS, "YouTube Shorts", 3, goal, null)
        main.postDelayed({ back() }, 3000L)
        app.recordIntervention(RECLAIM_SECONDS_PER_BLOCK)
    }

    private suspend fun blockWhole(subtitle: String) {
        val app = applicationContext as LorFocusApp
        val goal = app.prefs.flow.first().goal
        back()
        overlay(OverlayService.KIND_BLOCK, "Focus", 0, goal, subtitle)
        app.recordIntervention(RECLAIM_SECONDS_PER_BLOCK)
    }

    private fun remaining(): Int =
        (((focusEndsAt - System.currentTimeMillis()) / 60000L) + 1).coerceAtLeast(1L).toInt()

    /** Real per-app foreground minutes today (needs Usage access). Cached 15s. */
    private fun usageMinutesToday(pkg: String): Long {
        val nowUp = SystemClock.uptimeMillis()
        usageCache[pkg]?.let { if (nowUp - it.first < 15_000L) return it.second }
        val minutes = try {
            val usm = getSystemService(UsageStatsManager::class.java)
            val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val stats = usm?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, System.currentTimeMillis())
            (stats?.filter { it.packageName == pkg }?.sumOf { it.totalTimeInForeground } ?: 0L) / 60000L
        } catch (t: Throwable) { 0L }
        usageCache[pkg] = nowUp to minutes
        return minutes
    }

    private fun scan(pkg: String, root: AccessibilityNodeInfo): Scan {
        val frags = DetectionRules.byPackage[pkg]?.flatMap { it.viewIdContains } ?: emptyList()
        var isShorts = false
        var url: String? = null
        val ids = LinkedHashSet<String>()
        var scanned = 0
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        while (stack.isNotEmpty() && scanned < MAX_NODES) {
            val node = stack.removeLast(); scanned++
            val vid = node.viewIdResourceName
            if (vid != null) {
                val short = vid.substringAfterLast('/')
                if (KEYWORDS.any { short.contains(it, true) }) ids.add(short)
                if (!isShorts && frags.any { vid.contains(it, true) }) isShorts = true
                if (url == null && isUrlBar(short)) {
                    node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { url = it }
                }
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let { stack.addLast(it) }
        }
        return Scan(isShorts, url, ids.take(40).toList())
    }

    private fun isUrlBar(short: String): Boolean {
        val s = short.lowercase()
        return s.contains("url_bar") || s.contains("url_field") || s.contains("url_view") ||
            s.contains("location_bar") || s == "url" || s.contains("omnibox")
    }

    private fun isShortFormUrl(s: String): Boolean =
        s.contains("youtube.com/shorts") || s.contains("/shorts/") ||
            s.contains("instagram.com/reel") || s.contains("tiktok.com") ||
            s.contains("facebook.com/reel") || s.contains("fb.watch")

    private fun domainsFor(pkgs: List<String>): List<String> {
        val d = pkgs.mapNotNull { PKG_DOMAIN[it] }.toMutableList()
        if (pkgs.contains("com.twitter.android")) d.add("x.com")
        return d
    }

    private fun kindOf(pkg: String): Kind? = when {
        pkg == YOUTUBE && !blockPkgs.contains(pkg) -> Kind.YOUTUBE
        pkg in blockPkgs -> Kind.BLOCKED
        pkg in BROWSERS -> Kind.BROWSER
        else -> null
    }

    private fun back() = main.post { performGlobalAction(GLOBAL_ACTION_BACK) }

    private fun overlay(kind: String, name: String, seconds: Int, goal: String, subtitle: String?) {
        startForegroundService(
            Intent(this, OverlayService::class.java)
                .putExtra(OverlayService.EXTRA_KIND, kind)
                .putExtra(OverlayService.EXTRA_FEED, name)
                .putExtra(OverlayService.EXTRA_PAUSE, seconds)
                .putExtra(OverlayService.EXTRA_GOAL, goal)
                .putExtra(OverlayService.EXTRA_SUBTITLE, subtitle)
        )
    }

    override fun onInterrupt() {}
    override fun onDestroy() { main.removeCallbacksAndMessages(null); super.onDestroy() }

    companion object {
        private const val YOUTUBE = "com.google.android.youtube"
        private const val DEBOUNCE_MS = 400L
        private const val MAX_NODES = 700
        private const val RECLAIM_SECONDS_PER_BLOCK = 40
        private val KEYWORDS = listOf("reel", "short", "url", "location", "omnibox")
        private val BROWSERS = setOf(
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev",
            "org.mozilla.firefox", "com.brave.browser", "com.microsoft.emmx",
            "com.opera.browser", "com.opera.mini.native", "com.sec.android.app.sbrowser",
            "com.duckduckgo.mobile.android", "com.android.browser", "com.vivaldi.browser",
        )
        private val PKG_DOMAIN = mapOf(
            "com.instagram.android" to "instagram.com", "com.instagram.lite" to "instagram.com",
            "com.facebook.katana" to "facebook.com", "com.facebook.lite" to "facebook.com",
            "com.twitter.android" to "twitter.com",
            "com.zhiliaoapp.musically" to "tiktok.com", "com.ss.android.ugc.trill" to "tiktok.com",
            "com.snapchat.android" to "snapchat.com", "com.reddit.frontpage" to "reddit.com",
            "com.discord" to "discord.com", "com.whatsapp" to "whatsapp.com",
            "com.google.android.youtube" to "youtube.com", "com.pinterest" to "pinterest.com",
            "com.linkedin.android" to "linkedin.com", "tv.twitch.android.app" to "twitch.tv",
            "com.netflix.mediaclient" to "netflix.com",
        )
    }
}
