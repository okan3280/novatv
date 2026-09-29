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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * M3U adaptoru.
 *
 * Oynatma URL'si dogrudan listeden gelir. Catch-up, `catchup-source`
 * sablonundaki {start} / {duration} yer tutuculari ile uretilir.
 */
class M3uAdapter(
    override val playlist: Playlist,
    private val client: OkHttpClient,
) : SourceAdapter {

    override suspend fun loadChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val body = fetchText(playlist.url)
            ?: throw SourceException.Network("M3U indirilemedi")

        val channels = M3uParser().parseToList(body, playlist.url)
        if (channels.isEmpty()) {
            throw SourceException.Parse("M3U icinde kanal bulunamadi")
        }
        channels
    }

    override suspend fun loadPrograms(channels: List<Channel>): Flow<List<Program>> = flow {
        val epgUrl = playlist.epgUrl
        if (epgUrl.isBlank()) return@flow

        val parser = XmltvParser()
        val byTvg = channels.filter { it.tvgId.isNotEmpty() }.associateBy { it.tvgId }
        val collected = mutableListOf<Program>()

        val request = Request.Builder().url(epgUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw SourceException.Network("EPG indirilemedi: ${response.code}")
            }
            val stream = response.body?.byteStream()
                ?: throw SourceException.Network("EPG govdesi bos")

            parser.parse(stream) { tvgId, programs ->
                val channel = byTvg[tvgId] ?: return@parse
                collected += programs.map { it.copy(channelId = channel.id, tvgId = tvgId) }
            }
        }

        val from = startOfDay() - CATCHUP_WINDOW_DAYS * 86_400_000L
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

        val durationSec = (durationMs / 1000L)
        val startLocal = formatStamp(startEpoch, utc = false)
        val startUtc = formatStamp(startEpoch, utc = true)

        return source
            .replace("{start}", startLocal)
            .replace("{utc}", startUtc)
            .replace("{timestamp}", startEpoch.toString())
            .replace("{duration}", durationSec.toString())
            .replace("{end}", (startEpoch + durationSec).toString())
    }

    override suspend fun providerRecord(
        channel: Channel,
        startEpoch: Long,
        durationMs: Long,
    ): Boolean = false

    override suspend fun resolveTimeshiftUrl(channel: Channel, positionMs: Long): String? {
        val template = channel.timeshiftUrl
        if (template.isBlank()) return null
        return template
            .replace("{duration}", positionMs.toString())
            .replace("{start}", startOfDay().toString())
    }

    private fun formatStamp(epochSeconds: Long, utc: Boolean): String =
        SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US).apply {
            timeZone = if (utc) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
        }.format(Date(epochSeconds * 1000))

    private fun fetchText(url: String): String? = try {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }
    } catch (e: IOException) {
        null
    } catch (e: IllegalArgumentException) {
        // Gecersiz URL
        null
    }

    private companion object {
        const val CATCHUP_WINDOW_DAYS = 7
    }
}
