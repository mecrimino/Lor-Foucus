package com.lorfocus.app

import android.app.Application
import com.lorfocus.app.data.DayStat
import com.lorfocus.app.data.FeedMode
import com.lorfocus.app.data.LorDatabase
import com.lorfocus.app.data.MonitoredFeed
import com.lorfocus.app.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class LorFocusApp : Application() {

    val db by lazy { LorDatabase.get(this) }
    val prefs by lazy { Prefs(this) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        seedIfEmpty()
    }

    /** Only YouTube Shorts is active by default (3-second pause, then back to full videos).
     *  The other feeds are seeded but off — enable them in Rules if you want. No mock channels
     *  and no mock stats: the allowlist and the numbers start empty and fill from real use. */
    fun seedIfEmpty() = scope.launch {
        if (db.feedDao().count() == 0) {
            db.feedDao().insertAll(
                listOf(
                    MonitoredFeed("ys", "com.google.android.youtube", "YouTube Shorts", "YS", enabled = true, mode = FeedMode.PAUSE, pauseSeconds = 3),
                    MonitoredFeed("ir", "com.instagram.android", "Instagram Reels", "IR", enabled = false, mode = FeedMode.BLOCK),
                    MonitoredFeed("fr", "com.facebook.katana", "Facebook Reels", "FR", enabled = false, mode = FeedMode.BLOCK),
                    MonitoredFeed("tt", "com.zhiliaoapp.musically", "TikTok", "TT", enabled = false, mode = FeedMode.BLOCK),
                    MonitoredFeed("ss", "com.snapchat.android", "Snapchat Spotlight", "SS", enabled = false, mode = FeedMode.BLOCK),
                )
            )
        }
    }

    /** Record one real intervention against today's stats. Called by the detection service. */
    suspend fun recordIntervention(reclaimedSec: Int) {
        val date = LocalDate.now().toString()
        val cur = db.statsDao().get(date) ?: DayStat(date)
        db.statsDao().upsert(cur.copy(blocks = cur.blocks + 1, reclaimedSec = cur.reclaimedSec + reclaimedSec))
    }
}
