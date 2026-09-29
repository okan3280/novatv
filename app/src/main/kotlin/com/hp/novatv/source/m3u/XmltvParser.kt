package com.hp.novatv.source.m3u

import android.util.Xml
import com.hp.novatv.core.model.Program
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * XMLTV (EPG) ayristirici. XmlPullParser ile akis halinde, bellegi asmaz.
 *
 * <tv>
 *   <channel id="tr.spor.tv"><display-name>Spor TV</display-name><icon .../></channel>
 *   <programme start="20260929100000 +0300" stop="20260929110000 +0300" channel="tr.spor.tv">
 Ma
 *   </programme>
 * </tv>
 */
class XmltvParser {

    private data class ChannelInfo(
        val id: String,
        val name: String,
        val icon: String,
    )

    /** tvg-id -> kanal eslemesi. */
    suspend fun parse(
        stream: InputStream,
        onPrograms: suspend (tvgId: String, programs: List<Program>) -> Unit,
    ) {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)

        val channels = mutableMapOf<String, ChannelInfo>()
        val buffer = mutableMapOf<String, MutableList<Program>>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "channel" -> {
                            val id = parser.getAttributeValue(null, "id").orEmpty()
                            val info = readChannel(parser, id)
                            if (id.isNotEmpty()) channels[id] = info
                        }

                        "programme" -> {
                            val tvgId = parser.getAttributeValue(null, "channel").orEmpty()
                            val start = parser.getAttributeValue(null, "start")
                            val stop = parser.getAttributeValue(null, "stop")
                            val program = readProgramme(parser, tvgId, start, stop)
                            if (program != null && tvgId.isNotEmpty()) {
                                buffer.getOrPut(tvgId) { mutableListOf() }.add(program)
                                // 500'lik parcala ile Room'a akit
                                val list = buffer[tvgId]!!
                                if (list.size >= 500) {
                                    onPrograms(tvgId, list.toList())
                                    buffer[tvgId] = mutableListOf()
                                }
                            }
                        }
                    }
                }
            }
            event = parser.next()
        }

        // Kalan parcalar
        buffer.forEach { (tvgId, list) -> if (list.isNotEmpty()) onPrograms(tvgId, list.toList()) }
    }

    /** Sozdesme donusen akis. */
    fun parseToFlow(stream: InputStream): Flow<List<Program>> = flow {
        val all = mutableListOf<Program>()
        parse(stream) { _, programs -> all += programs }
        emit(all)
    }

    private fun readChannel(parser: XmlPullParser, id: String): ChannelInfo {
        var name = ""
        var icon = ""
        var depth = 1

        while (depth > 0) {
            val e = parser.next()
            when {
                e == XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "display-name" -> name = parser.nextText().trim()
                        "icon" -> if (icon.isEmpty()) {
                            icon = parser.getAttributeValue(null, "src").orEmpty()
                        }
                    }
                }

                e == XmlPullParser.END_TAG -> depth--
            }
        }
        return ChannelInfo(id, name, icon)
    }

    private fun readProgramme(
        parser: XmlPullParser,
        tvgId: String,
        start: String?,
        stop: String?,
    ): Program? {
        val startMs = parseXmltvTime(start) ?: return null
        val stopMs = parseXmltvTime(stop) ?: (startMs + 3_600_000L)

        var title = ""
        var desc = ""
        var category = ""
        var icon = ""
        var rating = ""
        var depth = 1

        while (depth > 0) {
            val e = parser.next()
            if (e == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "title" -> if (title.isEmpty()) title = parser.nextText().trim()
                    "desc" -> if (desc.isEmpty()) desc = parser.nextText().trim()
                    "category" -> if (category.isEmpty()) category = parser.nextText().trim()
                    "icon" -> if (icon.isEmpty()) {
                        icon = parser.getAttributeValue(null, "src").orEmpty()
                    }

                    "value" -> if (rating.isEmpty()) rating = parser.nextText().trim()
                }
            } else if (e == XmlPullParser.END_TAG) {
                depth--
            }
        }

        return Program(
            channelId = 0,
            startEpoch = startMs / 1000,
            endEpoch = stopMs / 1000,
            title = title,
            description = desc,
            category = category,
            icon = icon,
            rating = rating,
        )
    }

    /**
     * "20260929100000 +0300" veya "20260929100000" bicimini epoch'a cevirir.
     */
    private fun parseXmltvTime(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val main = value.trim().substringBefore(' ').trim()
        val offset = value.trim().substringAfter(' ', "").trim()

        if (main.length < 14) return null

        val fmt = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
        val tz = if (offset.isNotEmpty()) {
            TimeZone.getTimeZone("GMT$offset")
        } else {
            TimeZone.getDefault()
        }
        fmt.timeZone = tz
        return runCatching { fmt.parse(main) }.getOrNull()?.time
    }
}
