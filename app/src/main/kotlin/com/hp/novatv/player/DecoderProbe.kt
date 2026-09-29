package com.hp.novatv.player

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.util.Log

/** Cihazda bulunan decoder'lar. */
data class DecoderReport(
    val h264: Boolean,
    val h265: Boolean,
    val mpeg2: Boolean,
    val av1: Boolean,
    val vp9: Boolean,
    val hardwareOnly: List<String>,
) {
    /** TCL 65Q6C uyumlu temel set: H.264 / H.265 / MPEG-2. */
    val compatible: Boolean get() = h264 && h265

    val summary: String
        get() = buildList {
            if (h264) add("H.264")
            if (h265) add("H.265")
            if (mpeg2) add("MPEG-2")
            if (vp9) add("VP9")
            if (av1) add("AV1")
        }.joinToString(" · ").ifBlank { "Bulunamadı" }
}

/**
 * Uygulama acilisinda MediaCodecList ile uyumluluk dogrulamasi.
 * Dolby Vision / HDR destegi bilincli olarak pasif - oynatici
 * goruntuyu oldugu gibi birakir.
 */
object DecoderProbe {

    private const val TAG = "NovaTV/DecoderProbe"

    private val supportedTypes = setOf(
        MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
        MediaCodecInfo.CodecProfileLevel.AVCProfileHigh,
        MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10,
        MediaCodecInfo.CodecProfileLevel.HEVCProfileMain,
        MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10,
    )

    fun probe(context: Context): DecoderReport {
        var h264 = false
        var h265 = false
        var mpeg2 = false
        var av1 = false
        var vp9 = false
        val hardware = mutableListOf<String>()

        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val infos = runCatching { list.codecInfos }.getOrElse { emptyArray() }

        for (info in infos) {
            val name = info.name.lowercase()
            val isHardware = !info.isSoftwareOnly
            if (isHardware) hardware += info.name

            runCatching {
                for (type in info.supportedTypes) {
                    val t = type.lowercase()
                    when {
                        t.contains("avc") -> if (isHardware || supportedTypes.isNotEmpty()) h264 = true
                        t.contains("hevc") -> h265 = true
                        t.contains("mpeg2") -> mpeg2 = true
                        t.contains("av01") -> av1 = true
                        t.contains("vp09") || t.contains("vp9") -> vp9 = true
                    }
                }
            }.onFailure { Log.w(TAG, "codec bilgisi okunamadi: ${info.name}", it) }
        }

        Log.i(TAG, "H264=$h264 H265=$h265 MPEG2=$mpeg2 AV1=$av1 VP9=$vp9")
        return DecoderReport(h264, h265, mpeg2, av1, vp9, hardware.sorted())
    }
}
