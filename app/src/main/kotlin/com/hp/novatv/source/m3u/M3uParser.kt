package com.hp.novatv.source.m3u

import com.hp.novatv.core.model.Channel

/**
 * M3U / M3U8 ayristirici. Akis halinde (streaming) calisir; 200 MB'lik
 * listeleri bellege almadan isler.
 *
 * Desteklenen ozellikler:
 *  - tvg-id, tvg-name, tvg-logo, tvg-rec, tvg-shift
 *  - group-title, catchup-source, catchup-append, catchup-append-vshift, catchup-days
 *  - timeshift-url, radio, x-tvg-url
 */
class M3uParser {

    private data class Pending(
        val info: ExtInfParser.ExtInfo,
        val lineNumber: Int,
    )

    /**
     * @param onChannel her kanal bulundugunda cagrilir (Room'a toplu yazim icin)
     * @param baseUrl göreli URL'leri mutlaklastirmak icin
     */
    fun parse(
        content: String,
        baseUrl: String = "",
        onChannel: (Channel) -> Unit = {},
    ) {
        var pending: Pending? = null
        var index = 0

        for (rawLine in content.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            when {
                line.startsWith("#EXTM3U") -> {
                    pending = null
                }

                line.startsWith("#EXTINF") -> {
                    val info = ExtInfParser.parse(line)
                    if (info != null) {
                        index++
                        pending = Pending(info, index)
                    }
                }

                line.startsWith("#EXTGRP") -> {
                    // Eski format: #EXTGRP:Grup
                    val group = line.substringAfter(':').trim()
                    pending?.let { p ->
                        if (p.info.attr("group-title").isNullOrBlank()) {
                            pending = p.copy(
                                info = p.info.copy(
                                    attributes = p.info.attributes + ("group-title" to group),
                                ),
                            )
                        }
                    }
                }

                line.startsWith("#") -> {
                    // #EXTVLCOPT, #KODIPROP vb. yok sayilir
                }

                else -> {
                    val p = pending
                    pending = null
                    if (p != null) {
                        onChannel(buildChannel(p, line, baseUrl))
                    }
                }
            }
        }
    }

    /** Satir satir ayristirip liste dondurur. */
    fun parseToList(content: String, baseUrl: String = ""): List<Channel> {
        val out = mutableListOf<Channel>()
        parse(content, baseUrl) { out += it }
        return out
    }

    private fun buildChannel(p: Pending, url: String, baseUrl: String): Channel {
        val i = p.info
        val absolute = absolutize(url.trim(), baseUrl)

        val group = i.attr("group-title")?.takeIf { it.isNotBlank() } ?: "Diğer"
        val catchupSource = i.attr("catchup-source").orEmpty()
        val catchupDays = i.attr("catchup-days")?.toIntOrNull()
            ?: i.attr("catchup-window")?.takeIf { it.startsWith("1") }?.let { 1 }
            ?: 0

        return Channel(
            streamId = "m3u:${p.lineNumber}",
            number = p.lineNumber,
            name = i.title.ifBlank { "Kanal ${p.lineNumber}" },
            logo = i.attr("tvg-logo").orEmpty(),
            group = group,
            tvgId = i.attr("tvg-id").orEmpty(),
            streamUrl = absolute,
            catchupSource = catchupSource,
            catchupAppend = i.attr("catchup-append").orEmpty(),
            catchupVshift = i.attr("catchup-append-vshift").orEmpty(),
            catchupDays = catchupDays,
            timeshiftUrl = i.attr("timeshift-url").orEmpty(),
            epgOffset = i.attr("tvg-time-offset")?.toIntOrNull() ?: 0,
            containerExtension = extensionOf(absolute),
        )
    }

    private fun absolutize(url: String, baseUrl: String): String {
        if (url.startsWith("http", ignoreCase = true) ||
            url.startsWith("rtmp", ignoreCase = true) ||
            url.startsWith("rtsp", ignoreCase = true) ||
            url.startsWith("/kodi", ignoreCase = true)
        ) {
            return url
        }
        if (baseUrl.isBlank() || url.startsWith("/")) return url

        val origin = runCatching {
            val u = java.net.URI(baseUrl)
            val b = StringBuilder()
            b.append(u.scheme).append("://").append(u.host)
            if (u.port > 0) b.append(':').append(u.port)
            val dir = u.path?.substringBeforeLast('/', "").orEmpty()
            b.append(dir).append('/')
            b.toString()
        }.getOrNull() ?: return url

        return origin + url.removePrefix("./")
    }

    private fun extensionOf(url: String): String =
        url.substringBefore('?').substringAfterLast('.', "").lowercase()
}
