package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.media.AudioPlayerController
import com.example.ui.theme.HeritageGold
import com.example.ui.theme.LocalIsDarkThemeActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersistentGlassMiniPlayer(
    onSpeedChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by AudioPlayerController.state.collectAsStateWithLifecycle()
    val isDark = LocalIsDarkThemeActive.current

    // If offline Arabic TTS is reading a Dua/Ziyarat/Hadith
    if (state.isSpeakingTts && state.ttsTitle != null) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF132A27).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.76f),
            border = BorderStroke(1.dp, HeritageGold.copy(alpha = 0.45f)),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("tts_mini_player")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(9.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.ttsTitle!!,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "قراءة صوتية عربية محلية (أوفلاين)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { AudioPlayerController.stopArabicTts() }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close_player)
                    )
                }
            }
        }
        return
    }

    val track = state.currentTrack ?: return
    var showFullSheet by remember { mutableStateOf(false) }

    val glassColor = if (isDark) {
        Color(0xFF132A27).copy(alpha = 0.76f)
    } else {
        Color.White.copy(alpha = 0.74f)
    }
    val glassBorder = if (isDark) {
        Color.White.copy(alpha = 0.20f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = glassColor,
        border = BorderStroke(1.dp, glassBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { showFullSheet = true }
            .testTag("persistent_mini_player")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(9.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (state.isOfflineSourceActive) {
                            Icon(
                                imageVector = Icons.Outlined.OfflinePin,
                                contentDescription = "يعمل أوفلاين",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = state.errorMessage ?: buildString {
                            append(track.performer)
                            append(" · ")
                            append(state.formattedPosition())
                            if (state.isOfflineSourceActive) append(" · أوفلاين")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.errorMessage != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (state.isPreparing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(28.dp)
                            .padding(2.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    IconButton(
                        onClick = { AudioPlayerController.togglePlayPause() },
                        modifier = Modifier.testTag("mini_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_pause),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = { AudioPlayerController.stopAndDismiss() },
                    modifier = Modifier.testTag("mini_player_close")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close_player),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            LinearProgressIndicator(
                progress = { state.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            )
        }
    }

    if (showFullSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFullSheet = false },
            containerColor = if (isDark) Color(0xFF102422).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, HeritageGold.copy(alpha = 0.45f)),
                    modifier = Modifier.size(84.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (state.isOfflineSourceActive) "${track.category} · محفوظ أوفلاين ✓" else track.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = track.performer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                Slider(
                    value = state.progressFraction,
                    onValueChange = { AudioPlayerController.seekToFraction(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(state.formattedPosition(), style = MaterialTheme.typography.labelSmall)
                    Text(state.formattedDuration(), style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(onClick = { AudioPlayerController.toggleRepeatOne() }) {
                        Icon(
                            imageVector = if (state.isRepeatOne) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = "تكرار المقطع",
                            tint = if (state.isRepeatOne) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { AudioPlayerController.skipNext() }) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "التالي",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    FilledIconButton(
                        onClick = { AudioPlayerController.togglePlayPause() },
                        modifier = Modifier.size(60.dp)
                    ) {
                        if (state.isPreparing) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play_pause),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    IconButton(onClick = { AudioPlayerController.skipPrevious() }) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "السابق",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("السرعة:", style = MaterialTheme.typography.labelMedium)
                    listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { spd ->
                        GlassFilterChip(
                            label = "${spd}×",
                            selected = kotlin.math.abs(state.speed - spd) < 0.05f,
                            onClick = { onSpeedChanged(spd) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
