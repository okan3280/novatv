package com.hp.novatv.data.repo

import android.content.Context
import com.hp.novatv.core.model.Channel
import com.hp.novatv.core.model.HistoryEntry
import com.hp.novatv.core.model.Playlist
import com.hp.novatv.core.model.PlaylistType
import com.hp.novatv.core.model.Program
import com.hp.novatv.core.model.Recording
import com.hp.novatv.data.db.NovaDatabase
import com.hp.novatv.data.db.PlaylistEntity
import com.hp.novatv.data.db.ChannelEntity
import com.hp.novatv.data.db.ProgramEntity
import com.hp.novatv.data.db.FavoriteEntity
import com.hp.novatv.data.db.WatchlistEntity
import com.hp.novatv.data.db.HistoryEntity
import com.hp.novatv.data.db.RecordingEntity
import com.hp.novatv.data.db.ScheduleEntity
import com.hp.novatv.data.db.ParentalLockEntity
import com.hp.novatv.data.prefs.SettingsStore
import com.hp.novatv.source.AdapterFactory
import com.hp.novatv.source.SourceException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Ana veri merkezi. Tum ekranlar buradan okur.
 *
 * DI: Hilt yerine elle kurulum - [AppContainer] tek yapidir.
 */
class ChannelRepository(
    private val db: NovaDatabase,
    private val settings: SettingsStore,
    private val httpClient: okhttp3.OkHttpClient,
) {

    private val playlists = db.playlistDao()
    private val channels = db.channelDao()
    private val programDao = db.programDao()
    private val userData = db.userDataDao()

    // ---------------------------------------------------------------
    // Playlist
    // ---------------------------------------------------------------

    fun observePlaylists(): Flow<List<Playlist>> =
        playlists.observeAll().map { list -> list.map { it.toModel() } }

    fun observePlaylistCount(): Flow<Int> = playlists.observeCount()

    suspend fun playlist(id: Long): Playlist? = playlists.byId(id)?.toModel()

    suspend fun savePlaylist(playlist: Playlist): Long {
        val id = if (playlist.id == 0L) {
            playlists.insert(playlist.toEntity())
        } else {
            playlists.update(playlist.toEntity())
            playlist.id
        }
        return id
    }

    suspend fun deletePlaylist(id: Long) = playlists.deleteById(id)

    val lastPlaylistId: Flow<Long> = settings.lastPlaylistId

    // ---------------------------------------------------------------
    // Kanal
    // ---------------------------------------------------------------

    fun observeChannels(playlistId: Long): Flow<List<Channel>> =
        channels.observeByPlaylist(playlistId).map { list -> list.map { it.toModel() } }

    fun observeChannelGroup(playlistId: Long, group: String): Flow<List<Channel>> =
        channels.observeGroup(playlistId, group).map { list -> list.map { it.toModel() } }

    fun observeGroups(playlistId: Long): Flow<List<String>> = channels.observeGroups(playlistId)

    fun observeChannel(id: Long): Flow<Channel?> =
        channels.observeById(id).map { it?.toModel() }

    suspend fun channel(id: Long): Channel? = channels.byId(id)?.toModel()

    fun observeTotalChannelCount(): Flow<Int> = channels.observeTotalCount()

    fun searchChannels(query: String): Flow<List<Channel>> =
        channels.searchByName(query.trim()).map { list -> list.map { it.toModel() } }

    // ---------------------------------------------------------------
    // EPG
    // ---------------------------------------------------------------

    fun observeEpg(
        channelIds: List<Long>,
        from: Long,
        to: Long,
    ): Flow<Map<Long, List<Program>>> =
        programDao.observeRange(channelIds, from, to).map { rows ->
            rows.groupBy { it.channelId }.mapValues { (_, list) -> list.map { it.toModel() } }
        }

    fun observeChannelPrograms(channelId: Long, from: Long, to: Long): Flow<List<Program>> =
        programDao.observeForChannel(channelId, from, to).map { list -> list.map { it.toModel() } }

    fun observeCurrentProgram(channelId: Long): Flow<Program?> =
        programDao.observeCurrent(channelId, System.currentTimeMillis() / 1000)
            .map { it?.toModel() }

    fun searchPrograms(query: String): Flow<List<Program>> =
        programDao.searchPrograms(query.trim()).map { list -> list.map { it.toModel() } }

    val programCount: Flow<Int> = programDao.observeCount()

    // ---------------------------------------------------------------
    // Oynatma
    // ---------------------------------------------------------------

    /**
     * Kanalin oynatilabilir URL'ini cozer. Stalker gibi adaptorler
     * dinamik link uretir; M3U/Xtream dogrudan URL dondurur.
     *
     * @param startEpoch catch-up baslangici; normal oynatmada null
     */
    suspend fun resolvePlayUrl(
        channel: Channel,
        startEpoch: Long? = null,
        durationMs: Long? = null,
    ): String = withContext(Dispatchers.IO) {
        val playlist = playlists.byId(channel.playlistId)?.toModel()
            ?: return@withContext channel.streamUrl
        val adapter = AdapterFactory.create(playlist, httpClient)
        try {
            adapter.resolvePlayUrl(channel, startEpoch, durationMs)
        } catch (e: Exception) {
            // Adaptor basarisiz: dogrudan URL'a dus
            channel.streamUrl
        } finally {
            adapter.close()
        }
    }

    /** Timeshift (kaydirilabilir canli) URL'si. */
    suspend fun resolveTimeshiftUrl(channel: Channel, positionMs: Long): String? =
        withContext(Dispatchers.IO) {
            val playlist = playlists.byId(channel.playlistId)?.toModel()
                ?: return@withContext null
            val adapter = AdapterFactory.create(playlist, httpClient)
            try {
                adapter.resolveTimeshiftUrl(channel, positionMs)
            } catch (e: Exception) {
                null
            } finally {
                adapter.close()
            }
        }

    // ---------------------------------------------------------------
    // Favori / izleme listesi / gecmis
    // ---------------------------------------------------------------

    fun observeFavoriteIds(): Flow<Set<Long>> =
        userData.favoriteIds().map { it.toSet() }

    fun observeWatchlistIds(): Flow<Set<Long>> =
        userData.watchlistIds().map { it.toSet() }

    fun isFavorite(channelId: Long): Flow<Boolean> = userData.isFavorite(channelId)

    fun observeHistory(limit: Int = 50): Flow<List<HistoryEntry>> =
        userData.observeHistory(limit).map { list ->
            list.map {
                HistoryEntry(
                    channelId = it.channelId,
                    playlistId = it.playlistId,
                    name = it.name,
                    logo = it.logo,
                    watchedAt = it.watchedAt,
                    positionMs = it.positionMs,
                    durationMs = it.durationMs,
                )
            }
        }

    suspend fun toggleFavorite(channel: Channel) {
        val isFav = userData.isFavorite(channel.id).first()
        if (isFav) {
            userData.removeFavorite(channel.id)
            channels.setFavorite(channel.id, false)
        } else {
            userData.addFavorite(
                FavoriteEntity(channelId = channel.id, playlistId = channel.playlistId),
            )
            channels.setFavorite(channel.id, true)
        }
    }

    suspend fun toggleWatchlist(channel: Channel) {
        val inList = userData.watchlistIds().first().contains(channel.id)
        if (inList) {
            userData.removeWatchlist(channel.id)
        } else {
            userData.addWatchlist(
                WatchlistEntity(channelId = channel.id, playlistId = channel.playlistId),
            )
        }
    }

    suspend fun saveHistory(channel: Channel, positionMs: Long, durationMs: Long) {
        userData.upsertHistory(
            HistoryEntity(
                channelId = channel.id,
                playlistId = channel.playlistId,
                name = channel.name,
                logo = channel.logo,
                watchedAt = System.currentTimeMillis(),
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    suspend fun historyFor(channelId: Long): HistoryEntry? =
        userData.historyFor(channelId)?.let {
            HistoryEntry(
                channelId = it.channelId,
                playlistId = it.playlistId,
                name = it.name,
                logo = it.logo,
                watchedAt = it.watchedAt,
                positionMs = it.positionMs,
                durationMs = it.durationMs,
            )
        }

    // ---------------------------------------------------------------
    // Ebeveyn kilidi
    // ---------------------------------------------------------------

    fun observeLockedChannelIds(): Flow<Set<Long>> =
        userData.lockedChannelIds().map { it.toSet() }

    fun isLocked(channelId: Long): Flow<Boolean> = userData.isLocked(channelId)

    suspend fun setChannelLocked(channel: Channel, locked: Boolean) {
        if (locked) {
            userData.addLock(ParentalLockEntity(channelId = channel.id, playlistId = channel.playlistId))
        } else {
            userData.removeLock(channel.id)
        }
        channels.setLocked(channel.id, locked)
    }

    // ---------------------------------------------------------------
    // Kayitlar
    // ---------------------------------------------------------------

    fun observeRecordings(): Flow<List<Recording>> =
        userData.observeRecordings().map { list ->
            list.map {
                Recording(
                    id = it.id,
                    channelId = it.channelId,
                    playlistId = it.playlistId,
                    channelName = it.channelName,
                    title = it.title,
                    filePath = it.filePath,
                    startedAt = it.startedAt,
                    durationMs = it.durationMs,
                    sizeBytes = it.sizeBytes,
                    isLocal = it.isLocal,
                    thumbnailPath = it.thumbnailPath,
                )
            }
        }

    suspend fun addRecording(recording: Recording): Long =
        userData.upsertRecording(
            RecordingEntity(
                id = recording.id,
                channelId = recording.channelId,
                playlistId = recording.playlistId,
                channelName = recording.channelName,
                title = recording.title,
                filePath = recording.filePath,
                startedAt = recording.startedAt,
                durationMs = recording.durationMs,
                sizeBytes = recording.sizeBytes,
                isLocal = recording.isLocal,
                thumbnailPath = recording.thumbnailPath,
            ),
        )

    suspend fun recording(id: Long): Recording? = userData.recordingById(id)?.let {
        Recording(
            id = it.id,
            channelId = it.channelId,
            playlistId = it.playlistId,
            channelName = it.channelName,
            title = it.title,
            filePath = it.filePath,
            startedAt = it.startedAt,
            durationMs = it.durationMs,
            sizeBytes = it.sizeBytes,
            isLocal = it.isLocal,
            thumbnailPath = it.thumbnailPath,
        )
    }

    suspend fun deleteRecording(id: Long) = userData.deleteRecording(id)

    // ---------------------------------------------------------------
    // Zamanlanmis kayitlar
    // ---------------------------------------------------------------

    fun observeActiveSchedules() = userData.observeActiveSchedules()

    fun observeAllSchedules() = userData.observeAllSchedules()

    suspend fun upsertSchedule(schedule: com.hp.novatv.core.model.ScheduledRecording): Long =
        userData.upsertSchedule(
            ScheduleEntity(
                id = schedule.id,
                channelId = schedule.channelId,
                playlistId = schedule.playlistId,
                channelName = schedule.channelName,
                programTitle = schedule.programTitle,
                startEpoch = schedule.startEpoch,
                endEpoch = schedule.endEpoch,
                enabled = schedule.enabled,
                repeatDaily = schedule.repeatDaily,
            ),
        )

    suspend fun deleteSchedule(id: Long) = userData.deleteSchedule(id)

    // ---------------------------------------------------------------
    // Senkronizasyon
    // ---------------------------------------------------------------

    /**
     * Playlist'i senkronize eder: kanallari degistirir, EPG'yi doldurur.
     * Hatalar yakalanip [SyncOutcome] icinde dondurulur; UI cokmez.
     */
    suspend fun sync(playlistId: Long, epgDays: Int = 7): com.hp.novatv.core.model.SyncResult =
        withContext(Dispatchers.IO) {
            val playlist = playlists.byId(playlistId)?.toModel()
                ?: return@withContext com.hp.novatv.core.model.SyncResult(
                    playlistId = playlistId,
                    error = "Playlist bulunamadı",
                )

            val adapter = AdapterFactory.create(playlist, httpClient)

            try {
                val channelList = adapter.loadChannels()
                val entities = channelList.map { it.copy(playlistId = playlistId).toEntity() }
                channels.replaceForPlaylist(playlistId, entities)

                // Kanal id'leri yeni atandigi icin eski EPG satirlari orphan olur.
                programDao.clear()

                val programCount = runCatching {
                    var total = 0
                    adapter.loadPrograms(entities.map { it.toModel() }).collect { batch ->
                        total += insertPrograms(entities, batch)
                    }
                    total
                }.getOrDefault(0)

                playlists.markSynced(playlistId, System.currentTimeMillis(), entities.size)
                com.hp.novatv.core.model.SyncResult(
                    playlistId = playlistId,
                    channelCount = entities.size,
                    programCount = programCount,
                )
            } catch (e: SourceException) {
                com.hp.novatv.core.model.SyncResult(playlistId = playlistId, error = e.message)
            } catch (e: Exception) {
                com.hp.novatv.core.model.SyncResult(
                    playlistId = playlistId,
                    error = e.message ?: "Bilinmeyen hata",
                )
            } finally {
                adapter.close()
            }
        }

    /** tvg-id uzerinden kanali bulup programlari Room'a yazar. */
    private suspend fun insertPrograms(
        channelEntities: List<ChannelEntity>,
        programs: List<Program>,
    ): Int {
        if (programs.isEmpty()) return 0

        val byTvg = channelEntities.filter { it.tvgId.isNotEmpty() }
            .associateBy({ it.tvgId }, { it.id })
        val byId = channelEntities.associateBy { it.id }

        val rows = programs.mapNotNull { p ->
            val channelId = when {
                p.channelId != 0L -> byId[p.channelId]?.id
                p.tvgId.isNotEmpty() -> byTvg[p.tvgId]
                else -> null
            } ?: return@mapNotNull null

            ProgramEntity(
                channelId = channelId,
                tvgId = p.tvgId,
                startEpoch = p.startEpoch,
                endEpoch = p.endEpoch,
                title = p.title,
                description = p.description,
                category = p.category,
                icon = p.icon,
                rating = p.rating,
                catchupUrl = p.catchupUrl,
            )
        }

        if (rows.isNotEmpty()) programDao.insertAll(rows)
        return rows.size
    }
}

// ---------------------------------------------------------------
// Entity <-> Model donusumleri
// ---------------------------------------------------------------

internal fun PlaylistEntity.toModel() = Playlist(
    id = id,
    name = name,
    type = runCatching { PlaylistType.valueOf(type) }.getOrDefault(PlaylistType.M3U),
    url = url,
    username = username,
    password = password,
    epgUrl = epgUrl,
    logo = logo,
    portalId = portalId,
    mac = mac,
    lastSync = lastSync,
    channelCount = channelCount,
)

internal fun Playlist.toEntity() = PlaylistEntity(
    id = id,
    name = name,
    type = type.name,
    url = url,
    username = username,
    password = password,
    epgUrl = epgUrl,
    logo = logo,
    portalId = portalId,
    mac = mac,
    lastSync = lastSync,
    channelCount = channelCount,
)

internal fun ChannelEntity.toModel() = Channel(
    id = id,
    playlistId = playlistId,
    streamId = streamId,
    number = number,
    name = name,
    logo = logo,
    group = group,
    tvgId = tvgId,
    streamUrl = streamUrl,
    catchupSource = catchupSource,
    catchupAppend = catchupAppend,
    catchupVshift = catchupVshift,
    catchupDays = catchupDays,
    timeshiftUrl = timeshiftUrl,
    epgOffset = epgOffset,
    isFavorite = isFavorite,
    isLocked = isLocked,
    isHidden = isHidden,
    containerExtension = containerExtension,
)

internal fun Channel.toEntity() = ChannelEntity(
    id = id,
    playlistId = playlistId,
    streamId = streamId,
    number = number,
    name = name,
    logo = logo,
    group = group,
    tvgId = tvgId,
    streamUrl = streamUrl,
    catchupSource = catchupSource,
    catchupAppend = catchupAppend,
    catchupVshift = catchupVshift,
    catchupDays = catchupDays,
    timeshiftUrl = timeshiftUrl,
    epgOffset = epgOffset,
    isFavorite = isFavorite,
    isLocked = isLocked,
    isHidden = isHidden,
    containerExtension = containerExtension,
)

internal fun ProgramEntity.toModel() = Program(
    id = id,
    channelId = channelId,
    startEpoch = startEpoch,
    endEpoch = endEpoch,
    title = title,
    description = description,
    category = category,
    icon = icon,
    rating = rating,
    catchupUrl = catchupUrl,
)
