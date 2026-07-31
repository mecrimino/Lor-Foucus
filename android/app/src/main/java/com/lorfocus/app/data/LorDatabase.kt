package com.lorfocus.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MonitoredFeed::class, AllowlistChannel::class, DayStat::class, BlockedApp::class],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class LorDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
    abstract fun channelDao(): ChannelDao
    abstract fun statsDao(): StatsDao
    abstract fun blockedAppDao(): BlockedAppDao

    companion object {
        @Volatile private var instance: LorDatabase? = null
        fun get(context: Context): LorDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, LorDatabase::class.java, "lorfocus.db"
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
