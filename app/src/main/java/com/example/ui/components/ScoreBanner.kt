package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiDifficulty
import com.example.model.GameMode
import com.example.model.PenInstance
import com.example.model.TrickShotBadge

@Composable
fun ScoreBanner(
    gameMode: GameMode,
    aiDifficulty: AiDifficulty,
    currentRound: Int,
    targetRounds: Int,
    player1Score: Int,
    player2Score: Int,
    player1Pen: PenInstance,
    player2Pen: PenInstance,
    currentTurnPlayerId: Int,
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onResetRound: () -> Unit,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Top Row: Round Info & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Round pill badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "ROUND $currentRound / $targetRounds",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Toggle Audio",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onResetRound,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("reset_round_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Round",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onPauseClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("pause_match_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Scoreboard Duel Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player 1 Card
                PlayerScoreCard(
                    playerName = "Player 1",
                    penName = player1Pen.config.style.displayName,
                    score = player1Score,
                    targetRounds = targetRounds,
                    isCurrentTurn = (currentTurnPlayerId == 1),
                    teamColor = Color(0xFF0284C7),
                    isBottomAligned = false,
                    modifier = Modifier.weight(1f)
                )

                // VS Badge
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0284C7), Color(0xFFE11D48))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VS",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black)
                    )
                }

                // Player 2 / AI Card
                val p2Name = if (gameMode == GameMode.VS_AI) "AI (${aiDifficulty.displayName})" else "Player 2"
                PlayerScoreCard(
                    playerName = p2Name,
                    penName = player2Pen.config.style.displayName,
                    score = player2Score,
                    targetRounds = targetRounds,
                    isCurrentTurn = (currentTurnPlayerId == 2),
                    teamColor = Color(0xFFE11D48),
                    isBottomAligned = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PlayerScoreCard(
    playerName: String,
    penName: String,
    score: Int,
    targetRounds: Int,
    isCurrentTurn: Boolean,
    teamColor: Color,
    isBottomAligned: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrentTurn) teamColor.copy(alpha = 0.15f) else Color.Transparent,
        border = if (isCurrentTurn) androidx.compose.foundation.BorderStroke(2.dp, teamColor) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = if (isBottomAligned) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(teamColor)
                )
                Text(
                    text = playerName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }

            Text(
                text = penName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Score Stars / Dots
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 1..targetRounds) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (i <= score) teamColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun TrickShotAnnouncement(
    badge: TrickShotBadge?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = badge != null,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { -it / 2 } + fadeOut(),
        modifier = modifier
    ) {
        if (badge != null) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF59E0B))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = badge.icon,
                        fontSize = 22.sp
                    )
                    Column {
                        Text(
                            text = badge.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF59E0B)
                            )
                        )
                        Text(
                            text = badge.description,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                        )
                    }
                }
            }
        }
    }
}
