package com.hp.novatv.ui.screens

import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.hp.novatv.ui.components.Surface
import androidx.tv.material3.Text
import com.hp.novatv.AppContainer
import com.hp.novatv.R
import com.hp.novatv.ui.PlayerActivity
import com.hp.novatv.core.model.Channel
import com.hp.novatv.core.model.Program
import com.hp.novatv.core.theme.LocalBaseSp
import com.hp.novatv.core.theme.NovaLive
import com.hp.novatv.core.theme.NovaOnSurfaceVariant
import com.hp.novatv.core.theme.NovaRecord
import com.hp.novatv.core.util.asTime
import com.hp.novatv.data.prefs.PlayerEnginePref
import com.hp.novatv.player.Engine
import com.hp.novatv.ui.components.ChannelLogo
import com.hp.novatv.ui.components.LoadingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * Ekran 5: Oynatici.
 *
 * - Ustte program karti (kanal + program bilgisi)
 * - Iki kademeli ilerleme (program / oturum)
 * - Altta kanal karuseli + acma chevron'i
 * - Uzaktan kumanda kontrolleri (geri = cikis)
 * - PiP destekli
 */
@Composable
fun PlayerScreen(
    channelId: Long,
    startEpoch: Long?,
    durationMs: Long?,
    container: AppContainer,
    onExit: () -> Unit,
    onOpenCatchup: () -> Unit,
) {
    val repository = container.repository
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val channel by repository.observeChannel(channelId)
        .collectAsStateWithLifecycle(initialValue = null)

    var engine by remember { mutableStateOf(Engine.EXOPLAYER) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isPip by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var chromeExpanded by remember { mutableStateOf(false) }

    val player = remember { container.exoEngine.defaultPlayer }
    val recorder = container.recorder

    val currentProgram by repository.observeCurrentProgram(channelId)
        .collectAsStateWithLifecycle(initialValue = null)

    val playlistChannels by repository.observeChannels(channel?.playlistId ?: 0L)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Oynatma URL'ini cozer ve dogru motoru baslatir.
    // Compose icinde suspend fonksiyon tanimlamak sorunlu; bu yuzden
    // top-level yardimci fonksiyon kullanilir.
    suspend fun reloadCurrent() {
        val ch = channel ?: return
        val url = repository.resolvePlayUrl(ch, startEpoch, durationMs)
        if (url.isBlank()) return

        val enginePref = container.settings.settings.first().playerEngine
        val preferVlc = when (enginePref) {
            PlayerEnginePref.VLC -> true
            PlayerEnginePref.EXOPLAYER -> false
            PlayerEnginePref.AUTO -> ch.streamType in setOf(
                com.hp.novatv.core.model.StreamType.TS,
                com.hp.novatv.core.model.StreamType.RTMP,
                com.hp.novatv.core.model.StreamType.RTSP,
                com.hp.novatv.core.model.StreamType.KODI,
            )
        }

        if (engine == Engine.VLC || preferVlc) {
            engine = Engine.VLC
            if (!container.vlcEngine.play(url)) {
                // VLC da basaramadi: ExoPlayer'a geri don
                engine = Engine.EXOPLAYER
                container.exoEngine.play(player, ch, url)
            }
        } else {
            container.exoEngine.play(player, ch, url)
        }
    }

    // --- Oynatici olaylari ---
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED) {
                    // Canli yayin bitti: yeniden baglan
                    scope.launch { reloadCurrent() }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // ExoPlayer basarisiz: VLC'ye gec
                if (engine == Engine.EXOPLAYER) {
                    engine = Engine.VLC
                    errorMessage = null
                } else {
                    errorMessage = error.localizedMessage
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }


    // Kanal yuklendiginde oynat
    LaunchedEffect(channel?.id, startEpoch) {
        if (channel != null) reloadCurrent()
    }

    // Konum sayaci
    LaunchedEffect(Unit) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0)
            duration = player.duration.takeIf { it > 0 } ?: 0
            delay(1000)
        }
    }

    // Kontroller 5 saniye sonra gizlenir (oynatiyorken)
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(5000)
            controlsVisible = false
        }
    }

    // Geri tusu: once kontrolleri kapat, sonra cik
    BackHandler(enabled = true) {
        if (controlsVisible) controlsVisible = false else onExit()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // --- Video yuzeyi ---
        if (engine == Engine.EXOPLAYER) {
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
        } else {
            AndroidView(
                factory = { ctx -> SurfaceView(ctx) },
                update = { view ->
                    if (view.tag != "bound") {
                        view.tag = "bound"
                        container.vlcEngine.attach(view)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // --- Yukleme ---
        if (isBuffering) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingState(stringResource(R.string.loading))
            }
        }

        // --- Hata ---
        errorMessage?.let { msg ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.player_error) + "\n" + msg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = (LocalBaseSp.current * 0.7f).sp,
                )
            }
        }

        // --- Ust: program karti ---
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            TopInfoCard(
                channel = channel,
                program = currentProgram,
                isLive = true,
                isRecording = isRecording,
                startEpoch = startEpoch,
                durationMs = durationMs,
                onRecord = {
                    if (isRecording) {
                        scope.launch {
                            recorder.stop()?.let { rec ->
                                repository.addRecording(rec)
                            }
                            recorder.reset()
                            isRecording = false
                        }
                    } else {
                        isRecording = true
                        scope.launch {
                            val ch = channel ?: return@launch
                            // Kayit dosyasi DataSource uzerinden akitilir:
                            // oynatici yeniden kurulmalı
                            if (recorder.start(ch)) {
                                reloadCurrent()
                            } else {
                                isRecording = false
                            }
                        }
                    }
                },
                modifier = Modifier.padding(top = 26.dp, start = 30.dp, end = 30.dp),
            )
        }

        // --- Alt: kanal karuseli + ilerleme ---
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomBar(
                position = position,
                duration = duration,
                channels = playlistChannels,
                currentChannelId = channelId,
                chromeExpanded = chromeExpanded,
                onToggleChrome = { chromeExpanded = !chromeExpanded },
                onSelectChannel = { id ->
                    scope.launch {
                        container.settings.setLastChannel(id)
                        context.startActivity(
                            PlayerActivity.intent(context, id),
                        )
                    }
                },
                onBack = onExit,
            )
        }
    }
}

@Composable
private fun TopInfoCard(
    channel: Channel?,
    program: Program?,
    isLive: Boolean,
    isRecording: Boolean,
    startEpoch: Long?,
    durationMs: Long?,
    onRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.62f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChannelLogo(
            logoUrl = channel?.logo.orEmpty(),
            name = channel?.name.orEmpty(),
            modifier = Modifier.size(58.dp),
        )

        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLive) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(NovaLive)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "CANLI",
                            color = Color.White,
                            fontSize = (LocalBaseSp.current * 0.4f).sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Box(Modifier.width(8.dp))
                }
                if (isRecording) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(NovaRecord)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "KAYIT",
                            color = Color.White,
                            fontSize = (LocalBaseSp.current * 0.4f).sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Box(Modifier.width(8.dp))
                }
                Text(
                    text = channel?.name.orEmpty(),
                    color = Color.White,
                    fontSize = (LocalBaseSp.current * 0.68f).sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val timeRange = if (startEpoch != null && durationMs != null) {
                "${startEpoch.asTime()} – ${(startEpoch + durationMs / 1000).asTime()}"
            } else {
                program?.let { "${it.startEpoch.asTime()} – ${it.endEpoch.asTime()}" }
                    ?: stringResource(R.string.player_no_info)
            }

            Text(
                text = program?.title ?: timeRange,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = (LocalBaseSp.current * 0.5f).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                text = timeRange,
                color = NovaOnSurfaceVariant,
                fontSize = (LocalBaseSp.current * 0.42f).sp,
            )
        }

        Surface(
            onClick = onRecord,
            shape = RoundedCornerShape(8.dp),
            color = if (isRecording) NovaRecord else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = if (isRecording) "#" else "o",
                color = Color.White,
                fontSize = (LocalBaseSp.current * 0.6f).sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun BottomBar(
    position: Long,
    duration: Long,
    channels: List<Channel>,
    currentChannelId: Long,
    chromeExpanded: Boolean,
    onToggleChrome: () -> Unit,
    onSelectChannel: (Long) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(vertical = 14.dp),
    ) {
        // Iki kademeli ilerleme
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.18f)),
        ) {
            if (duration > 0) {
                Box(
                    Modifier
                        .fillMaxWidth((position.toFloat() / duration).coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }

        // Kanal karuseli
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(channels.take(24), key = { it.id }) { ch ->
                    val selected = ch.id == currentChannelId
                    Surface(
                        onClick = { if (!selected) onSelectChannel(ch.id) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.White.copy(alpha = 0.12f)
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = ch.name,
                                color = Color.White,
                                fontSize = (LocalBaseSp.current * 0.46f).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Acma chevron'i
            Surface(
                onClick = onToggleChrome,
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.12f),
            ) {
                Text(
                    text = if (chromeExpanded) "" else "",
                    color = Color.White,
                    fontSize = (LocalBaseSp.current * 0.55f).sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/** Geri tusu yakalayici. */
@Composable
private fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}
