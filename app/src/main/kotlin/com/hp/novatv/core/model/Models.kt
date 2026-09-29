package com.hp.novatv.core.model

import kotlinx.serialization.Serializable

/** Playlist turu. */
@Serializable
enum class PlaylistType { M3U, XTREAM, STALKER }

/**
 * Bir kaynak listesi. Sifre bilgileri DB'de duz metin tutulur; cihaz
 * root'suz erisime kapali oldugu icin uygulama ici sifreleme yerine
 * yedeklemede bu alanlar haric birakilir.
 */
data class Playlist(
    val id: Long = 0,
    val name: String,
    val type: PlaylistType,
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val epgUrl: String = "",
    val logo: String = "",
    /** Stalker portal icin: portal_id + MAC. */
    val portalId: String = "",
    val mac: String = "",
    val lastSync: Long = 0L,
    val channelCount: Int = 0,
) {
    val isSynced: Boolean get() = lastSync > 0L
}

/** Bir kanal. */
data class Channel(
    val id: Long = 0,
    val playlistId: Long,
    /** Saglayici tarafindaki stabil kimlik (Xtream stream_id, M3U satir no, Stalker cmd). */
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
) {
    /** M3U satirinda veya API'de gelen ham uzantidan tur. */
    val streamType: StreamType get() = StreamType.fromUrl(streamUrl)
}

/** Oynatilabilir akis turu. */
enum class StreamType(val label: String) {
    HLS("HLS"),
    DASH("DASH"),
    PROGRESSIVE("Progressive"),
    TS("MPEG-TS"),
    RTSP("RTSP"),
    RTMP("RTMP"),
    KODI("Kodi"),
    UNKNOWN("Bilinmiyor");

    companion object {
        fun fromUrl(url: String): StreamType = when {
            url.contains(".m3u8", ignoreCase = true) -> HLS
            url.contains(".mpd", ignoreCase = true) -> DASH
            url.startsWith("rtsp", ignoreCase = true) -> RTSP
            url.startsWith("rtmp", ignoreCase = true) -> RTMP
            url.contains("/kodi", ignoreCase = true) -> KODI
            url.contains(".ts", ignoreCase = true) -> TS
            url.endsWith(".mp4", ignoreCase = true) ||
                url.endsWith(".mkv", ignoreCase = true) ||
                url.endsWith(".avi", ignoreCase = true) -> PROGRESSIVE
            else -> UNKNOWN
        }
    }
}

/** EPG programi. */
data class Program(
    val id: Long = 0,
    val channelId: Long,
    val startEpoch: Long,
    val endEpoch: Long,
    val title: String,
    val description: String = "",
    val category: String = "",
    val icon: String = "",
    val rating: String = "",
    val catchupUrl: String = "",
) {
    val durationMs: Long get() = (endEpoch - startEpoch) * 1000L
}

/** Oynatma durumu gecmisi. */
data class HistoryEntry(
    val channelId: Long,
    val playlistId: Long,
    val name: String,
    val logo: String = "",
    val watchedAt: Long,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

/** Kayit tanimi. */
data class Recording(
    val id: Long = 0,
    val channelId: Long,
    val playlistId: Long,
    val channelName: String,
    val title: String = "",
    val filePath: String,
    val startedAt: Long,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    /** true = yerel MP4, false = saglayici tarafi kaydi. */
    val isLocal: Boolean = true,
    val thumbnailPath: String = "",
)

/** Zamanlanmis kayit. */
data class ScheduledRecording(
    val id: Long = 0,
    val channelId: Long,
    val playlistId: Long,
    val channelName: String,
    val programTitle: String = "",
    val startEpoch: Long,
    val endEpoch: Long,
    val enabled: Boolean = true,
    val repeatDaily: Boolean = false,
)

/** Multiview karti. */
data class MultiViewSlot(
    val index: Int,
    val channelId: Long? = null,
    val playlistId: Long? = null,
    val channelName: String = "",
    val logo: String = "",
    val expanded: Boolean = false,
)

/** Arama sonucu. */
sealed interface SearchResult {
    data class ChannelHit(val channel: Channel) : SearchResult
    data class ProgramHit(val program: Program, val channelName: String) : SearchResult
}

/** Ana ekranda gosterilen grup. */
data class ChannelGroup(
    val title: String,
    val channels: List<Channel>,
    val totalCount: Int,
)

/** Bir playlistin senkron sonucu. */
data class SyncResult(
    val playlistId: Long,
    val channelCount: Int = 0,
    val programCount: Int = 0,
    val error: String? = null,
) {
    val success: Boolean get() = error == null
}
