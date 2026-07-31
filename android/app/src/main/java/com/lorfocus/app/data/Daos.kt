package com.lorfocus.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Query("SELECT * FROM monitored_feed") fun all(): Flow<List<MonitoredFeed>>
    @Query("SELECT * FROM monitored_feed WHERE id = :id") suspend fun byId(id: String): MonitoredFeed?
    @Query("SELECT * FROM monitored_feed WHERE appPackage = :pkg AND enabled = 1")
    suspend fun enabledForPackage(pkg: String): MonitoredFeed?
    @Upsert suspend fun upsert(feed: MonitoredFeed)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(feeds: List<MonitoredFeed>)
    @Query("SELECT COUNT(*) FROM monitored_feed") suspend fun count(): Int
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM allowlist_channel ORDER BY id DESC") fun all(): Flow<List<AllowlistChannel>>
    @Query("SELECT COUNT(*) FROM allowlist_channel WHERE channelName = :name") suspend fun countByName(name: String): Int
    @Query("SELECT channelName FROM allowlist_channel") suspend fun names(): List<String>
    @Insert suspend fun insert(channel: AllowlistChannel)
    @Delete suspend fun delete(channel: AllowlistChannel)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(channels: List<AllowlistChannel>)
    @Query("SELECT COUNT(*) FROM allowlist_channel") suspend fun count(): Int
}

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_app ORDER BY label") fun all(): Flow<List<BlockedApp>>
    @Query("SELECT pkg FROM blocked_app") suspend fun packages(): List<String>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(app: BlockedApp)
    @Query("DELETE FROM blocked_app WHERE pkg = :pkg") suspend fun deleteByPkg(pkg: String)
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM day_stat WHERE date = :date") fun todayFlow(date: String): Flow<DayStat?>
    @Query("SELECT * FROM day_stat ORDER BY date DESC LIMIT :n") fun recentFlow(n: Int): Flow<List<DayStat>>
    @Query("SELECT * FROM day_stat WHERE date = :date") suspend fun get(date: String): DayStat?
    @Upsert suspend fun upsert(stat: DayStat)
}
