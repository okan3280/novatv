package com.hp.novatv.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "novatv_settings")

/** Gorsel yogunlugu. */
enum class UiDensity { COMPACT, NORMAL, COMFORTABLE }

/** Temalar. */
enum class AppTheme { DARK, LIGHT }

/** Oynatici motoru tercihi. */
enum class PlayerEnginePref { AUTO, EXOPLAYER, VLC }

/** Vurgu renkleri (10 secenek). */
enum class AccentColor(val hex: Long) {
    BLUE(0xFF00A8E8),
    CYAN(0xFF00BCD4),
    TEAL(0xFF009688),
    GREEN(0xFF4CAF50),
    LIME(0xFFCDDC39),
    AMBER(0xFFFFC107),
    ORANGE(0xFFFF9800),
    RED(0xFFF44336),
    PINK(0xFFE91E63),
    PURPLE(0xFF9C27B0),
}

/**
 * UI tercihleri. DataStore Preferences.
 * Sifreli alan burada tutulmaz; playlist sifreleri Room'da.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val ACCENT = stringPreferencesKey("accent")
        val THEME = stringPreferencesKey("theme")
        val DENSITY = stringPreferencesKey("density")
        val FONT_SCALE = intPreferencesKey("font_scale")
        val SHOW_NUMBERS = booleanPreferencesKey("show_channel_numbers")
        val SHOW_PROGRESS = booleanPreferencesKey("show_program_progress")
        val EPG_DAYS = intPreferencesKey("epg_days")
        val PLAYER_ENGINE = stringPreferencesKey("player_engine")
        val BUFFER_SECONDS = intPreferencesKey("buffer_seconds")
        val HW_DECODER = booleanPreferencesKey("hw_decoder")
        val TIMESHIFT = booleanPreferencesKey("timeshift")
        val PARENTAL_PIN = stringPreferencesKey("parental_pin")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val MAX_STREAMS = intPreferencesKey("max_streams")
        val LAST_PLAYLIST = intPreferencesKey("last_playlist")
        val LAST_CHANNEL = intPreferencesKey("last_channel")
        val KEEPAWARM = booleanPreferencesKey("keep_awake")
    }

    val settings: Flow<UiSettings> = context.dataStore.data.map { p ->
        UiSettings(
            accent = p[Keys.ACCENT]?.let { name ->
                AccentColor.entries.firstOrNull { it.name == name }
            } ?: AccentColor.BLUE,
            theme = p[Keys.THEME]?.let { name ->
                AppTheme.entries.firstOrNull { it.name == name }
            } ?: AppTheme.DARK,
            density = p[Keys.DENSITY]?.let { name ->
                UiDensity.entries.firstOrNull { it.name == name }
            } ?: UiDensity.NORMAL,
            fontScale = (p[Keys.FONT_SCALE] ?: 100) / 100f,
            showChannelNumbers = p[Keys.SHOW_NUMBERS] ?: true,
            showProgramProgress = p[Keys.SHOW_PROGRESS] ?: true,
            epgDays = p[Keys.EPG_DAYS] ?: 7,
            playerEngine = p[Keys.PLAYER_ENGINE]?.let { name ->
                PlayerEnginePref.entries.firstOrNull { it.name == name }
            } ?: PlayerEnginePref.AUTO,
            bufferSeconds = p[Keys.BUFFER_SECONDS] ?: 15,
            hwDecoder = p[Keys.HW_DECODER] ?: true,
            timeshift = p[Keys.TIMESHIFT] ?: true,
            hasPin = p[Keys.PIN_ENABLED] ?: false,
            maxStreams = p[Keys.MAX_STREAMS] ?: 2,
            keepAwake = p[Keys.KEEPAWARM] ?: true,
        )
    }

    val parentalPin: Flow<String> = context.dataStore.data.map { it[Keys.PARENTAL_PIN] ?: "" }

    val lastPlaylistId: Flow<Long> = context.dataStore.data.map {
        (it[Keys.LAST_PLAYLIST] ?: -1L).toLong()
    }

    val lastChannelId: Flow<Long> = context.dataStore.data.map {
        (it[Keys.LAST_CHANNEL] ?: -1L).toLong()
    }

    suspend fun setAccent(v: AccentColor) = put(Keys.ACCENT, v.name)
    suspend fun setTheme(v: AppTheme) = put(Keys.THEME, v.name)
    suspend fun setDensity(v: UiDensity) = put(Keys.DENSITY, v.name)
    suspend fun setFontScale(percent: Int) = edit { it[Keys.FONT_SCALE] = percent }
    suspend fun setShowChannelNumbers(v: Boolean) = put(Keys.SHOW_NUMBERS, v)
    suspend fun setShowProgramProgress(v: Boolean) = put(Keys.SHOW_PROGRESS, v)
    suspend fun setEpgDays(v: Int) = put(Keys.EPG_DAYS, v)
    suspend fun setPlayerEngine(v: PlayerEnginePref) = put(Keys.PLAYER_ENGINE, v.name)
    suspend fun setBufferSeconds(v: Int) = put(Keys.BUFFER_SECONDS, v)
    suspend fun setHwDecoder(v: Boolean) = put(Keys.HW_DECODER, v)
    suspend fun setTimeshift(v: Boolean) = put(Keys.TIMESHIFT, v)
    suspend fun setMaxStreams(v: Int) = put(Keys.MAX_STREAMS, v)

    suspend fun setParentalPin(pin: String) = edit {
        it[Keys.PARENTAL_PIN] = pin
        it[Keys.PIN_ENABLED] = pin.isNotEmpty()
    }

    suspend fun clearParentalPin() = edit {
        it.remove(Keys.PARENTAL_PIN)
        it[Keys.PIN_ENABLED] = false
    }

    suspend fun setLastPlaylist(id: Long) = put(Keys.LAST_PLAYLIST, id.toInt())
    suspend fun setLastChannel(id: Long) = put(Keys.LAST_CHANNEL, id.toInt())
    suspend fun setKeepAwake(v: Boolean) = put(Keys.KEEPAWARM, v)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}

/** UI ayarlarinin tek anlik goruntusu. */
data class UiSettings(
    val accent: AccentColor = AccentColor.BLUE,
    val theme: AppTheme = AppTheme.DARK,
    val density: UiDensity = UiDensity.NORMAL,
    val fontScale: Float = 1f,
    val showChannelNumbers: Boolean = true,
    val showProgramProgress: Boolean = true,
    val epgDays: Int = 7,
    val playerEngine: PlayerEnginePref = PlayerEnginePref.AUTO,
    val bufferSeconds: Int = 15,
    val hwDecoder: Boolean = true,
    val timeshift: Boolean = true,
    val hasPin: Boolean = false,
    val maxStreams: Int = 2,
    val keepAwake: Boolean = true,
) {
    /** 10-feot icin 48-56sp taban araligi. */
    val baseSp: Float
        get() = when (density) {
            UiDensity.COMPACT -> 40f
            UiDensity.NORMAL -> 48f
            UiDensity.COMFORTABLE -> 56f
        } * fontScale
}
