package com.hp.novatv.source.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Xtream player_api.php yanitlari. */

@Serializable
data class AuthResponse(
    @SerialName("user_info") val userInfo: UserInfo? = null,
    @SerialName("server_info") val serverInfo: ServerInfo? = null,
    @SerialName("auth") val auth: Int = 0,
)

@Serializable
data class UserInfo(
    val username: String = "",
    val password: String = "",
    val message: String = "",
    val auth: Int = 0,
    val status: String = "",
)

@Serializable
data class ServerInfo(
    val url: String = "",
    val port: String = "",
    val https_port: String = "",
    val server_protocol: String = "http",
    val rtmp_port: String = "",
    val timezone: String = "",
    val time_now: String = "",
    val process: Boolean = true,
)

@Serializable
data class Category(
    val category_id: String = "",
    val category_name: String = "",
    val parent_id: Int = 0,
)

@Serializable
data class LiveStream(
    val num: Int = 0,
    val name: String = "",
    val stream_id: Int = 0,
    val stream_icon: String = "",
    val epg_channel_id: String = "",
    val added: String = "",
    val category_id: String = "",
    val custom_sid: String = "",
    val tv_archive: Int = 0,
    val direct_source: String = "",
    val tv_archive_duration: Int = 0,
)

@Serializable
data class LiveStreamDetails(
    val stream_id: Int = 0,
    val name: String = "",
    val stream_icon: String = "",
    val epg_channel_id: String = "",
    val category_id: String = "",
    val tv_archive: Int = 0,
    val tv_archive_duration: Int = 0,
    val container_extension: String = "ts",
    val custom_sid: String = "",
    val direct_source: String = "",
    /** Yonlendirmeli oynatmada dogrudan URL. */
    val direct_source_properties: DirectSourceProperties? = null,
    /** timeshift_url doluysa kaydirilabilir yayin. */
    val timeshift_url: String = "",
)

@Serializable
data class DirectSourceProperties(
    val container_extension: String = "",
    val video_codec: String = "",
    val audio_codec: String = "",
)

@Serializable
data class SimpleDataTable(
    val name: String = "",
    val value: String = "",
    val stream_id: Int = 0,
    val category_id: String = "",
    val stream_icon: String = "",
    val epg_channel_id: String = "",
    val added: String = "",
    val timeshift: String = "",
)

/** Kolaylik yardimcilari. */
internal object XtreamJson {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    /**
     * Xtream bazi uçlarda [] yerine {"user_info":...} dondurebilir ya da
     * tam sayi dondurebilir (auth kontrolu). Guvenli okuma.
     */
    fun parseAuth(body: String): AuthResponse {
        val element = json.parseToJsonElement(body)
        return when (element) {
            is JsonObject -> {
                val auth = element["user_info"]?.let {
                    (it as? JsonObject)?.get("auth")?.jsonPrimitive?.intOrZeroSafe() ?: 0
                } ?: element["auth"]?.jsonPrimitive?.intOrZeroSafe() ?: 0
                AuthResponse(
                    userInfo = (element["user_info"] as? JsonObject)?.let { ui ->
                        UserInfo(
                            username = ui.str("username"),
                            password = ui.str("password"),
                            message = ui.str("message"),
                            auth = ui["auth"]?.jsonPrimitive?.intOrZeroSafe() ?: 0,
                            status = ui.str("status"),
                        )
                    },
                    serverInfo = (element["server_info"] as? JsonObject)?.let { si ->
                        ServerInfo(
                            url = si.str("url"),
                            port = si.str("port"),
                            https_port = si.str("https_port"),
                            server_protocol = si.str("server_protocol").ifBlank { "http" },
                            rtmp_port = si.str("rtmp_port"),
                            timezone = si.str("timezone"),
                            time_now = si.str("time_now"),
                        )
                    },
                    auth = auth,
                )
            }

            is kotlinx.serialization.json.JsonPrimitive -> AuthResponse(
                auth = element.content.toIntOrNull() ?: 0,
            )

            else -> AuthResponse()
        }
    }

    fun parseCategories(body: String): List<Category> {
        val element = json.parseToJsonElement(body)
        val arr = element as? kotlinx.serialization.json.JsonArray ?: return emptyList()
        return arr.mapNotNull { item ->
            val o = item as? JsonObject ?: return@mapNotNull null
            Category(
                category_id = o.str("category_id"),
                category_name = o.str("category_name"),
                parent_id = o["parent_id"]?.jsonPrimitive?.intOrZeroSafe() ?: 0,
            )
        }
    }

    fun parseStreams(body: String): List<LiveStream> {
        val element = json.parseToJsonElement(body)
        val arr = element as? kotlinx.serialization.json.JsonArray ?: return emptyList()
        return arr.mapNotNull { item ->
            val o = item as? JsonObject ?: return@mapNotNull null
            LiveStream(
                num = o["num"]?.jsonPrimitive?.intOrZeroSafe() ?: 0,
                name = o.str("name"),
                stream_id = o["stream_id"]?.jsonPrimitive?.intOrZeroSafe() ?: return@mapNotNull null,
                stream_icon = o.str("stream_icon"),
                epg_channel_id = o.str("epg_channel_id"),
                added = o.str("added"),
                category_id = o.str("category_id"),
                custom_sid = o.str("custom_sid"),
                tv_archive = o["tv_archive"]?.jsonPrimitive?.intOrZeroSafe() ?: 0,
                direct_source = o.str("direct_source"),
                tv_archive_duration = o["tv_archive_duration"]?.jsonPrimitive?.intOrZeroSafe() ?: 0,
            )
        }
    }

    private fun JsonObject.str(key: String): String =
        this[key]?.jsonPrimitive?.contentOrEmptySafe() ?: ""

    private fun JsonPrimitive.contentOrEmptySafe(): String =
        if (this is kotlinx.serialization.json.JsonNull) "" else content

    private fun JsonPrimitive.intOrZeroSafe(): Int =
        content.trim().toIntOrNull() ?: 0
}
