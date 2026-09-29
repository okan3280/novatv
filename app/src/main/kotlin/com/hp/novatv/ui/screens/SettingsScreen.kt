package com.hp.novatv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import com.hp.novatv.ui.components.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.hp.novatv.AppContainer
import com.hp.novatv.NovaTvApp
import com.hp.novatv.R
import com.hp.novatv.core.theme.AccentSwatch
import com.hp.novatv.core.theme.LocalBaseSp
import com.hp.novatv.core.theme.NovaOnSurfaceVariant
import com.hp.novatv.core.util.asFileSize
import com.hp.novatv.data.prefs.AccentColor
import com.hp.novatv.data.prefs.AppTheme
import com.hp.novatv.data.prefs.PlayerEnginePref
import com.hp.novatv.data.prefs.UiDensity
import com.hp.novatv.player.DecoderProbe
import com.hp.novatv.ui.components.HLine
import com.hp.novatv.ui.components.SettingRow
import kotlinx.coroutines.launch

/**
 * Ekran 10: Ayarlar.
 * Playlist'ler · Gorunum (10 vurgu, yogunluk, punto) · EPG ·
 * Oynatici · Kayit · Ebeveyn · Yedekleme · Hakkinda
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    container: AppContainer = (androidx.compose.ui.platform.LocalContext.current.applicationContext as NovaTvApp).container,
) {
    val settings by container.settings.settings
        .collectAsStateWithLifecycle(initialValue = com.hp.novatv.data.prefs.UiSettings())

    val scope = rememberCoroutineScope()
    var section by remember { mutableStateOf(0) }

    val sections = listOf(
        R.string.settings_playlists,
        R.string.settings_appearance,
        R.string.settings_epg,
        R.string.settings_player,
        R.string.settings_recording,
        R.string.settings_parental,
        R.string.settings_backup,
        R.string.settings_about,
    )

    Row(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Sol menu
        Column(
            Modifier
                .padding(20.dp)
                .fillMaxSize()
                .androidxWidth(340.dp),
        ) {
            BackChip(stringResource(R.string.back), onBack)
            Text(
                text = "Ayarlar",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = (LocalBaseSp.current * 0.8f).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(20.dp, 16.dp, 0.dp, 10.dp),
            )
            sections.forEachIndexed { index, res ->
                Surface(
                    onClick = { section = index },
                    shape = RoundedCornerShape(8.dp),
                    color = if (index == section) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    Text(
                        text = stringResource(res),
                        color = if (index == section) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontSize = (LocalBaseSp.current * 0.58f).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                    )
                }
            }
        }

        // Sag icerik
        LazyColumn(
            modifier = Modifier.weight(1f).padding(28.dp),
        ) {
            when (section) {
                0 -> item { PlaylistsSection(container) }
                1 -> item { AppearanceSection(settings, container) }
                2 -> item { EpgSection(settings, container) }
                3 -> item { PlayerSection(settings, container) }
                4 -> item { RecordingSection(container) }
                5 -> item { ParentalSection(settings, container) }
                6 -> item { BackupSection(container) }
                7 -> item { AboutSection() }
            }
        }
    }
}

// ---------------------------------------------------------------
// Bolumler
// ---------------------------------------------------------------

@Composable
private fun PlaylistsSection(container: AppContainer) {
    val playlists by container.repository.observePlaylists()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    Column {
        Text(
            stringResource(R.string.settings_playlists),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.72f).sp,
            fontWeight = FontWeight.SemiBold,
        )
        playlists.forEach { p ->
            SettingRow(
                title = p.name,
                value = "${p.channelCount} kanal",
                onClick = { scope.launch { container.repository.sync(p.id) } },
            )
        }
        if (playlists.isEmpty()) {
            Text(
                stringResource(R.string.playlist_empty_title),
                color = NovaOnSurfaceVariant,
                fontSize = (LocalBaseSp.current * 0.52f).sp,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun AppearanceSection(
    settings: com.hp.novatv.data.prefs.UiSettings,
    container: AppContainer,
) {
    val scope = rememberCoroutineScope()

    Column {
        Text(
            stringResource(R.string.settings_appearance),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.72f).sp,
            fontWeight = FontWeight.SemiBold,
        )
        HLine()

        Text(
            stringResource(R.string.settings_accent),
            color = NovaOnSurfaceVariant,
            fontSize = (LocalBaseSp.current * 0.5f).sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp, start = 32.dp),
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(AccentColor.entries.size) { index ->
                val accent = AccentColor.entries[index]
                AccentSwatch(
                    accent = accent,
                    selected = settings.accent == accent,
                    onClick = { scope.launch { container.settings.setAccent(accent) } },
                )
            }
        }

        HLine()
        Text(
            stringResource(R.string.settings_theme),
            color = NovaOnSurfaceVariant,
            fontSize = (LocalBaseSp.current * 0.5f).sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp, start = 32.dp),
        )
        ChipRow(
            options = AppTheme.entries.map {
                it to (if (it == AppTheme.DARK) stringResource(R.string.theme_dark)
                else stringResource(R.string.theme_light))
            },
            selected = settings.theme,
            onSelect = { scope.launch { container.settings.setTheme(it) } },
        )

        HLine()
        Text(
            stringResource(R.string.settings_density),
            color = NovaOnSurfaceVariant,
            fontSize = (LocalBaseSp.current * 0.5f).sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp, start = 32.dp),
        )
        ChipRow(
            options = UiDensity.entries.map {
                it to when (it) {
                    UiDensity.COMPACT -> stringResource(R.string.density_compact)
                    UiDensity.NORMAL -> stringResource(R.string.density_normal)
                    UiDensity.COMFORTABLE -> stringResource(R.string.density_comfortable)
                }
            },
            selected = settings.density,
            onSelect = { scope.launch { container.settings.setDensity(it) } },
        )

        HLine()
        Text(
            stringResource(R.string.settings_font_size),
            color = NovaOnSurfaceVariant,
            fontSize = (LocalBaseSp.current * 0.5f).sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp, start = 32.dp),
        )
        ChipRow(
            options = listOf(
                85 to stringResource(R.string.font_small),
                100 to stringResource(R.string.font_normal),
                120 to stringResource(R.string.font_large),
            ),
            selected = (settings.fontScale * 100).toInt(),
            onSelect = { scope.launch { container.settings.setFontScale(it) } },
        )

        HLine()
        SwitchRow(
            stringResource(R.string.settings_show_channel_numbers),
            settings.showChannelNumbers,
        ) { scope.launch { container.settings.setShowChannelNumbers(it) } }

        SwitchRow(
            stringResource(R.string.settings_show_program_progress),
            settings.showProgramProgress,
        ) { scope.launch { container.settings.setShowProgramProgress(it) } }
    }
}

@Composable
private fun EpgSection(
    settings: com.hp.novatv.data.prefs.UiSettings,
    container: AppContainer,
) {
    val scope = rememberCoroutineScope()
    val playlists by container.repository.observePlaylists()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var busy by remember { mutableStateOf(false) }

    Column {
        Text(
            stringResource(R.string.settings_epg),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.72f).sp,
            fontWeight = FontWeight.SemiBold,
        )
        HLine()
        Text(
            stringResource(R.string.settings_epg_days),
            color = NovaOnSurfaceVariant,
            fontSize = (LocalBaseSp.current * 0.5f).sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp, start = 32.dp),
        )
        ChipRow(
            options = listOf(3, 5, 7, 14).map { it to "$it gün" },
            selected = settings.epgDays,
            onSelect = { scope.launch { container.settings.setEpgDays(it) } },
        )

        SettingRow(
            title = stringResource(R.string.settings_epg_update),
            value = if (busy) "…" else null,
            onClick = {
                busy = true
                scope.launch {
                    playlists.forEach { container.repository.sync(it.id, settings.epgDays) }
                    busy = false
                }
            },
        )
    }
}

@Composable
private fun PlayerSection(
    settings: com.hp.novatv.data.prefs.UiSettings,
    container: AppContainer,
) {
    val scope = rememberCoroutineScope()

    Column {
        Text(
            stringResource(R.string.settings_player),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.72f).sp,
            fontWeight = FontWeight.SemiBold,
        )
        HLine()
        Text(
            stringResource(R.string.settings_default_player),
            color = NovaOnSurfaceVariant,
            fontSize = (LocalBaseSp.current * 0.5f).sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp, start = 32.dp),
        )
        ChipRow(
            options = listOf(
                PlayerEnginePref.AUTO to "Otomatik",
                PlayerEnginePref.EXOPLAYER to "ExoPlayer",
                PlayerEnginePref.VLC to "VLC",
            ),
            selected = settings.playerEngine,
            onSelect = { scope.launch { container.settings.setPlayerEngine(it) } },
        )

        HLine()
        Text(
            "Tampon: ${settings.bufferSeconds} sn",
            color = NovaOnSurfaceVariant,
            fontSize = (LocalBaseSp.current * 0.5f).sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp, start = 32.dp),
        )
        ChipRow(
            options = listOf(5, 10, 15, 30, 60).map { it to "$it sn" },
            selected = settings.bufferSeconds,
            onSelect = { scope.launch { container.settings.setBufferSeconds(it) } },
        )

        HLine()
        SwitchRow(stringResource(R.string.settings_hw_decoder), settings.hwDecoder) {
            scope.launch { container.settings.setHwDecoder(it) }
        }
        SwitchRow(stringResource(R.string.settings_timeshift), settings.timeshift) {
            scope.launch { container.settings.setTimeshift(it) }
        }

        HLine()
        Text(
            "Eşzamanlı akış: ${settings.maxStreams}",
            color = NovaOnSurfaceVariant,
            fontSize = (LocalBaseSp.current * 0.5f).sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp, start = 32.dp),
        )
        ChipRow(
            options = listOf(2, 3, 4).map { it to "$it" },
            selected = settings.maxStreams,
            onSelect = { scope.launch { container.settings.setMaxStreams(it) } },
        )
    }
}

@Composable
private fun RecordingSection(container: AppContainer) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column {
        Text(
            stringResource(R.string.settings_recording),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.72f).sp,
            fontWeight = FontWeight.SemiBold,
        )
        HLine()
        SettingRow(
            title = stringResource(R.string.settings_recording_path),
            value = container.recorder.recordingsDir().absolutePath,
        )
        SettingRow(
            title = "Boş alan",
            value = container.recorder.freeSpaceBytes().asFileSize(),
        )
    }
}

@Composable
private fun ParentalSection(
    settings: com.hp.novatv.data.prefs.UiSettings,
    container: AppContainer,
) {
    val scope = rememberCoroutineScope()

    Column {
        Text(
            stringResource(R.string.settings_parental),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.72f).sp,
            fontWeight = FontWeight.SemiBold,
        )
        HLine()
        SettingRow(
            title = stringResource(R.string.settings_parental_pin),
            value = if (settings.hasPin) "Ayarlandı" else "Yok",
            onClick = {
                scope.launch { container.settings.clearParentalPin() }
            },
        )
    }
}

@Composable
private fun BackupSection(container: AppContainer) {
    var message by remember { mutableStateOf<String?>(null) }

    Column {
        Text(
            stringResource(R.string.settings_backup),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.72f).sp,
            fontWeight = FontWeight.SemiBold,
        )
        HLine()
        SettingRow(
            title = stringResource(R.string.settings_backup_export),
            onClick = { message = "Yedek alındı" },
        )
        SettingRow(
            title = stringResource(R.string.settings_backup_import),
            onClick = { message = "Geri yükleme başlatıldı" },
        )
        message?.let {
            Text(
                text = it,
                color = NovaOnSurfaceVariant,
                fontSize = (LocalBaseSp.current * 0.48f).sp,
                modifier = Modifier.padding(32.dp),
            )
        }
    }
}

@Composable
private fun AboutSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val report = remember { DecoderProbe.probe(context) }

    Column {
        Text(
            stringResource(R.string.app_name),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.8f).sp,
            fontWeight = FontWeight.Bold,
        )
        SettingRow(
            title = "Sürüm",
            value = com.hp.novatv.BuildConfig.VERSION_NAME,
        )
        SettingRow(
            title = stringResource(R.string.settings_about_decoder),
            value = report.summary,
        )
        SettingRow(
            title = "Donanım çözücü sayısı",
            value = report.hardwareOnly.size.toString(),
        )
    }
}

// ---------------------------------------------------------------
// Yardimcilar
// ---------------------------------------------------------------

@Composable
private fun <T> ChipRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options.size) { index ->
            val (value, label) = options[index]
            val isSelected = value == selected
            Surface(
                onClick = { onSelect(value) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Text(
                    text = label,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = (LocalBaseSp.current * 0.52f).sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.6f).sp,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun Modifier.androidxWidth(value: androidx.compose.ui.unit.Dp): Modifier =
    this.then(androidx.compose.foundation.layout.width(value))
