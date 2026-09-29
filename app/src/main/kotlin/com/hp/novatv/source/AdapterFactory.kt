package com.hp.novatv.source

import com.hp.novatv.core.model.Playlist
import com.hp.novatv.core.model.PlaylistType
import com.hp.novatv.source.m3u.M3uAdapter
import com.hp.novatv.source.stalker.StalkerAdapter
import com.hp.novatv.source.xtream.XtreamAdapter
import okhttp3.OkHttpClient

/** Playlist turune gore adaptor uretir. */
object AdapterFactory {

    fun create(playlist: Playlist, client: OkHttpClient): SourceAdapter = when (playlist.type) {
        PlaylistType.M3U -> M3uAdapter(playlist, client)
        PlaylistType.XTREAM -> XtreamAdapter(playlist, client)
        PlaylistType.STALKER -> StalkerAdapter(playlist, client)
    }
}
