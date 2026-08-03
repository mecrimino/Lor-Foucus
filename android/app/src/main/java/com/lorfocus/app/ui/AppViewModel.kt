package com.lorfocus.app.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lorfocus.app.LorFocusApp
import com.lorfocus.app.data.AllowlistChannel
import com.lorfocus.app.data.AppSettings
import com.lorfocus.app.data.BlockedApp
import com.lorfocus.app.data.DayStat
import com.lorfocus.app.data.FeedMode
import com.lorfocus.app.data.MonitoredFeed
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** An installed, launchable app the user can choose to block. */
data class InstalledApp(val pkg: String, val label: String, val icon: Bitmap?)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val a = app as LorFocusApp

    val settings: StateFlow<AppSettings> =
        a.prefs.flow.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val feeds: StateFlow<List<MonitoredFeed>> =
        a.db.feedDao().all().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val channels: StateFlow<List<AllowlistChannel>> =
        a.db.channelDao().all().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val blockedApps: StateFlow<List<BlockedApp>> =
        a.db.blockedAppDao().all().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Installed apps, cached in a flow so the Rules list and the app-detail screen share them.
    val installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private var appsLoaded = false
    fun ensureAppsLoaded() {
        if (appsLoaded) return
        appsLoaded = true
        viewModelScope.launch { installedApps.value = loadInstalledApps() }
    }
    fun appFor(pkg: String): InstalledApp? = installedApps.value.firstOrNull { it.pkg == pkg }

    /** mode: "off" removes the app; otherwise "always" | "focus" | "limit" (with limitMinutes). */
    fun setAppMode(pkg: String, label: String, mode: String, limitMinutes: Int = 30) = viewModelScope.launch {
        if (mode == "off") a.db.blockedAppDao().deleteByPkg(pkg)
        else a.db.blockedAppDao().insert(BlockedApp(pkg, label, mode, limitMinutes))
    }

    /** Installed, launchable, non-system apps (Instagram, Discord, …) — excludes Settings/Phone/Camera. */
    suspend fun loadInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = getApplication<Application>().packageManager
        val self = getApplication<Application>().packageName
        // Never let the user block their launcher (that would trap them).
        val homePkgs = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }.toHashSet()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0).mapNotNull { ri ->
            val ai = ri.activityInfo.applicationInfo
            if (ai.packageName == self || ai.packageName in homePkgs) return@mapNotNull null
            val stockSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            if (stockSystem) return@mapNotNull null            // hide Settings/Phone/Camera etc.
            val icon = runCatching { ri.loadIcon(pm).toBitmap(96, 96) }.getOrNull()
            InstalledApp(ai.packageName, ri.loadLabel(pm).toString(), icon)
        }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
    }

    // Real stats — today's row and the recent history (for the week chart + streak).
    val todayStat: StateFlow<DayStat?> =
        a.db.statsDao().todayFlow(LocalDate.now().toString()).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val recentStats: StateFlow<List<DayStat>> =
        a.db.statsDao().recentFlow(30).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** One-shot read of the persisted settings (raw datastore flow, not the conflated StateFlow)
     *  so the start route is chosen reliably even on a fresh install. */
    suspend fun firstSettings(): AppSettings = a.prefs.flow.first()

    // settings
    fun completeOnboarding() = viewModelScope.launch { a.prefs.setOnboarded(true) }
    fun setTheme(v: String) = viewModelScope.launch { a.prefs.setTheme(v) }
    fun setLogo(v: String) = viewModelScope.launch { a.prefs.setLogo(v) }
    fun setStrict(v: Boolean) = viewModelScope.launch { a.prefs.setStrict(v) }
    fun setUninstall(v: Boolean) = viewModelScope.launch { a.prefs.setUninstall(v) }
    fun saveGoal(v: String) = viewModelScope.launch { a.prefs.setGoal(v) }

    // Timed Focus session
    fun startFocus(minutes: Int) = viewModelScope.launch {
        a.prefs.setFocusDuration(minutes)
        a.prefs.setFocusEnds(System.currentTimeMillis() + minutes * 60_000L)
    }
    fun endFocus() = viewModelScope.launch { a.prefs.setFocusEnds(0L) }
    fun setFocusDuration(minutes: Int) = viewModelScope.launch { a.prefs.setFocusDuration(minutes) }

    // "Teach Shorts" — save the reel ids captured from a real Short as the detection signature.
    fun teachShorts(reelIds: List<String>) = viewModelScope.launch { a.prefs.setShortsSig(reelIds.joinToString(",")) }
    fun clearShortsSignature() = viewModelScope.launch { a.prefs.setShortsSig("") }

    // feeds
    fun upsertFeed(feed: MonitoredFeed) = viewModelScope.launch { a.db.feedDao().upsert(feed) }
    fun setMode(id: String, mode: FeedMode) = editFeed(id) { it.copy(mode = mode) }
    fun stepBudget(id: String, delta: Int) = editFeed(id) { it.copy(budgetMinutes = (it.budgetMinutes + delta).coerceIn(5, 120)) }
    fun stepPause(id: String, delta: Int) = editFeed(id) { it.copy(pauseSeconds = (it.pauseSeconds + delta).coerceIn(3, 30)) }
    fun setHours(id: String, from: String, until: String) = editFeed(id) { it.copy(activeFrom = from, activeUntil = until) }
    fun setEnabled(id: String, on: Boolean) = editFeed(id) { it.copy(enabled = on) }

    private fun editFeed(id: String, f: (MonitoredFeed) -> MonitoredFeed) = viewModelScope.launch {
        a.db.feedDao().byId(id)?.let { a.db.feedDao().upsert(f(it)) }
    }

    // channels
    fun addChannel(name: String) = viewModelScope.launch {
        if (a.db.channelDao().countByName(name) == 0) a.db.channelDao().insert(AllowlistChannel(channelName = name))
    }
    fun removeChannel(channel: AllowlistChannel) = viewModelScope.launch { a.db.channelDao().delete(channel) }

    // F9.5 — wipe the local database + prefs, then re-seed the defaults so onboarding works again.
    fun resetAll() = viewModelScope.launch(Dispatchers.IO) {
        a.db.clearAllTables()
        a.prefs.clear()
        a.seedIfEmpty()
    }
}
