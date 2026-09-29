package com.hp.novatv.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val epgUrl: String = "",
    val logo: String = "",
    val portalId: String = "",
    val mac: String = "",
    val lastSync: Long = 0L,
    val channelCount: Int = 0,
)

@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("playlistId"),
        // Arama ve grup listesi bu indeksi kullanir.
        Index(value = ["playlistId", "group", "number"]),
        Index("tvgId"),
    ],
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val streamId: String,
    val number: Int = 0,
    val name: String,
    val logo: String = "",
    val group: String = "",
    val tvgId: String = "",
    val streamUrl: String = "",
    val catchupSource: String = "",
    val catchupAppend: String = "",
    val catchupVshift: String = "",
    val catchupDays: Int = 0,
    val timeshiftUrl: String = "",
    val epgOffset: Int = 0,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val isHidden: Boolean = false,
    val containerExtension: String = "",
)

@Entity(
    tableName = "programs",
    indices = [
        // EPG sorgusu: kanal + zaman araligi
        Index(value = ["channelId", "startEpoch"]),
        // XMLTV channel siralamasi
        Index(value = ["tvgId"]),
        Index("startEpoch"),
    ],
)
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: Long,
    /** Saglayicinin kanal kimligi (tvg-id). Kanal silinince temizlenmesi icin. */
    val tvgId: String = "",
    val startEpoch: Long,
    val endEpoch: Long,
    val title: String,
    val description: String = "",
    val category: String = "",
    val icon: String = "",
    val rating: String = "",
    val catchupUrl: String = "",
)

@Entity(
    tableName = "favorites",
    primaryKeys = ["channelId"],
    indices = [Index("addedAt")],
)
data class FavoriteEntity(
    val channelId: Long,
    val playlistId: Long,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "watchlist",
    primaryKeys = ["channelId"],
    indices = [Index("addedAt")],
)
data class WatchlistEntity(
    val channelId: Long,
    val playlistId: Long,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "history",
    primaryKeys = ["channelId"],
    indices = [Index("watchedAt")],
)
data class HistoryEntity(
    val channelId: Long,
    val playlistId: Long,
    val name: String,
    val logo: String = "",
    val watchedAt: Long = System.currentTimeMillis(),
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

@Entity(tableName = "recordings", indices = [Index("startedAt"), Index("channelId")])
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: Long,
    val playlistId: Long,
    val channelName: String,
    val title: String = "",
    val filePath: String,
    val startedAt: Long,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val isLocal: Boolean = true,
    val thumbnailPath: String = "",
)

@Entity(tableName = "schedules", indices = [Index("startEpoch"), Index("channelId")])
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: Long,
    val playlistId: Long,
    val channelName: String,
    val programTitle: String = "",
    val startEpoch: Long,
    val endEpoch: Long,
    val enabled: Boolean = true,
    val repeatDaily: Boolean = false,
)

/** Kanal kilidi (ebeveyn kontrolu). */
@Entity(tableName = "parental_locks", primaryKeys = ["channelId"])
data class ParentalLockEntity(
    val channelId: Long,
    val playlistId: Long,
    val lockedAt: Long = System.currentTimeMillis(),
)
