package com.hp.novatv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.hp.novatv.R
import com.hp.novatv.core.model.Playlist
import com.hp.novatv.core.model.PlaylistType
import com.hp.novatv.core.theme.LocalBaseSp
import com.hp.novatv.ui.components.TvTextField

/**
 * Playlist ekleme/duzenleme formu.
 * Tur secimi hangi alanlarin gorunecegini belirler.
 */
@Composable
fun PlaylistFormDialog(
    initial: Playlist?,
    onDismiss: () -> Unit,
    onSave: (Playlist) -> Unit,
) {
    var type by remember { mutableStateOf(initial?.type ?: PlaylistType.M3U) }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var url by remember { mutableStateOf(initial?.url.orEmpty()) }
    var epgUrl by remember { mutableStateOf(initial?.epgUrl.orEmpty()) }
    var username by remember { mutableStateOf(initial?.username.orEmpty()) }
    var password by remember { mutableStateOf(initial?.password.orEmpty()) }
    var mac by remember { mutableStateOf(initial?.mac.orEmpty()) }
    var portalId by remember { mutableStateOf(initial?.portalId.orEmpty()) }
    var logo by remember { mutableStateOf(initial?.logo.orEmpty()) }

    val firstFocus = remember { FocusRequester() }
    val sp = LocalBaseSp.current

    val canSave = url.isNotBlank() && name.isNotBlank()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { firstFocus.requestFocus() }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.62f)
            .heightIn(max = 900.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(
                    if (initial == null) R.string.playlist_add else R.string.playlist_edit,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = (sp * 0.76f).sp,
                modifier = Modifier.padding(bottom = 18.dp),
            )

            // --- Tur secimi ---
            Text(
                text = "Tür",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (sp * 0.5f).sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TypeChip("M3U", type == PlaylistType.M3U) { type = PlaylistType.M3U }
                TypeChip("Xtream", type == PlaylistType.XTREAM) { type = PlaylistType.XTREAM }
                TypeChip("Stalker", type == PlaylistType.STALKER) { type = PlaylistType.STALKER }
            }

            // --- Ortak alanlar ---
            TvTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.field_name),
                focusRequester = firstFocus,
            )
            TvTextField(value = url, onValueChange = { url = it }, label = urlLabel(type))
            TvTextField(
                value = epgUrl,
                onValueChange = { epgUrl = it },
                label = stringResource(R.string.field_epg_url),
            )

            // --- Xtream / Stalker ---
            if (type == PlaylistType.XTREAM) {
                TvTextField(value = username, onValueChange = { username = it }, label = stringResource(R.string.field_username))
                TvTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.field_password),
                    isPassword = true,
                )
            }

            if (type == PlaylistType.STALKER) {
                TvTextField(value = portalId, onValueChange = { portalId = it }, label = stringResource(R.string.field_portal_id))
                TvTextField(value = mac, onValueChange = { mac = it }, label = stringResource(R.string.field_mac))
            }

            TvTextField(value = logo, onValueChange = { logo = it }, label = stringResource(R.string.field_logo))

            // --- Butonlar ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),            ) {
                Button(onClick = onDismiss, enabled = true) {
                    Text(stringResource(R.string.cancel), fontSize = (sp * 0.58f).sp)
                }
                Button(
                    onClick = {
                        onSave(
                            Playlist(
                                id = initial?.id ?: 0L,
                                name = name.trim(),
                                type = type,
                                url = url.trim(),
                                username = username.trim(),
                                password = password,
                                epgUrl = epgUrl.trim(),
                                logo = logo.trim(),
                                portalId = portalId.trim(),
                                mac = mac.trim(),
                                lastSync = initial?.lastSync ?: 0L,
                                channelCount = initial?.channelCount ?: 0,
                            ),
                        )
                    },
                    enabled = canSave,
                ) {
                    Text(stringResource(R.string.save), fontSize = (sp * 0.58f).sp)
                }
            }
        }
    }
}

/** Ilk alana odak verilsin mi? */
@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
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
            fontSize = (LocalBaseSp.current * 0.54f).sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}

private fun urlLabel(type: PlaylistType): String = when (type) {
    PlaylistType.M3U -> "M3U adresi"
    PlaylistType.XTREAM -> "Sunucu adresi (port dahil)"
    PlaylistType.STALKER -> "Portal adresi"
}
