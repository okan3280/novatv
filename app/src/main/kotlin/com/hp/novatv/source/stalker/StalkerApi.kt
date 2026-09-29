package com.hp.novatv.source.stalker

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Stalker (MAG) portal API. Kurumsal MAC tabanli portal.
 *
 * Akis:
 *  1. handshake  -> jsessionid
 *  2. get_profile -> kullanici/MAC dogrulama
 *  3. get_live_categories
 *  4. get_ordered_list (get_all_channels=1)
 *  5. get_event (EPG)
 *  6. create_link (oynatma)
 */
class StalkerApi(
    private val baseUrl: String,
    private val portalId: String,
    private val macAddress: String,
    private val serialNumber: String = "",
    private val deviceId: String = "",
    private val httpClient: HttpPoster,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Volatile
    var jsessionId: String? = null
        private set

    @Volatile
    var serverTimeOffsetSec: Long = 0
        private set

    /** 1. Adim: portal ile toklas. */
    suspend fun handshake(): String {
        val payload = buildJsonObject {
            put("type", "stb")
            put("action", "handshake")
            put("token", "")
            put("jsessionid", "")
            putJsonObject("userData") {
                put("login", portalId)
                put("password", "0")
                put("mac", macHex(macAddress))
                put("serial", serialNumber)
                put("device", deviceId)
                put("device_id", deviceId)
            }
        }

        val response = httpClient.postJson(endpoint("handshake"), payload.toString())
        val error = response["error"].asString()
        if (!error.isNullOrBlank()) throw StalkerException.Auth("Handshake hatası: $error")

        val jsession = response["jsessionid"].asString()
        if (jsession.isNullOrBlank()) throw StalkerException.Auth("jsessionid alınamadı")
        jsessionId = jsession

        // Sunucu saat farki (timeshift icin gerekli)
        response["server_time"]?.asString()?.toLongOrNull()?.let { serverTime ->
            serverTimeOffsetSec = serverTime - System.currentTimeMillis() / 1000
        }
        return jsession
    }

    /** 2. Adim: profil dogrulama. */
    suspend fun getProfile(): StalkerProfile {
        val response = call("get_profile")
        val error = response["error"].asString()
        if (!error.isNullOrBlank()) throw StalkerException.Auth("Profil hatası: $error")

        return StalkerProfile(
            authorized = response["authorized"]?.asString()?.toIntOrNull() == 1,
            status = response["status"].asString().orEmpty(),
            login = response["login"].asString().orEmpty(),
            tariffPlan = response["tariff_plan"].asString().orEmpty(),
            expireBilled = response["expire_billed"].asString().orEmpty(),
        )
    }

    /** 3. Adim: canli kategorileri. */
    suspend fun getLiveCategories(): List<StalkerCategory> {
        val response = call("get_live_categories")
        val error = response["error"].asString()
        if (!error.isNullOrBlank()) throw StalkerException.Network("Kategori hatası: $error")

        return response["data"]
            ?.let { it as? kotlinx.serialization.json.JsonArray }
            ?.mapNotNull { item ->
                val o = item as? JsonObject ?: return@mapNotNull null
                StalkerCategory(
                    id = o["id"]?.asString().orEmpty(),
                    name = o["name"]?.asString().orEmpty(),
                )
            }
            .orEmpty()
    }

    /** 4. Adim: tum kanallar. */
    suspend fun getAllChannels(): List<StalkerChannel> {
        val response = call("get_ordered_list", buildJsonObject {
            put("get_all_channels", 1)
            put("fav", 0)
        })
        val error = response["error"].asString()
        if (!error.isNullOrBlank()) throw StalkerException.Network("Kanal listesi hatası: $error")

        return response["data"]
            ?.let { it as? kotlinx.serialization.json.JsonArray }
            ?.mapNotNull { item ->
                val o = item as? JsonObject ?: return@mapNotNull null
                StalkerChannel(
                    id = o["id"]?.asString().orEmpty(),
                    name = o["name"]?.asString().orEmpty(),
                    number = o["number"]?.asString()?.toIntOrNull() ?: 0,
                    logo = o["logo"].asString().orEmpty(),
                    categoryId = o["category_id"]?.asString().orEmpty(),
                    xmltvId = o["xmltv_id"]?.asString().orEmpty(),
                    isProtected = o["is_protected"]?.asString()?.toIntOrNull() == 1,
                    archiveDays = o["timeshift"]?.asString()?.let {
                        (it.toLongOrNull() ?: 0L) / 86_400L
                    }?.toInt() ?: 0,
                )
            }
            .orEmpty()
    }

    /** 5. Adim: EPG programlari. */
    suspend fun getEvent(channelId: String): List<StalkerEvent> {
        val response = call("get_event", buildJsonObject {
            put("movie_id", channelId)
        })
        val error = response["error"].asString()
        if (!error.isNullOrBlank()) return emptyList()

        val now = System.currentTimeMillis() / 1000 + serverTimeOffsetSec

        return response["data"]
            ?.let { it as? kotlinx.serialization.json.JsonArray }
            ?.mapNotNull { item ->
                val o = item as? JsonObject ?: return@mapNotNull null
                val from = o["start_timestamp"]?.asString()?.toLongOrNull() ?: return@mapNotNull null
                val to = o["finish_timestamp"]?.asString()?.toLongOrNull() ?: return@mapNotNull null
                // Gecmise donuk programlari ele
                if (to < now - 3 * 86_400L) return@mapNotNull null

                StalkerEvent(
                    startEpoch = from,
                    endEpoch = to,
                    name = o["name"].asString().orEmpty(),
                    description = o["descr"].asString().orEmpty(),
                    category = o["category"].asString().orEmpty(),
                )
            }
            .orEmpty()
    }

    /** 6. Adim: oynatilabilir URL. */
    suspend fun createLink(
        channelId: String,
        command: String = "",
        from: Long? = null,
        to: String? = null,
        series: String = "0",
    ): String? {
        val payload = buildJsonObject {
            put("cmd", "ffmpeg")
            put("jsessionid", jsessionId.orEmpty())
            put("server_denied_params", "")
            put("server_allowed_params", "")
            put("streamer", 1)
            put("timeshift", from ?: 0)
            putJsonObject("params") {
                put("redirect", "")
                put("from", to ?: "")
                put("force", "1")
                put("code", channelId)
            }
            put("custom_params", buildJsonObject {
                put("login", portalId)
                put("password", "0")
                put("mac", macHex(macAddress))
                put("serial", serialNumber)
                put("device", deviceId)
                put("device_id", deviceId)
            })
            if (command.isNotBlank()) put("fix_cmd", command)
            put("fix_rate", "0")
            put("series", series)
            put("forced_storage", 0)
        }

        val response = httpClient.postJson(endpoint("create_link"), payload.toString())
        response["cmd"]?.asString()?.let { if (it.isNotBlank()) return it }

        // create_link yoksa create_link/iptv yapisi
        response["url"]?.asString()?.let { if (it.isNotBlank()) return it }
        return null
    }

    // ------------------------------------------------------------------

    private suspend fun call(action: String, extra: JsonObject? = null): JsonObject {
        val payload = buildJsonObject {
            put("type", "stb")
            put("action", action)
            put("jsessionid", jsessionId.orEmpty())
            if (extra != null) {
                for ((k, v) in extra) put(k, v)
            }
        }
        val response = httpClient.postJson(endpoint(action), payload.toString())
        val error = response["error"].asString()
        if (!error.isNullOrBlank() && error != "null") {
            if (error.contains("not_auth") || error.contains("invalid_token")) {
                // Oturum dustu: yeniden handshake dene
                handshake()
                return call(action, extra)
            }
        }
        return response
    }

    private fun endpoint(action: String) =
        "${baseUrl.trimEnd('/')}/portal_api.php?action=$action&type=stb&token=&jsessionid="

    private fun macHex(mac: String): String =
        mac.replace(":", "").replace("-", "").uppercase().ifBlank { "00:00:00:00:00:00" }
}

internal fun kotlinx.serialization.json.JsonElement?.asString(): String? =
    (this as? JsonPrimitive)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.content

data class StalkerProfile(
    val authorized: Boolean,
    val status: String,
    val login: String,
    val tariffPlan: String,
    val expireBilled: String,
)

data class StalkerCategory(val id: String, val name: String)

data class StalkerChannel(
    val id: String,
    val name: String,
    val number: Int,
    val logo: String,
    val categoryId: String,
    val xmltvId: String,
    val isProtected: Boolean,
    val archiveDays: Int,
)

data class StalkerEvent(
    val startEpoch: Long,
    val endEpoch: Long,
    val name: String,
    val description: String,
    val category: String,
)

sealed class StalkerException(message: String) : Exception(message) {
    class Auth(message: String) : StalkerException(message)
    class Network(message: String) : StalkerException(message)
}

/** HTTP katmani; test icin degistirilebilir. */
fun interface HttpPoster {
    suspend fun postJson(url: String, body: String): JsonObject
}
