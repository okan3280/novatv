package com.hp.novatv.source.m3u

import com.hp.novatv.core.util.cleanChannelName

/**
 * #EXTINF satirindaki ozellikleri ayristirir.
 * Ornek: #EXTINF:-1 tvg-id="tr.spor.tv" tvg-logo="http://..." group-title="Spor", Spor TV
 */
internal object ExtInfParser {

    private val ATTR_REGEX = Regex("""([\w-]+)\s*=\s*"([^"]*)\"""")
    private val ATTR_UNQUOTED = Regex("""([\w-]+)\s*=\s*([^\s,]+)""")

    data class ExtInfo(
        val durationSeconds: Int,
        val attributes: Map<String, String>,
        val title: String,
    ) {
        fun attr(vararg names: String): String? {
            for (n in names) attributes[n]?.let { return it }
            return null
        }
    }

    fun parse(line: String): ExtInfo? {
        if (!line.startsWith("#EXTINF")) return null
        val body = line.removePrefix("#EXTINF:").trim()

        // "{-1 attr="v" ...}, Title" -> sure | ozellikler | baslik
        val firstComma = body.indexOf(',')
        if (firstComma < 0) return null

        val head = body.substring(0, firstComma)
        val title = body.substring(firstComma + 1).trim()

        val duration = head.substringBefore(' ').trim().removePrefix("-").toIntOrNull() ?: -1

        val attrPart = head.substringAfter(' ', "")
        val attrs = buildMap {
            ATTR_REGEX.findAll(attrPart).forEach { put(it.groupValues[1], it.groupValues[2]) }
            // Tirnakli olmayanlar (tvg-shift genelde unquoted gelir)
            ATTR_UNQUOTED.findAll(attrPart).forEach { m ->
                val key = m.groupValues[1]
                val value = m.groupValues[2]
                if (!containsKey(key) && !value.startsWith("\"")) put(key, value)
            }
        }

        return ExtInfo(duration, attrs, title.cleanChannelName())
    }
}
