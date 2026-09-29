package com.hp.novatv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import com.hp.novatv.ui.components.Surface
import androidx.tv.material3.Text
import com.hp.novatv.AppContainer
import com.hp.novatv.NovaTvApp
import com.hp.novatv.R
import com.hp.novatv.core.model.Channel
import com.hp.novatv.core.model.Program
import com.hp.novatv.core.theme.LocalBaseSp
import com.hp.novatv.core.theme.NovaLive
import com.hp.novatv.core.theme.NovaOnSurfaceVariant
import com.hp.novatv.core.util.asShortDate
import com.hp.novatv.core.util.asTime
import com.hp.novatv.core.util.startOfDay
import com.hp.novatv.ui.components.EmptyState
import com.hp.novatv.ui.components.LoadingState
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Ekran 3: EPG Grid.
 *
 * Yatay kanal sutunlari x dikey zaman seridi. "Simdi" cizgisi ve
 * gun basliklari sabit durur. Program hucreleri 15 dakikalik yuvarlanmis
 * genislikte dizilir.
 */
@Composable
fun EpgRoute(
    playlistId: Long,
    onBack: () -> Unit,
    onOpenProgram: (Long, Long) -> Unit,
    container: AppContainer = (androidx.compose.ui.platform.LocalContext.current.applicationContext as NovaTvApp).container,
) {
    val repository = container.repository

    // Hangi playlist gosterilecek: parametre gelmediyse en son kullanilan
    var effectiveId by remember(playlistId) { mutableStateOf(playlistId) }
    if (effectiveId <= 0) {
        LaunchedEffect(Unit) {
            repository.lastPlaylistId.collect { id ->
                if (id > 0) effectiveId = id
            }
        }
    }

    val channels by repository.observeChannels(effectiveId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    if (channels.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.epg_no_epg),
            body = "Önce bir playlist seçin veya yenileyin.",
            modifier = Modifier.fillMaxSize(),
            actionLabel = stringResource(R.string.back),
            onAction = onBack,
        )
        return
    }

    EpgGrid(
        channels = channels,
        repository = container.repository,
        onBack = onBack,
        onOpenProgram = onOpenProgram,
    )
}

@Composable
private fun EpgGrid(
    channels: List<Channel>,
    repository: com.hp.novatv.data.repo.ChannelRepository,
    onBack: () -> Unit,
    onOpenProgram: (Long, Long) -> Unit,
) {
    var dayOffset by remember { mutableStateOf(0) }
    val dayStart = remember(dayOffset) { startOfDay() + dayOffset * 86_400_000L }
    val now = remember { System.currentTimeMillis() / 1000 }

    val visibleChannels = remember(channels) {
        // Cok buyuk listelerde ilk 40 kanal ile sinirla: 4K'da performans
        channels.take(40)
    }

    val epg by produceEpg(repository, visibleChannels.map { it.id }, dayStart)

    val hourHeight = 96.dp
    val channelWidth = 160.dp
    val headerHeight = 64.dp

    val channelListState = rememberLazyListState()
    val timeListState = rememberLazyListState()

    // Baslangicta "simdi"ya kaydir
    LaunchedEffect(dayOffset) {
        if (dayOffset == 0) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            channelListState.scrollToItem((hour * 4).coerceAtLeast(0))
            timeListState.scrollToItem(hour)
        }
    }

    // "Simdi" cizgisi her dakika ilerler
    var tick by remember { mutableStateOf(now) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            tick = System.currentTimeMillis() / 1000
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Ust bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavChip(stringResource(R.string.back), onBack)
            Box(Modifier.width(20.dp))

            // Gun secici
            for (offset in 0..6) {
                val dayEpoch = dayStart + offset * 86_400_000L
                val label = if (offset == 0) {
                    stringResource(R.string.epg_now)
                } else {
                    dayEpoch.asShortDate()
                }
                DayChip(
                    label = label,
                    selected = dayOffset == offset,
                    onClick = { dayOffset = offset },
                )
                Box(Modifier.width(8.dp))
            }
        }

        if (epg.isEmpty()) {
            LoadingState(
                stringResource(R.string.epg_loading_epg),
                Modifier.padding(top = headerHeight),
            )
        } else {
            // Kanal basligi satiri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = headerHeight),
            ) {
                Box(
                    Modifier
                        .width(200.dp)
                        .height(headerHeight)
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    Text(
                        text = "Kanallar",
                        color = NovaOnSurfaceVariant,
                        fontSize = (LocalBaseSp.current * 0.5f).sp,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                LazyRow(
                    modifier = Modifier.weight(1f).height(headerHeight),
                    state = channelListState,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 24.dp),
                ) {
                    items(visibleChannels, key = { it.id }) { channel ->
                        EpgChannelHeader(
                            channel = channel,
                            program = epg[channel.id]?.firstOrNull { it.startEpoch <= tick && it.endEpoch > tick },
                            width = channelWidth,
                        )
                    }
                }
            }

            // Zaman seridi
            Row(Modifier.fillMaxSize()) {
                // Kanal adlari sutunu
                LazyColumn(
                    modifier = Modifier
                        .width(200.dp)
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    items(visibleChannels, key = { it.id }) { channel ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(hourHeight)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = channel.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = (LocalBaseSp.current * 0.5f).sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Izgara
                Box(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxSize()) {
                        // Saat olcekleri
                        LazyColumn(
                            state = timeListState,
                            modifier = Modifier
                                .width(56.dp)
                                .height(hourHeight * 24),
                        ) {
                            items(24) { hour ->
                                Box(
                                    Modifier
                                        .height(hourHeight)
                                        .width(56.dp),
                                    contentAlignment = Alignment.TopStart,
                                ) {
                                    Text(
                                        text = String.format("%02d:00", hour),
                                        color = NovaOnSurfaceVariant,
                                        fontSize = (LocalBaseSp.current * 0.44f).sp,
                                        modifier = Modifier.padding(start = 6.dp, top = 2.dp),
                                    )
                                }
                            }
                        }

                        // Kanallar x programlar
                        LazyRow(
                            modifier = Modifier
                                .weight(1f)
                                .height(hourHeight * 24),
                        ) {
                            items(visibleChannels, key = { it.id }) { channel ->
                                val programs = epg[channel.id].orEmpty()
                                if (programs.isEmpty()) {
                                    Box(Modifier.width(channelWidth).height(hourHeight * 24))
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .width(channelWidth)
                                            .height(hourHeight * 24),
                                    ) {
                                        programs.forEach { program ->
                                            ProgramCell(
                                                program = program,
                                                dayStart = dayStart,
                                                hourHeight = hourHeight,
                                                isLive = program.startEpoch <= tick && program.endEpoch > tick,
                                                onClick = { onOpenProgram(channel.id, program.id) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // "Simdi" cizgisi
                    if (dayOffset == 0) {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = System.currentTimeMillis()
                        }
                        val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                        val offsetY = hourHeight * (minutes / 60f)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(NovaLive)
                                .offset(y = offsetY),
                        )
                    }
                }
            }
        }
    }
}

/** Iki saat arasindaki programlari %15 yuvarlanmis bloklara dizer. */
@Composable
private fun ProgramCell(
    program: Program,
    dayStart: Long,
    hourHeight: Dp,
    isLive: Boolean,
    onClick: () -> Unit,
) {
    val startHour = ((program.startEpoch - dayStart) / 3600.0).toFloat()
    val endHour = ((program.endEpoch - dayStart) / 3600.0).toFloat()
    val durationHours = (endHour - startHour).coerceAtLeast(0.15f)

    Box(
        modifier = Modifier
            .width((hourHeight * durationHours * 0.5f).coerceAtLeast(70.dp))
            .height(hourHeight * 0.92f)
            .padding(1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isLive) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(horizontal = 5.dp, vertical = 3.dp)) {
            Text(
                text = program.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = (LocalBaseSp.current * 0.42f).sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isLive) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                text = program.startEpoch.asTime(),
                color = NovaOnSurfaceVariant,
                fontSize = (LocalBaseSp.current * 0.36f).sp,
            )
        }
    }
}

@Composable
private fun EpgChannelHeader(
    channel: Channel,
    program: Program?,
    width: Dp,
) {
    Column(
        modifier = Modifier
            .width(width)
            .height(64.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = channel.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.5f).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
        )
        if (program != null) {
            Text(
                text = program.title,
                color = MaterialTheme.colorScheme.primary,
                fontSize = (LocalBaseSp.current * 0.4f).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NavChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.54f).sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun DayChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
            label,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontSize = (LocalBaseSp.current * 0.5f).sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/** EPG akisini toplar. */
@Composable
private fun <T> produceEpg(
    repository: com.hp.novatv.data.repo.ChannelRepository,
    channelIds: List<Long>,
    dayStart: Long,
): androidx.compose.runtime.State<Map<Long, List<Program>>> {
    val from = dayStart
    val to = dayStart + 86_400_000L
    val flow = remember(channelIds, dayStart) {
        repository.observeEpg(channelIds, from, to)
    }
    return flow.collectAsStateWithLifecycle(initialValue = emptyMap())
}
