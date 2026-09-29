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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.hp.novatv.core.theme.LocalBaseSp
import com.hp.novatv.core.theme.NovaOnSurfaceVariant
import com.hp.novatv.core.util.asDateTime
import com.hp.novatv.core.util.asDuration
import com.hp.novatv.core.util.asFileSize
import com.hp.novatv.core.util.asTime
import com.hp.novatv.ui.components.ChannelLogo
import com.hp.novatv.ui.components.EmptyState
import kotlinx.coroutines.launch
import java.io.File

/**
 * Ekran 8: Kayitlar (DVR).
 * Liste, oynat/sil, zamanlanmis kayit kurulumu, boÅŸ alan uyarisi.
 */
@Composable
fun RecordingsScreen(
    onBack: () -> Unit,
    container: AppContainer = (androidx.compose.ui.platform.LocalContext.current.applicationContext as NovaTvApp).container,
) {
    val repository = container.repository
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val recordings by repository.observeRecordings()
        .collectAsStateWithLifecycle(initial = emptyList())

    val schedules by repository.observeAllSchedules()
        .collectAsStateWithLifecycle(initial = emptyList())

    val freeSpace = remember(recordings) { container.recorder.freeSpaceBytes() }

    var tab by remember { mutableStateOf(0) }

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
            Box(Modifier.width(24.dp))
            TabChip(stringResource(R.string.nav_recordings), tab == 0) { tab = 0 }
            Box(Modifier.width(8.dp))
            TabChip(stringResource(R.string.recording_scheduled), tab == 1) { tab = 1 }
            Box(Modifier.weight(1f))
            Text(
                text = "BoÅŸ: ${freeSpace.asFileSize()}",
                color = NovaOnSurfaceVariant,
                fontSize = (LocalBaseSp.current * 0.46f).sp,
            )
        }

        if (tab == 0) {
            if (recordings.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.empty_no_recordings),
                    body = "OynatÄ±cÄ± ekranÄ±ndaki kayÄ±t dÃ¼ÄŸmesiyle kayÄ±t baÅŸlatabilirsiniz.",
                )
                return@Column
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(recordings, key = { it.id }) { rec ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ChannelLogo(
                                logoUrl = "",
                                name = rec.channelName,
                                modifier = Modifier.size(48.dp),
                            )
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(
                                    text = rec.title.ifBlank { rec.channelName },
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = (LocalBaseSp.current * 0.58f).sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = "${rec.startedAt.asDateTime()} Â· " +
                                        rec.durationMs.asDuration() + " Â· " +
                                        rec.sizeBytes.asFileSize(),
                                    color = NovaOnSurfaceVariant,
                                    fontSize = (LocalBaseSp.current * 0.44f).sp,
                                )
                            }

                            if (rec.isLocal) {
                                SmallAction("Oynat") {
                                    val uri = android.net.Uri.fromFile(File(rec.filePath))
                                    context.startActivity(
                                        com.hp.novatv.ui.PlayerActivity.intent(
                                            context = context,
                                            channelId = rec.channelId,
                                            startEpoch = null,
                                        ),
                                    )
                                }
                            }
                            Box(Modifier.width(8.dp))
                            SmallAction("Sil", destructive = true) {
                                scope.launch {
                                    if (rec.isLocal) File(rec.filePath).delete()
                                    repository.deleteRecording(rec.id)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (schedules.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.recording_scheduled),
                    body = "HenÃ¼z zamanlanmÄ±ÅŸ kayÄ±t yok. Program detay kartÄ±ndan planlayabilirsiniz.",
                )
                return@Column
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(schedules, key = { it.id }) { schedule ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = schedule.programTitle.ifBlank { schedule.channelName },
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = (LocalBaseSp.current * 0.58f).sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${schedule.startEpoch.asDateTime()} â€“ " +
                                        schedule.endEpoch.asTime(),
                                    color = NovaOnSurfaceVariant,
                                    fontSize = (LocalBaseSp.current * 0.44f).sp,
                                )
                            }
                            SmallAction("Sil", destructive = true) {
                                scope.launch { repository.deleteSchedule(schedule.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
            fontSize = (LocalBaseSp.current * 0.52f).sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
internal fun SmallAction(
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (destructive) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Text(
            text = label,
            color = if (destructive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontSize = (LocalBaseSp.current * 0.48f).sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}
