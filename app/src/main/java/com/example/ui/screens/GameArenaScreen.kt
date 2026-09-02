package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMode
import com.example.model.RoundState
import com.example.model.ScreenState
import com.example.ui.components.DeskArenaView
import com.example.ui.components.ScoreBanner
import com.example.ui.components.TrickShotAnnouncement
import com.example.ui.viewmodel.GameViewModel

@Composable
fun GameArenaScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val isInteractivityEnabled = (viewModel.roundState == RoundState.WAITING_FOR_TURN || viewModel.roundState == RoundState.AIMING) && !viewModel.isPaused
    val isAiTurn = (viewModel.selectedGameMode == GameMode.VS_AI && viewModel.currentTurnPlayerId == 2)

    val p1Pen = viewModel.activePens.firstOrNull { it.playerId == 1 } ?: return
    val p2Pen = viewModel.activePens.firstOrNull { it.playerId == 2 } ?: return

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF090D16))) {
        // Main Interactive Table Arena
        DeskArenaView(
            arena = viewModel.selectedArena,
            pens = viewModel.activePens,
            obstacles = viewModel.activeObstacles,
            currentTurnPlayerId = viewModel.currentTurnPlayerId,
            isInteractivityEnabled = isInteractivityEnabled,
            isAiTurn = isAiTurn,
            particles = viewModel.particles,
            isTabletopFlippedPlayer2 = viewModel.isTabletopFlippedPlayer2,
            onPlayerFlick = { vx, vy, hx, hy, powerRatio ->
                viewModel.handlePlayerFlick(vx, vy, hx, hy, powerRatio)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Overlay: Score Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            ScoreBanner(
                gameMode = viewModel.selectedGameMode,
                aiDifficulty = viewModel.selectedAiDifficulty,
                currentRound = viewModel.currentRound,
                targetRounds = viewModel.targetRoundsToWin,
                player1Score = viewModel.player1Score,
                player2Score = viewModel.player2Score,
                player1Pen = p1Pen,
                player2Pen = p2Pen,
                currentTurnPlayerId = viewModel.currentTurnPlayerId,
                isSoundEnabled = viewModel.audioEffects.isSoundEnabled,
                onToggleSound = { viewModel.toggleSound() },
                onResetRound = { viewModel.resetRound(isNewRound = false) },
                onPauseClick = { viewModel.togglePause() }
            )

            // Trick Shot Announcement Banner
            TrickShotAnnouncement(
                badge = viewModel.activeTrickShotBadge,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )
        }

        // Bottom Turn Action Banner
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.90f),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (viewModel.currentTurnPlayerId == 1) Color(0xFF0284C7) else Color(0xFFE11D48)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (viewModel.currentTurnPlayerId == 1) Color(0xFF0284C7) else Color(0xFFE11D48)
                        )
                )

                Column(modifier = Modifier.weight(1f)) {
                    val turnText = when {
                        viewModel.roundState == RoundState.PHYSICS_SIMULATION -> "CLASH IN PROGRESS..."
                        isAiTurn -> "AI IS LINING UP SHOT..."
                        viewModel.currentTurnPlayerId == 1 -> "PLAYER 1'S TURN"
                        else -> "PLAYER 2'S TURN"
                    }

                    val hintText = when {
                        viewModel.roundState == RoundState.PHYSICS_SIMULATION -> "Physics collision active"
                        isAiTurn -> "Calculating angle & power"
                        else -> "Drag back on your pen & release to flick!"
                    }

                    Text(
                        text = turnText,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = hintText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Pause Menu Dialog
        if (viewModel.isPaused) {
            PauseDialog(
                onResume = { viewModel.togglePause() },
                onRestart = { viewModel.startNewMatch() },
                onExitToMenu = {
                    viewModel.togglePause()
                    viewModel.navigateTo(ScreenState.MAIN_MENU)
                }
            )
        }

        // Game Over Dialog
        if (viewModel.roundState == RoundState.MATCH_OVER && viewModel.matchWinner != null) {
            GameOverDialog(
                winnerName = viewModel.matchWinner!!,
                player1Score = viewModel.player1Score,
                player2Score = viewModel.player2Score,
                targetRounds = viewModel.targetRoundsToWin,
                onRematch = { viewModel.startNewMatch() },
                onExitToMenu = { viewModel.navigateTo(ScreenState.MAIN_MENU) }
            )
        }
    }
}

@Composable
fun PauseDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.65f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "MATCH PAUSED",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = onResume,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("resume_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resume Clash")
                    }

                    OutlinedButton(
                        onClick = onRestart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restart_match_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restart Match")
                    }

                    TextButton(
                        onClick = onExitToMenu,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exit_menu_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exit to Menu")
                    }
                }
            }
        }
    }
}

@Composable
fun GameOverDialog(
    winnerName: String,
    player1Score: Int,
    player2Score: Int,
    targetRounds: Int,
    onRematch: () -> Unit,
    onExitToMenu: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.75f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .padding(20.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👑", fontSize = 34.sp)
                    }

                    Text(
                        text = "VICTORY!",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        ),
                        color = Color(0xFFF59E0B)
                    )

                    Text(
                        text = "$winnerName Wins the Desk Clash!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )

                    // Final Score Pill
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "P1: $player1Score",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0284C7)
                                )
                            )
                            Text(
                                text = "—",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "P2: $player2Score",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFE11D48)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onRematch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("rematch_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PLAY AGAIN",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = onExitToMenu,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("game_over_exit_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Main Menu")
                    }
                }
            }
        }
    }
}
