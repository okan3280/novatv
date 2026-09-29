package com.hp.novatv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.hp.novatv.AppContainer
import com.hp.novatv.NovaTvApp
import com.hp.novatv.R
import com.hp.novatv.core.model.Program
import com.hp.novatv.core.theme.LocalBaseSp
import com.hp.novatv.core.theme.NovaLive
import com.hp.novatv.core.theme.NovaOnSurfaceVariant
import com.hp.novatv.core.theme.NovaSurfaceVariant
import com.hp.novatv.core.util.asDate
import com.hp.novatv.core.util.asTime
import com.hp.novatv.core.util.programElapsed
import com.hp.novatv.core.util.startOfDay
import com.hp.novatv.ui.components.ChannelBadge
import com.hp.novatv.ui.components.EmptyState

/**
 * Ekran 7: Catch-up.
 * Tarih kolonu + saat listesi + onizleme. `catchup` URL'i uretilir.
 */
@Composable
fun CatchupScreen(
    channelId: Long,
    onBack: () -> Unit,
    container: AppContainer = (androidx.compose.ui.platform.LocalContext.current.applicationContext as NovaTvApp).container,
) {
    val repository = container.repository
    val context = androidx.compose.ui.platform.LocalContext.current

    val channel by repository.observeChannel(channelId)
        .collectAsStateWithLifecycle(initial = null)

    var dayOffset by remember { mutableIntStateOf(1) } // 1 = dun
    val dayStart = remember(dayOffset) { startOfDay() - dayOffset * 86_400_000L }
    val now = System.currentTimeMillis() / 1000

    val programs by repository
        .observeChannelPrograms(channelId, dayStart, dayStart + 86_400_000L)
        .collectAsStateWithLifecycle(initial = emptyList())

    val catchupDays = channel?.catchupDays ?: 0
    val maxDays = catchupDays.coerceIn(1, 7)

    // Yalnizca gecmise donuk programlar
    val past = remember(programs, now) {
        programs.filter { it.endEpoch < now }
            .sortedByDescending { it.startEpoch }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackChip(stringResource(R.string.back), onBack)
            Column(Modifier.padding(start = 18.dp)) {
                Text(
                    text = channel?.name.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (LocalBaseSp.current * 0.7f).sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (catchupDays > 0) {
                        stringResource(R.string.catchup_days, catchupDays)
                    } else {
                        stringResource(R.string.catchup_unavailable)
                    },
                    color = NovaOnSurfaceVariant,
                    fontSize = (LocalBaseSp.current * 0.46f).sp,
                )
            }
        }

        // Tarih kolonu
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(74.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (offset in 1..maxDays) {
                val epoch = startOfDay() - offset * 86_400_000L
                item {
                    DayPill(
                        label = (epoch * 1000).asDate(),
                        selected = dayOffset == offset,
                        onClick = { dayOffset = offset },
                    )
                }
            }
        }

        if (catchupDays == 0) {
            EmptyState(
                title = stringResource(R.string.catchup_unavailable),
                body = "Bu kanal iÃ§in geÃ§miÅŸ yayÄ±n sunucu tarafÄ±ndan desteklenmiyor.",
            )
            return@Column
        }

        if (past.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.empty_no_catchup),
                body = "SeÃ§ili gÃ¼n iÃ§in kayÄ±tlÄ± program bulunamadÄ±.",
            )
            return@Column
        }

        // Saat listesi
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(past, key = { it.id }) { program ->
                CatchupRow(
                    program = program,
                    channelId = channelId,
                    onPlay = {
                        context.startActivity(
                            com.hp.novatv.ui.PlayerActivity.intent(
                                context = context,
                                channelId = channelId,
                                startEpoch = program.startEpoch,
                                durationMs = program.durationMs,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun DayPill(label: String, selected: Boolean, onClick: () -> Unit) {
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun CatchupRow(
    program: Program,
    channelId: Long,
    onPlay: () -> Unit,
) {
    Surface(
        onClick = onPlay,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (program.icon.isNotBlank()) {
                AsyncImage(
                    model = program.icon,
                    contentDescription = program.title,
                    modifier = Modifier
                        .width(96.dp)
                        .height(56.dp)
                        .background(NovaSurfaceVariant, RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop,
                )
                Box(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = program.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (LocalBaseSp.current * 0.58f).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${program.startEpoch.asTime()} â€“ ${program.endEpoch.asTime()}",
                    color = NovaOnSurfaceVariant,
                    fontSize = (LocalBaseSp.current * 0.44f).sp,
                )
            }
            ChannelBadge("Ä°ZLE", background = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Ekran 4: Program detay karti.
 * Afis, baslik, saat araligi, sure, HD/STEREO rozetleri.
 */
@Composable
fun ProgramDetailScreen(
    channelId: Long,
    programId: Long,
    onBack: () -> Unit,
    container: AppContainer = (androidx.compose.ui.platform.LocalContext.current.applicationContext as NovaTvApp).container,
) {
    val repository = container.repository
    val context = androidx.compose.ui.platform.LocalContext.current

    val channel by repository.observeChannel(channelId)
        .collectAsStateWithLifecycle(initial = null)

    val programs by repository
        .observeChannelPrograms(channelId, startOfDay() - 3 * 86_400_000L, startOfDay() + 86_400_000L)
        .collectAsStateWithLifecycle(initial = emptyList())

    val program = remember(programs, programId) {
        programs.firstOrNull { it.id == programId }
    }

    if (program == null) {
        EmptyState(
            title = stringResource(R.string.error_generic),
            body = "Program bilgisi bulunamadÄ±.",
            actionLabel = stringResource(R.string.back),
            onAction = onBack,
        )
        return
    }

    val now = System.currentTimeMillis() / 1000
    val isPast = program.endEpoch < now

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
    ) {
        BackChip(stringResource(R.string.back), onBack)

        Row(Modifier.padding(top = 22.dp)) {
            // Afis
            Box(
                modifier = Modifier
                    .width(380.dp)
                    .height(214.dp)
                    .background(NovaSurfaceVariant, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (program.icon.isNotBlank()) {
                    AsyncImage(
                        model = program.icon,
                        contentDescription = program.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        text = program.title.take(24),
                        color = NovaOnSurfaceVariant,
                        fontSize = (LocalBaseSp.current * 0.6f).sp,
                    )
                }
            }

            Column(Modifier.padding(start = 28.dp).weight(1f)) {
                Text(
                    text = channel?.name.orEmpty(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = (LocalBaseSp.current * 0.56f).sp,
                )
                Text(
                    text = program.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (LocalBaseSp.current * 0.86f).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )

                // Rozetler
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ChannelBadge("HD")
                    ChannelBadge("STEREO")
                    if (program.category.isNotBlank()) ChannelBadge(program.category)
                    if (program.rating.isNotBlank()) ChannelBadge(program.rating)
                    if (isPast) {
                        ChannelBadge("GEÃ‡MÄ°Å", background = NovaOnSurfaceVariant)
                    } else {
                        ChannelBadge("CANLI", background = NovaLive)
                    }
                }

                // Saat araligi + sure
                Text(
                    text = "${program.startEpoch.asTime()} â€“ ${program.endEpoch.asTime()}" +
                        "  Â·  " + program.durationMs.asDurationLabel(),
                    color = NovaOnSurfaceVariant,
                    fontSize = (LocalBaseSp.current * 0.5f).sp,
                    modifier = Modifier.padding(top = 12.dp),
                )

                // Ilerleme (gecmis icin)
                if (!isPast) {
                    val progress = programElapsed(now, program.startEpoch, program.endEpoch)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(top = 8.dp)
                            .background(NovaSurfaceVariant, RoundedCornerShape(2.dp)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress)
                                .height(4.dp)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }

                if (program.description.isNotBlank()) {
                    Text(
                        text = program.description,
                        color = NovaOnSurfaceVariant,
                        fontSize = (LocalBaseSp.current * 0.48f).sp,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }

                // Aksiyonlar
                Row(
                    modifier = Modifier.padding(top = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        onClick = {
                            context.startActivity(
                                com.hp.novatv.ui.PlayerActivity.intent(
                                    context = context,
                                    channelId = channelId,
                                    startEpoch = if (isPast) program.startEpoch else null,
                                    durationMs = if (isPast) program.durationMs else null,
                                ),
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = if (isPast) stringResource(R.string.catchup_start)
                            else stringResource(R.string.play),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = (LocalBaseSp.current * 0.58f).sp,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                        )
                    }

                    channel?.let { ch ->
                        Surface(
                            onClick = {
                                context.startActivity(
                                    com.hp.novatv.ui.PlayerActivity.intent(context, ch.id),
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = stringResource(R.string.player_live),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = (LocalBaseSp.current * 0.58f).sp,
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Long.asDurationLabel(): String {
    val totalMin = this / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h} sa ${m} dk" else "${m} dk"
}
