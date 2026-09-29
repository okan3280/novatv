package com.hp.novatv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.hp.novatv.AppContainer
import com.hp.novatv.NovaTvApp
import com.hp.novatv.R
import com.hp.novatv.core.model.Channel
import com.hp.novatv.core.theme.LocalBaseSp
import com.hp.novatv.core.theme.NovaOnSurfaceVariant
import com.hp.novatv.core.util.programElapsed
import com.hp.novatv.core.util.startOfDay
import com.hp.novatv.ui.components.ChannelLogo
import com.hp.novatv.ui.components.ChannelNumber
import com.hp.novatv.ui.components.EmptyState
import com.hp.novatv.ui.components.LoadingState
import kotlinx.coroutines.delay

/**
 * Ekran 2: Ana ekran.
 * Sol: ikon rayi Â· Orta: grup kolonu Â· Sag: EPG grid.
 */
@Composable
fun HomeScreen(
    onOpenEpg: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCatchup: () -> Unit,
    onOpenRecordings: () -> Unit,
    onOpenMultiview: () -> Unit,
    onOpenSettings: () -> Unit,
    container: AppContainer = (androidx.compose.ui.platform.LocalContext.current.applicationContext as NovaTvApp).container,
) {
    val repository = container.repository
    val context = androidx.compose.ui.platform.LocalContext.current

    var playlistId by remember { mutableStateOf(-1L) }
    LaunchedEffect(Unit) {
        repository.lastPlaylistId.collect { id -> playlistId = id }
    }

    val playlists by repository.observePlaylists()
        .collectAsStateWithLifecycle(initial = emptyList())

    val effectiveId = remember(playlistId, playlists) {
        if (playlistId > 0) playlistId else playlists.firstOrNull()?.id ?: -1L
    }

    val groups by repository.observeGroups(effectiveId)
        .collectAsStateWithLifecycle(initial = emptyList())

    var selectedGroup by remember(effectiveId) { mutableStateOf("") }
    LaunchedEffect(groups) {
        if (selectedGroup !in groups) selectedGroup = groups.firstOrNull().orEmpty()
    }

    val channels by repository.observeChannelGroup(effectiveId, selectedGroup)
        .collectAsStateWithLifecycle(initial = emptyList())

    val favoriteIds by repository.observeFavoriteIds()
        .collectAsStateWithLifecycle(initial = emptySet())

    // Ana ekrandaki "simdi" guncellemesi
    var now by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis() / 1000
        }
    }

    BackHandler { onOpenSettings() }

    if (effectiveId <= 0) {
        EmptyState(
            title = stringResource(R.string.playlist_empty_title),
            body = "Ã–nce bir playlist seÃ§in.",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    Row(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // --- Sol ikon rayi ---
        IconRail(
            activeIndex = 0,
            onSelect = { index ->
                when (index) {
                    0 -> Unit
                    1 -> onOpenSearch()
                    2 -> onOpenCatchup()
                    3 -> onOpenRecordings()
                    4 -> onOpenMultiview()
                    5 -> onOpenEpg()
                }
            },
            onSettings = onOpenSettings,
            modifier = Modifier.width(96.dp).fillMaxHeight(),
        )

        // --- Orta: grup kolonu + kanallar ---
        Column(Modifier.weight(1f).fillMaxHeight()) {

            // Grup seridi
            LazyRow(
                modifier = Modifier.fillMaxWidth().height(74.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 20.dp, vertical = 10.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    RailChip(
                        label = stringResource(R.string.channel_favorite),
                        selected = selectedGroup == FAVORITES,
                        onClick = { selectedGroup = FAVORITES },
                    )
                }
                items(groups) { group ->
                    RailChip(
                        label = group,
                        selected = selectedGroup == group,
                        onClick = { selectedGroup = group },
                    )
                }
            }

            val visible = remember(channels, favoriteIds, selectedGroup) {
                if (selectedGroup == FAVORITES) {
                    channels.filter { it.id in favoriteIds }
                } else {
                    channels
                }
            }

            if (visible.isEmpty()) {
                LoadingState(
                    stringResource(R.string.empty_no_channels),
                    Modifier.weight(1f),
                )
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visible, key = { it.id }) { channel ->
                        ChannelRow(
                            channel = channel,
                            now = now,
                            showNumber = true,
                            isFavorite = channel.id in favoriteIds,
                            onClick = {
                                context.startActivity(
                                    com.hp.novatv.ui.PlayerActivity.intent(context, channel.id),
                                )
                            },
                        )
                    }
                }
            }
        }

        // --- Sag: EPG onizleme ---
        EpgPreviewPanel(
            channels = visible.take(12),
            repository = container.repository,
            now = now,
            onOpenFull = onOpenEpg,
            modifier = Modifier.width(420.dp).fillMaxHeight(),
        )
    }
}

private const val FAVORITES = "__favorites__"

/** Sol ikon rayi. */
@Composable
private fun IconRail(
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        "Ana" to "âŒ‚",
        "Ara" to "âŒ•",
        "GeÃ§miÅŸ" to "â†º",
        "KayÄ±t" to "â—",
        "Multi" to "âŠ",
        "EPG" to "â–¦",
    )

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface).padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEachIndexed { index, (label, icon) ->
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(12.dp),
                color = if (index == activeIndex) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                },
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = icon,
                        color = if (index == activeIndex) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = (LocalBaseSp.current * 0.7f).sp,
                    )
                    Text(
                        text = label,
                        color = if (index == activeIndex) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = (LocalBaseSp.current * 0.32f).sp,
                    )
                }
            }
        }

        Box(Modifier.weight(1f))

        Surface(
            onClick = onSettings,
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
        ) {
            Text(
                text = "âš™",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (LocalBaseSp.current * 0.7f).sp,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

@Composable
private fun RailChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Text(
            text = label,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontSize = (LocalBaseSp.current * 0.5f).sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/** Kanal satiri: numara, logo, ad, su anki program. */
@Composable
fun ChannelRow(
    channel: Channel,
    now: Long,
    showNumber: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showNumber) {
                ChannelNumber(channel.number)
                Box(Modifier.width(10.dp))
            }
            ChannelLogo(
                logoUrl = channel.logo,
                name = channel.name,
                modifier = Modifier.size(48.dp),
                cornerRadius = 6.dp,
            )
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = channel.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (LocalBaseSp.current * 0.6f).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                )
                CurrentProgramLine(channel = channel, now = now)
            }
            if (isFavorite) {
                Text(
                    text = "â˜…",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = (LocalBaseSp.current * 0.6f).sp,
                )
            }
        }
    }
}

@Composable
private fun CurrentProgramLine(channel: Channel, now: Long) {
    val repository = remember(channel.id) {
        (androidx.compose.ui.platform.LocalContext.current.applicationContext as NovaTvApp)
            .container.repository
    }
    val flow = remember(channel.id) {
        repository.observeChannelPrograms(channel.id, startOfDay(), startOfDay() + 86_400_000L)
    }
    val programs by flow.collectAsStateWithLifecycle(initial = emptyList())
    val current = programs.firstOrNull { it.startEpoch <= now && it.endEpoch > now }

    if (current == null) {
        Text(
            text = stringResource(R.string.player_no_info),
            color = NovaOnSurfaceVariant,
            fontSize = (LocalBaseSp.current * 0.44f).sp,
            maxLines = 1,
        )
        return
    }

    Column {
        Text(
            text = current.title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = (LocalBaseSp.current * 0.46f).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Ilerleme cubugu
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .padding(top = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(programElapsed(now, current.startEpoch, current.endEpoch))
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

/** Sag panel: secili kanallarin su anki programlari. */
@Composable
private fun EpgPreviewPanel(
    channels: List<Channel>,
    repository: com.hp.novatv.data.repo.ChannelRepository,
    now: Long,
    onOpenFull: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.nav_tv),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = (LocalBaseSp.current * 0.66f).sp,
                fontWeight = FontWeight.SemiBold,
            )
            Box(Modifier.weight(1f))
            Surface(
                onClick = onOpenFull,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = "Tam ekran",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (LocalBaseSp.current * 0.46f).sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }

        if (channels.isEmpty()) {
            Text(
                text = stringResource(R.string.epg_no_epg),
                color = NovaOnSurfaceVariant,
                fontSize = (LocalBaseSp.current * 0.5f).sp,
                modifier = Modifier.padding(top = 20.dp),
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(channels, key = { it.id }) { channel ->
                val programs by repository.observeCurrentProgram(channel.id)
                    .collectAsStateWithLifecycle(initial = null)
                val p = programs
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = channel.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = (LocalBaseSp.current * 0.54f).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = p?.title ?: stringResource(R.string.player_no_info),
                        color = if (p != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            NovaOnSurfaceVariant
                        },
                        fontSize = (LocalBaseSp.current * 0.46f).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
