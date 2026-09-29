package com.hp.novatv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.hp.novatv.AppContainer
import com.hp.novatv.NovaTvApp
import com.hp.novatv.R
import com.hp.novatv.core.model.Playlist
import com.hp.novatv.core.model.PlaylistType
import com.hp.novatv.core.theme.LocalBaseSp
import com.hp.novatv.core.util.asDateTime
import com.hp.novatv.ui.components.ChannelLogo
import com.hp.novatv.ui.components.EmptyState
import com.hp.novatv.ui.components.LoadingState
import kotlinx.coroutines.launch

/**
 * Ekran 1: Playlist listesi (bos durum + liste kartlari).
 */
@Composable
fun PlaylistScreen(
    onPlaylistSelected: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    container: AppContainer = (androidx.compose.ui.platform.LocalContext.current.applicationContext as NovaTvApp).container,
) {
    val repository = container.repository
    val playlists by repository.observePlaylists().collectAsStateWithLifecycle(initial = emptyList())
    var loading by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf<Playlist?>(null) }
    var showForm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showForm || editing != null) {
        PlaylistFormDialog(
            initial = editing,
            onDismiss = {
                showForm = false
                editing = null
            },
            onSave = { playlist ->
                scope.launch {
                    val id = repository.savePlaylist(playlist)
                    val result = repository.sync(id)
                    if (!result.success) {
                        // Hata adaptorde tutuldu; liste yine de gorunur
                    }
                    showForm = false
                    editing = null
                }
            },
        )
        return
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        if (loading) {
            LoadingState(stringResource(R.string.loading))
        }

        if (playlists.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.playlist_empty_title),
                body = stringResource(R.string.playlist_empty_body),
                actionLabel = stringResource(R.string.playlist_add),
                onAction = { showForm = true },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 40.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 40.dp, end = 40.dp, bottom = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = (LocalBaseSp.current * 0.9f).sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Box(Modifier.weight(1f))
                        Button(onClick = { showForm = true }) {
                            Text(
                                stringResource(R.string.playlist_add),
                                fontSize = (LocalBaseSp.current * 0.58f).sp,
                            )
                        }
                    }
                }

                items(playlists, key = { it.id }) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onClick = { onPlaylistSelected(playlist.id) },
                        onEdit = { editing = playlist },
                        onRefresh = {
                            scope.launch { repository.sync(playlist.id) }
                        },
                        onDelete = {
                            scope.launch { repository.deletePlaylist(playlist.id) }
                        },
                    )
                }
            }
        }

        // Sag ust: ayarlar
        Box(Modifier.align(Alignment.TopEnd).padding(20.dp)) {
            Button(onClick = onOpenSettings) {
                Text(
                    stringResource(R.string.nav_settings),
                    fontSize = (LocalBaseSp.current * 0.56f).sp,
                )
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { loading = false }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
) {
    val typeLabel = when (playlist.type) {
        PlaylistType.M3U -> stringResource(R.string.type_m3u)
        PlaylistType.XTREAM -> stringResource(R.string.type_xtream)
        PlaylistType.STALKER -> stringResource(R.string.type_stalker)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChannelLogo(
                logoUrl = playlist.logo,
                name = playlist.name,
                modifier = Modifier.size(72.dp),
            )

            Column(Modifier.padding(start = 18.dp).weight(1f)) {
                Text(
                    text = playlist.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (LocalBaseSp.current * 0.72f).sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = typeLabel,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = (LocalBaseSp.current * 0.5f).sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = if (playlist.isSynced) {
                        stringResource(R.string.playlist_channels, playlist.channelCount) +
                            " Â· " + stringResource(
                                R.string.playlist_updated,
                                playlist.lastSync.asDateTime(),
                            )
                    } else {
                        stringResource(R.string.playlist_never)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = (LocalBaseSp.current * 0.48f).sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(stringResource(R.string.playlist_refresh), onRefresh)
                ActionButton(stringResource(R.string.playlist_edit), onEdit)
                ActionButton(stringResource(R.string.playlist_delete), onDelete)
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (LocalBaseSp.current * 0.5f).sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}
