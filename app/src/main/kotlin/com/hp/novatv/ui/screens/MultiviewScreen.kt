package com.hp.novatv.ui.screens

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickableimport androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.hp.novatv.AppContainer
import com.hp.novatv.NovaTvApp
import com.hp.novatv.R
import com.hp.novatv.core.model.Channel
import com.hp.novatv.core.model.MultiViewSlot
import com.hp.novatv.core.theme.LocalBaseSp
import com.hp.novatv.core.theme.NovaOnSurfaceVariant
import com.hp.novatv.player.ExoPlayerEngine
import com.hp.novatv.ui.components.ChannelLogo
import com.hp.novatv.ui.components.EmptyState
import kotlinx.coroutines.launch

/**
 * Ekran 9: Multiview (2-4 ekran).
 *
 * Bu SoC'de ust sinir 2-4 SD akis. 4K'da donanim limiti nedeniyle
 * ekran sayisi kisitlanir ve kullaniciya uyari gosterilir.
 */
@Composable
fun MultiviewScreen(
    onBack: () -> Unit,
    container: AppContainer = (androidx.compose.ui.platform.LocalContext.current.applicationContext as NovaTvApp).container,
) {
    val repository = container.repository
    val scope = rememberCoroutineScope()

    val settings by container.settings.settings
        .collectAsStateWithLifecycle(initial = com.hp.novatv.data.prefs.UiSettings())

    val limit = settings.maxStreams.coerceIn(2, ExoPlayerEngine.MAX_SLOTS)

    var slots by remember { mutableStateOf(List(limit) { MultiViewSlot(it) }) }
    var expanded by remember { mutableStateOf(-1) }
    var pickerFor by remember { mutableStateOf(-1) }
    var showPicker by remember { mutableStateOf(false) }

    var playlistId by remember { mutableStateOf(-1L) }
    LaunchedEffect(Unit) {
        repository.lastPlaylistId.collect { id -> playlistId = id }
    }
    val playlists by repository.observePlaylists()
        .collectAsStateWithLifecycle(initial = emptyList())
    val effectiveId = remember(playlistId, playlists) {
        if (playlistId > 0) playlistId else playlists.firstOrNull()?.id ?: -1L
    }
    val channels by repository.observeChannels(effectiveId)
        .collectAsStateWithLifecycle(initial = emptyList())

    // Yeni kanal secildiginde oynatici slotlarina yaz
    LaunchedEffect(slots) {
        slots.filter { it.channelId != null }.forEach { slot ->
            val channelId = slot.channelId ?: return@forEach
            val channel = repository.channel(channelId) ?: return@forEach
            val url = repository.resolvePlayUrl(channel)
            val player = container.exoEngine.getOrCreate(slot.index)
            container.exoEngine.play(player, channel, url)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            (1 until ExoPlayerEngine.MAX_SLOTS).forEach { container.exoEngine.releaseSlot(it) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Ust bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackChip(stringResource(R.string.back), onBack)
            Box(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                Text(
                    text = stringResource(R.string.nav_multiview),
                    color = Color.White,
                    fontSize = (LocalBaseSp.current * 0.62f).sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(R.string.multiview_limit, limit),
                color = NovaOnSurfaceVariant,
                fontSize = (LocalBaseSp.current * 0.42f).sp,
            )
        }

        // Izgara
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            slots.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { slot ->
                        MultiviewCell(
                            slot = slot,
                            isExpanded = expanded == slot.index,
                            container = container,
                            onAdd = { showPicker = true; pickerFor = slot.index },
                            onRemove = {
                                slots = slots.map {
                                    if (it.index == slot.index) MultiViewSlot(it.index) else it
                                }
                                container.exoEngine.releaseSlot(slot.index)
                            },
                            onExpand = {
                                expanded = if (expanded == slot.index) -1 else slot.index
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Tek kalan hucreyi tamamla
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }

    if (showPicker) {
        ChannelPickerDialog(
            channels = channels,
            onDismiss = { showPicker = false },
            onSelect = { channel ->
                slots = slots.map {
                    if (it.index == pickerFor) {
                        MultiViewSlot(
                            index = it.index,
                            channelId = channel.id,
                            playlistId = channel.playlistId,
                            channelName = channel.name,
                            logo = channel.logo,
                        )
                    } else {
                        it
                    }
                }
                showPicker = false
            },
        )
    }
}

@Composable
private fun MultiviewCell(
    slot: MultiViewSlot,
    isExpanded: Boolean,
    container: AppContainer,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.DarkGray)
            .border(
                width = if (isExpanded) 3.dp else 1.dp,
                color = if (isExpanded) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Gray
                },
                shape = RoundedCornerShape(8.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (slot.channelId == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    onClick = onAdd,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = stringResource(R.string.multiview_add),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = (LocalBaseSp.current * 0.52f).sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                    )
                }
            }
        } else {
            val player = remember(slot.index) { container.exoEngine.getOrCreate(slot.index) }

            AndroidView(
                factory = { ctx ->
                    androidx.media3.ui.PlayerView(ctx).apply {
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        this.player = player
                    }
                },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize(),
            )

            // Alt bilgi + menu
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = slot.channelName,
                    color = Color.White,
                    fontSize = (LocalBaseSp.current * 0.44f).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                SmallAction("⇄", onClick = onAdd)
                Box(Modifier.padding(horizontal = 4.dp))
                SmallAction(if (isExpanded) "⤡" else "⤢", onClick = onExpand)
                Box(Modifier.padding(horizontal = 4.dp))
                SmallAction("✕", destructive = true, onClick = onRemove)
            }
        }
    }
}

/**
 * Kanal secici. TV Material'de Dialog yok; modal tam ekran yuzey
 * kullanilir (TV deseni).
 */
@Composable
private fun ChannelPickerDialog(
    channels: List<Channel>,
    onDismiss: () -> Unit,
    onSelect: (Channel) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(700.dp)
                .clickable(enabled = true, onClick = {}),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.multiview_add),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (LocalBaseSp.current * 0.66f).sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (channels.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.empty_no_channels),
                        body = "Önce bir playlist senkronize edin.",
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(channels.take(200), key = { it.id }) { channel ->
                            Surface(
                                onClick = { onSelect(channel) },
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    ChannelLogo(
                                        logoUrl = channel.logo,
                                        name = channel.name,
                                        modifier = Modifier.size(40.dp),
                                        cornerRadius = 6.dp,
                                    )
                                    Text(
                                        text = channel.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = (LocalBaseSp.current * 0.54f).sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                SmallAction(
                    stringResource(R.string.cancel),
                    onClick = onDismiss,
                )
            }
        }
    }
}
