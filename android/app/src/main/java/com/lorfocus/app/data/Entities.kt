package com.lorfocus.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class FeedMode { BLOCK, PAUSE, LIMIT }

/**
 * One row per feed the user monitors. The PRD splits mode parameters into a separate
 * FeedModeConfig table; for the scaffold they're folded in here — split later if the
 * parameter set grows. ponytail: flat table now, normalise when it earns its keep.
 */
@Entity(tableName = "monitored_feed")
data class MonitoredFeed(
    @PrimaryKey val id: String,
    val appPackage: String,
    val name: String,
    val mono: String,
    val enabled: Boolean = true,
    val mode: FeedMode = FeedMode.PAUSE,
    val budgetMinutes: Int = 15,
    val pauseSeconds: Int = 10,
    val activeFrom: String = "07:00",
    val activeUntil: String = "23:00",
)

@Entity(tableName = "allowlist_channel")
data class AllowlistChannel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelName: String,
    val channelId: String = "",
    val addedNote: String = "Added just now",
)

/** An installed app the user chose to block.
 *  mode: "focus" = only during a Focus session · "always" = strict, all the time ·
 *  "limit" = usable limitMinutes per day, then blocked for the rest of the day. */
@Entity(tableName = "blocked_app")
data class BlockedApp(
    @PrimaryKey val pkg: String,
    val label: String = "",
    val mode: String = "focus",
    val limitMinutes: Int = 30,
)

/** Real on-device stats — one row per local day. Counted from actual interventions, never seeded. */
@Entity(tableName = "day_stat")
data class DayStat(
    @PrimaryKey val date: String,   // ISO local date, e.g. 2026-07-30
    val blocks: Int = 0,            // interventions that fired (scrolls stopped)
    val reclaimedSec: Int = 0,      // estimated seconds not spent scrolling
)

class Converters {
    @TypeConverter fun toMode(v: String): FeedMode = FeedMode.valueOf(v)
    @TypeConverter fun fromMode(m: FeedMode): String = m.name
}
