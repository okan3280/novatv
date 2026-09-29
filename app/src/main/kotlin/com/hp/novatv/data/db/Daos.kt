package com.hp.novatv.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun byId(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observeById(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT COUNT(*) FROM playlists")
    fun observeCount(): Flow<Int>

    @Query("UPDATE playlists SET lastSync = :ts, channelCount = :count WHERE id = :id")
    suspend fun markSynced(id: Long, ts: Long, count: Int)
}

@Dao
interface ChannelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(channel: ChannelEntity): Long

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)

    @Transaction
    suspend fun replaceForPlaylist(playlistId: Long, channels: List<ChannelEntity>) {
        deleteByPlaylist(playlistId)
        insertAll(channels)
    }

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY number ASC, name ASC")
    fun observeByPlaylist(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun byId(id: Long): ChannelEntity?

    @Query("SELECT * FROM channels WHERE id = :id")
    fun observeById(id: Long): Flow<ChannelEntity?>

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId")
    suspend fun countByPlaylist(playlistId: Long): Int

    @Query("SELECT COUNT(*) FROM channels")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT DISTINCT `group` FROM channels WHERE playlistId = :playlistId AND `group` != '' ORDER BY `group` COLLATE NOCASE ASC")
    fun observeGroups(playlistId: Long): Flow<List<String>>

    /** Ana ekran grup kolonu icin. */
    @Query(
        """
        SELECT * FROM channels
        WHERE playlistId = :playlistId
          AND isHidden = 0
          AND (`group` = :group OR :group = '')
        ORDER BY number ASC, name ASC
        """
    )
    fun observeGroup(playlistId: Long, group: String): Flow<List<ChannelEntity>>

    @Query(
        """
        SELECT * FROM channels
        WHERE isHidden = 0
          AND name LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    fun searchByName(query: String, limit: Int = 50): Flow<List<ChannelEntity>>

    @Query(
        """
        SELECT * FROM channels
        WHERE playlistId = :playlistId AND tvgId != '' AND tvgId IN (:tvgIds)
        """
    )
    suspend fun byTvgIds(playlistId: Long, tvgIds: List<String>): List<ChannelEntity>

    @Query("UPDATE channels SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("UPDATE channels SET isLocked = :locked WHERE id = :id")
    suspend fun setLocked(id: Long, locked: Boolean)

    @Query("UPDATE channels SET isHidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: Long, hidden: Boolean)

    /**
     * Eski favori isaretini tasiyan kanallari yeni favorites tablosuna bagla.
     * Ilk senkron sonrasi cagrilir.
     */
    @Query("SELECT id FROM channels WHERE isFavorite = 1")
    suspend fun favoriteChannelIds(): List<Long>
}

@Dao
interface ProgramDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<ProgramEntity>)

    @Query("DELETE FROM programs WHERE channelId = :channelId")
    suspend fun deleteByChannel(channelId: Long)

    @Query("DELETE FROM programs WHERE tvgId = :tvgId")
    suspend fun deleteByTvgId(tvgId: String)

    /** EPG grid: belirli kanallar, zaman araligi. */
    @Query(
        """
        SELECT * FROM programs
        WHERE channelId IN (:channelIds)
          AND endEpoch > :from
          AND startEpoch < :to
        ORDER BY channelId ASC, startEpoch ASC
        """
    )
    fun observeRange(
        channelIds: List<Long>,
        from: Long,
        to: Long,
    ): Flow<List<ProgramEntity>>

    @Query(
        """
        SELECT * FROM programs
        WHERE channelId = :channelId
          AND endEpoch > :from
          AND startEpoch < :to
        ORDER BY startEpoch ASC
        """
    )
    fun observeForChannel(channelId: Long, from: Long, to: Long): Flow<List<ProgramEntity>>

    @Query(
        """
        SELECT * FROM programs
        WHERE channelId = :channelId
          AND startEpoch <= :time AND endEpoch > :time
        LIMIT 1
        """
    )
    fun observeCurrent(channelId: Long, time: Long): Flow<ProgramEntity?>

    @Query(
        """
        SELECT * FROM programs
        WHERE title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
        ORDER BY startEpoch DESC
        LIMIT :limit
        """
    )
    fun searchPrograms(query: String, limit: Int = 50): Flow<List<ProgramEntity>>

    /** Program aramasinda kanal adini da gostermek icin. */
    @Query("SELECT name FROM channels WHERE id = :id")
    suspend fun channelName(id: Long): String?

    @Query("SELECT COUNT(*) FROM programs")
    fun observeCount(): Flow<Int>

    @Query("SELECT MIN(startEpoch) FROM programs")
    suspend fun oldestStart(): Long?

    @Query("DELETE FROM programs")
    suspend fun clear()
}

@Dao
interface UserDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE channelId = :channelId")
    suspend fun removeFavorite(channelId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE channelId = :channelId)")
    fun isFavorite(channelId: Long): Flow<Boolean>

    @Query("SELECT channelId FROM favorites ORDER BY addedAt DESC")
    fun favoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE channelId = :channelId")
    suspend fun removeWatchlist(channelId: Long)

    @Query("SELECT channelId FROM watchlist ORDER BY addedAt DESC")
    fun watchlistIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE channelId = :channelId")
    suspend fun removeHistory(channelId: Long)

    @Query("SELECT * FROM history ORDER BY watchedAt DESC LIMIT :limit")
    fun observeHistory(limit: Int = 50): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE channelId = :channelId")
    suspend fun historyFor(channelId: Long): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecording(recording: RecordingEntity): Long

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecording(id: Long)

    @Query("SELECT * FROM recordings ORDER BY startedAt DESC")
    fun observeRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun recordingById(id: Long): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE isLocal = 1 AND startedAt < :before")
    suspend fun staleRecordings(before: Long): List<RecordingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchedule(schedule: ScheduleEntity): Long

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteSchedule(id: Long)

    @Query("SELECT * FROM schedules WHERE enabled = 1 ORDER BY startEpoch ASC")
    fun observeActiveSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules ORDER BY startEpoch ASC")
    fun observeAllSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE startEpoch BETWEEN :from AND :to AND enabled = 1")
    suspend fun schedulesBetween(from: Long, to: Long): List<ScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addLock(lock: ParentalLockEntity)

    @Query("DELETE FROM parental_locks WHERE channelId = :channelId")
    suspend fun removeLock(channelId: Long)

    @Query("SELECT channelId FROM parental_locks")
    fun lockedChannelIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM parental_locks WHERE channelId = :channelId)")
    fun isLocked(channelId: Long): Flow<Boolean>
}
