package com.hp.novatv.source.m3u

import com.hp.novatv.core.model.Channel
import com.hp.novatv.core.model.Playlist
import com.hp.novatv.core.model.Program
import com.hp.novatv.core.util.startOfDay
import com.hp.novatv.source.SourceAdapter
import com.hp.novatv.source.SourceException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Calendar
import java.util.TimeZone

/**
 * M3U adaptoru. Oynatma URL'si dogrudan dosyadan gelir; catch-up
 * catchup-source sablonu ile uretilir.
 */
class M3uAdapter(
    override val playlist: Playlist,
    private val client: OkHttpClient,
) : SourceAdapter {

    override suspend fun loadChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val body = fetchText(playlist.url) ?: throw SourceException.Network("M3U indirilemedi")
        val channels = M3uParser().parseToList(body, playlist.url)
        if (channels.isEmpty()) throw SourceException.Parse("M3U iÃ§inde kanal bulunamadÄ±")
        channels
    }

    override suspend fun loadPrograms(channels: List<Channel>): Flow<List<Program>> = flow {
        val epgUrl = playlist.epgUrl.ifBlank { null }
        if (epgUrl == null) return@flow

        val parser = XmltvParser()
        // tvgId -> kanal eslemesi
        val byTvg = channels.filter { it.tvgId.isNotEmpty() }
            .associateBy { it.tvgId }

        val collected = mutableListOf<Program>()
        client.newCall(Request.Builder().url(epgUrl).build()).execute().use { response ->
            if (!response.isSuccessful) throw SourceException.Network("EPG indirilemedi: ${response.code}")
            val stream = response.body?.byteStream()
                ?: throw SourceException.Network("EPG gÃ¶vdesi boÅŸ")
            parser.parse(stream) { tvgId, programs ->
                val channel = byTvg[tvgId] ?: return@parse
                collected += programs.map { it.copy(channelId = channel.id) }
            }
        }

        val from = startOfDay() - (playlistCatchupWindow() * 86_400_000L)
        emit(collected.filter { it.endEpoch >= from })
    }.flowOn(Dispatchers.IO)

    override suspend fun resolvePlayUrl(
        channel: Channel,
        startEpoch: Long?,
        durationMs: Long?,
    ): String {
        if (startEpoch == null || durationMs == null) return channel.streamUrl

        val source = channel.catchupSource
        if (source.isBlank()) return channel.streamUrl

        val durationSec = (durationMs / 1000L).toInt()
        return when {
            // {start} / {duration} yer tutuculari
            source.contains("{start}") || source.contains("{utc}") ||
                source.contains("{timestamp}") -> {
                val ts = if (source.contains("{utc}")) {
                    SimpleUtc(startEpoch)
                } else {
                    SimpleLocal(startEpoch)
                }
                source
                    .replace("{start}", ts.value)
                    .replace("{utc}", ts.value)
                    .replace("{timestamp}", (startEpoch).toString())
                    .replace("{duration}", durationSec.toString())
                    .replace("{end}", (startEpoch + durationMs / 1000L).toString())
            }

            // Ekleme (append) sekli
            source.isBlank() -> channel.streamUrl

            else -> source
        }
    }

    override suspend fun providerRecord(
        channel: Channel,
        startEpoch: Long,
        durationMs: Long,
    ): Boolean = false

    override suspend fun resolveTimeshiftUrl(channel: Channel, positionMs: Long): String? {
        val ts = channel.timeshiftUrl.ifBlank { return null }
        // TiviMate uyumu: {duration} ms cinsinden geriye gitme
        return ts.replace("{duration}", positionMs.toString())
            .replace("{start}", startOfDay().toString())
    }

    private fun playlistCatchupWindow(): Int = 7

    private fun fetchText(url: String): String? = try {
        client.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) null else r.body?.string()
        }
    } catch (e: IOException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private class SimpleUtc(epoch: Long) {
        val value: String = java.text.SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(java.util.Date(epoch * 1000))
    }

    private class SimpleLocal(epoch: Long) {
        val value: String = java.text.SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US)
            .format(java.util.Date(epoch * 1000))
    }
}
