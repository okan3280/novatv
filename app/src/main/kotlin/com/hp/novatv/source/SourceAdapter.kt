package com.hp.novatv.source

import com.hp.novatv.core.model.Channel
import com.hp.novatv.core.model.Playlist
import com.hp.novatv.core.model.Program
import com.hp.novatv.core.model.SyncResult
import kotlinx.coroutines.flow.Flow

/**
 * Ortak kaynak arayuzu. Her adaptor ayni sozlesmeyi uygular; UI katmani
 * tur farkinda bilmez.
 */
interface SourceAdapter {

    val playlist: Playlist

    /** Kanallari getirir. Buyuk listelerde sayfali/streaming tercih edilir. */
    suspend fun loadChannels(): List<Channel>

    /**
     * EPG'yi doldurur. M3U'da [epgUrl] XMLTV'den okunur; Xtream'da xmltv.php;
     * Stalker'da get_event ile program listesi.
     */
    suspend fun loadPrograms(channels: List<Channel>): Flow<List<Program>>

    /**
     * Oynatilabilir URL. [startEpoch]/[duration] catch-up icin kullanilir;
     * normal oynatmada null verilir.
     */
    suspend fun resolvePlayUrl(
        channel: Channel,
        startEpoch: Long? = null,
        durationMs: Long? = null,
    ): String

    /** Kanali kaydet komutu (DVR). Saglayici desteklemiyorsa false. */
    suspend fun providerRecord(channel: Channel, startEpoch: Long, durationMs: Long): Boolean

    /** Zaman kaydirma (timeshift) URL'si; yoksa null. */
    suspend fun resolveTimeshiftUrl(channel: Channel, positionMs: Long): String?

    /** Kanal kapat. Adaptorler kaynak tutuyorsa serbest birakir. */
    suspend fun close() {}
}

/** Adaptor secim hatalari. */
sealed class SourceException(message: String) : Exception(message) {
    class Network(message: String) : SourceException(message)
    class Auth(message: String) : SourceException(message)
    class Parse(message: String) : SourceException(message)
    class Unsupported(message: String) : SourceException(message)
}

/** Sonuc kisayollari. */
typealias SyncOutcome = SyncResult
