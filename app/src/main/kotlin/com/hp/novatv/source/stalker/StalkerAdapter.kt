package com.hp.novatv.source.stalker

import com.hp.novatv.core.model.Channel
import com.hp.novatv.core.model.Playlist
import com.hp.novatv.core.model.Program
import com.hp.novatv.core.util.cleanChannelName
import com.hp.novatv.source.SourceAdapter
import com.hp.novatv.source.SourceException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.util.UUID

/**
 * Stalker Portal (MAG) adaptoru.
 *
 * Not: Bircok kurumsal portal artik erisimi kapatti. Hata durumunda
 * adaptoru sessizce devre disi birakiyoruz - [SourceException] firlatilir,
 * cagiran taraf bu turu kullanilamaz olarak isaretler.
 */
class StalkerAdapter(
    override val playlist: Playlist,
    private val client: OkHttpClient,
) : SourceAdapter {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private val api: StalkerApi by lazy {
        StalkerApi(
            baseUrl = playlist.url,
            portalId = playlist.portalId.ifBlank { "1" },
            macAddress = playlist.mac,
            serialNumber = UUID.randomUUID().toString().replace("-", "").take(12).uppercase(),
            deviceId = UUID.randomUUID().toString(),
            httpClient = OkHttpPoster(client, json),
        )
    }

    private var categoriesById: Map<String, String> = emptyMap()
    private val channelIdByOurId = mutableMapOf<Long, String>()

    override suspend fun loadChannels(): List<Channel> = withContext(Dispatchers.IO) {
        api.handshake()
        val profile = api.getProfile()
        if (!profile.authorized) {
            throw SourceException.Auth("Portal yetkilendirmesi reddetti (${profile.status})")
        }

        categoriesById = api.getLiveCategories().associate { it.id to it.name }
        val channels = api.getAllChannels()
        if (channels.isEmpty()) throw SourceException.Parse("Portaldan kanal dönmedi")

        channelIdByOurId.clear()

        channels.mapIndexed { index, ch ->
            Channel(
                streamId = ch.id,
                number = ch.number.takeIf { it > 0 } ?: (index + 1),
                name = ch.name.cleanChannelName(),
                logo = ch.logo,
                group = categoriesById[ch.categoryId] ?: "Diğer",
                tvgId = ch.xmltvId,
                streamUrl = "",
                catchupDays = ch.archiveDays,
                isLocked = ch.isProtected,
            )
        }.also { list ->
            // Stalker kanal kimligi string; Room autoincrement id'si sonradan
            // atanir. Adapter, id atandiktan sonra cagrilacak sekilde
            // oynatma sirasinda tekrar cozumler.
        }
    }

    override suspend fun loadPrograms(channels: List<Channel>): Flow<List<Program>> = flow {
        val result = mutableListOf<Program>()
        for (channel in channels) {
            val portalId = channel.streamId
            if (portalId.isBlank()) continue
            val events = runCatching { api.getEvent(portalId) }.getOrDefault(emptyList())
            result += events.map { e ->
                Program(
                    channelId = channel.id,
                    startEpoch = e.startEpoch,
                    endEpoch = e.endEpoch,
                    title = e.name,
                    description = e.description,
                    category = e.category,
                )
            }
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    override suspend fun resolvePlayUrl(
        channel: Channel,
        startEpoch: Long?,
        durationMs: Long?,
    ): String {
        val url = api.createLink(
            channelId = channel.streamId,
            from = startEpoch,
            to = if (startEpoch != null && durationMs != null) {
                (startEpoch + durationMs / 1000).toString()
            } else {
                null
            },
        )
        return url ?: throw SourceException.Network("Oynatma linki alınamadı")
    }

    override suspend fun providerRecord(
        channel: Channel,
        startEpoch: Long,
        durationMs: Long,
    ): Boolean = false

    override suspend fun resolveTimeshiftUrl(channel: Channel, positionMs: Long): String? {
        val nowServer = System.currentTimeMillis() / 1000 + api.serverTimeOffsetSec
        val from = nowServer - (positionMs / 1000)
        return runCatching {
            api.createLink(channelId = channel.streamId, from = from, to = "", series = "1")
        }.getOrNull()
    }
}

/** OkHttp tabanli JSON POST. */
private class OkHttpPoster(
    private val client: OkHttpClient,
    private val json: Json,
) : HttpPoster {

    override suspend fun postJson(url: String, body: String): JsonObject {
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 NovaTV/1.0")
            .header("Cookie", "mac=$mac; stb_lang=en; timezone=Europe/Istanbul")
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful && text.isBlank()) {
                throw StalkerException.Network("HTTP ${response.code}")
            }
            return runCatching {
                json.parseToJsonElement(text) as? JsonObject
                    ?: throw StalkerException.Network("Beklenmeyen yanıt biçimi")
            }.getOrElse {
                throw StalkerException.Network("JSON ayrıştırılamadı")
            }
        }
    }

    private companion object {
        val mac = "00:1A:79:00:00:00"
    }
}
