package com.hp.novatv.source.xtream

import com.hp.novatv.core.model.Channel
import com.hp.novatv.core.model.Playlist
import com.hp.novatv.core.model.Program
import com.hp.novatv.core.util.startOfDay
import com.hp.novatv.source.SourceAdapter
import com.hp.novatv.source.SourceException
import com.hp.novatv.source.m3u.XmltvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale

/**
 * Xtream Codes (panel) adaptoru.
 *
 * Ucler:
 *  {base}/player_api.php?username=&password=
 *  {base}/player_api.php?username=&password=&action=get_live_categories
 *  {base}/player_api.php?username=&password=&action=get_live_streams&category_id=
 *  {base}/player_api.php?username=&password=&action=get_simple_data_table&stream_id=
 *  {base}/player_api.php?username=&password=&action=get_epg&stream_id=&duration=
 *  {base}/get.php?username=&password=&stream=&output=
 *  {base}/xmltv.php?username=&password=
 */
class XtreamAdapter(
    override val playlist: Playlist,
    private val client: OkHttpClient,
) : SourceAdapter {

    private var serverInfo: ServerInfo? = null
    private var categories: Map<String, String> = emptyMap()

    /** Panel taban adresi (port ile birlikte). */
    private val baseUrl: String by lazy {
        val raw = playlist.url.trimEnd('/')
        // Kullanici genelde "http://sunucu:8080" yazar; bazi portal "kullanici/pass"
        // eklemis olabilir, temizle.
        raw.substringBefore("/player_api.php").substringBefore("/get.php")
    }

    override suspend fun loadChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val auth = authenticate()
        categories = XtreamJson.parseCategories(
            get("player_api.php?username=${enc(playlist.username)}&password=${enc(playlist.password)}&action=get_live_categories"),
        ).associate { it.category_id to it.category_name }

        val body = get(
            "player_api.php?username=${enc(playlist.username)}&password=${enc(playlist.password)}&action=get_live_streams"
        )
        val streams = XtreamJson.parseStreams(body)
        if (streams.isEmpty()) {
            throw SourceException.Parse("Xtream panelden kanal donmedi")
        }

        streams.map { s ->
            val group = categories[s.category_id] ?: DEFAULT_GROUP
            Channel(
                streamId = s.stream_id.toString(),
                number = s.num,
                name = s.name,
                logo = s.stream_icon,
                group = group,
                tvgId = s.epg_channel_id,
                streamUrl = buildPlayUrl(s.stream_id, container = containerOf(s)),
                catchupDays = if (s.tv_archive == 1) s.tv_archive_duration else 0,
                timeshiftUrl = "",
                containerExtension = containerOf(s),
            )
        }.sortedBy { it.number }
    }

    override suspend fun loadPrograms(channels: List<Channel>): Flow<List<Program>> = flow {
        val xmltvUrl = playlist.epgUrl.ifBlank {
            buildString {
                append(baseUrl)
                append("/xmltv.php?username=")
                append(enc(playlist.username))
                append("&password=")
                append(enc(playlist.password))
            }
        }

        val byTvg = channels.filter { it.tvgId.isNotEmpty() }.associateBy { it.tvgId }
        val collected = mutableListOf<Program>()

        val request = Request.Builder().url(xmltvUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw SourceException.Network("EPG indirilemedi: ${response.code}")
            val stream = response.body?.byteStream()
                ?: throw SourceException.Network("EPG govdesi bos")

            XmltvParser().parse(stream) { tvgId, programs ->
                val channel = byTvg[tvgId] ?: return@parse
                collected += programs.map { it.copy(channelId = channel.id, tvgId = tvgId) }
            }
        }

        val from = startOfDay() - 7 * 86_400_000L
        emit(collected.filter { it.endEpoch >= from })
    }.flowOn(Dispatchers.IO)

    override suspend fun resolvePlayUrl(
        channel: Channel,
        startEpoch: Long?,
        durationMs: Long?,
    ): String {
        val streamId = channel.streamId.toIntOrNull()
            ?: return channel.streamUrl

        // Yonlendirme (output=) yerine dogrudan extension'li URL: sunucu
        // 302 atmaz, ExoPlayer/VLC uzerinde daha guvenilir.
        val container = channel.containerExtension.ifBlank { "ts" }

        return if (startEpoch != null && durationMs != null) {
            // Xtream catch-up: start & duration (saniye)
            val startSec = startEpoch
            val durSec = durationMs / 1000
            "$baseUrl/streaming/timeshift.php" +
                "?username=${enc(playlist.username)}" +
                "&password=${enc(playlist.password)}" +
                "&stream=$streamId" +
                "&start=$startSec" +
                "&duration=$durSec" +
                "&output=$container"
        } else {
            buildPlayUrl(streamId, container)
        }
    }

    override suspend fun providerRecord(
        channel: Channel,
        startEpoch: Long,
        durationMs: Long,
    ): Boolean = false

    override suspend fun resolveTimeshiftUrl(channel: Channel, positionMs: Long): String? {
        val streamId = channel.streamId.toIntOrNull() ?: return null
        // timeshift.php start = "simdi - position", duration = kalan
        val startSec = System.currentTimeMillis() / 1000 - (positionMs / 1000)
        return "$baseUrl/streaming/timeshift.php" +
            "?username=${enc(playlist.username)}" +
            "&password=${enc(playlist.password)}" +
            "&stream=$streamId" +
            "&start=$startSec" +
            "&duration=0" +
            "&output=${channel.containerExtension.ifBlank { "ts" }}"
    }

    // ------------------------------------------------------------------

    private suspend fun authenticate(): ServerInfo {
        serverInfo?.let { return it }

        val body = get("player_api.php?username=${enc(playlist.username)}&password=${enc(playlist.password)}")
        val auth = XtreamJson.parseAuth(body)
        val info = auth.serverInfo
        if (info == null || auth.userInfo?.auth != 1) {
            throw SourceException.Auth(auth.userInfo?.message ?: "Xtream giriÅŸi baÅŸarÄ±sÄ±z")
        }
        serverInfo = info
        return info
    }

    private fun buildPlayUrl(streamId: Int, container: String): String =
        "$baseUrl/live/${playlist.username}/${playlist.password}/$streamId.$container"

    private fun containerOf(stream: LiveStream): String =
        if (stream.direct_source.isNotBlank()) stream.direct_source else "ts"

    private fun get(path: String): String {
        val url = "$baseUrl/$path"
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { r ->
                if (!r.isSuccessful) throw SourceException.Network("HTTP ${r.code}: $path")
                r.body?.string() ?: throw SourceException.Network("BoÅŸ yanÄ±t: $path")
            }
        } catch (e: IOException) {
            throw SourceException.Network(e.message ?: "AÄŸ hatasÄ±")
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private companion object {
        const val DEFAULT_GROUP = "Diger"
    }
}
