package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MatchHistoryEntity
import com.example.data.PenFightRepository
import com.example.model.*
import com.example.ui.components.ParticleSystem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.math.PI

class GameViewModel(
    application: Application,
    private val repository: PenFightRepository
) : AndroidViewModel(application) {

    val audioEffects = AudioEffects(application)
    private val penPhysics = PenPhysics()
    private val aiController = PenAiController()

    // Database Flows
    val matchHistory: StateFlow<List<MatchHistoryEntity>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMatchesCount: StateFlow<Int> = repository.totalMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val player1WinsCount: StateFlow<Int> = repository.player1Wins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Active Screen
    var currentScreen by mutableStateOf(ScreenState.MAIN_MENU)
        private set

    // Game Configuration
    var selectedGameMode by mutableStateOf(GameMode.LOCAL_DUO)
    var selectedAiDifficulty by mutableStateOf(AiDifficulty.DESK_BATTLER)
    var selectedArena by mutableStateOf(ArenaType.WOODEN_DESK)
    var targetRoundsToWin by mutableStateOf(3)
    var isTabletopFlippedPlayer2 by mutableStateOf(false)
    var enableDeskObstacles by mutableStateOf(true)

    // Player Customizations
    var player1Customization by mutableStateOf(PenCustomization(style = PenStyle.BALLPOINT))
    var player2Customization by mutableStateOf(PenCustomization(style = PenStyle.FOUNTAIN_PEN))

    // Active Match State
    var currentRound by mutableStateOf(1)
        private set
    var player1Score by mutableStateOf(0)
        private set
    var player2Score by mutableStateOf(0)
        private set
    var currentTurnPlayerId by mutableStateOf(1)
        private set
    var roundState by mutableStateOf(RoundState.WAITING_FOR_TURN)
        private set
    var matchWinner by mutableStateOf<String?>(null)
        private set
    var isPaused by mutableStateOf(false)
        private set

    var activeTrickShotBadge by mutableStateOf<TrickShotBadge?>(null)
        private set

    // Pens & Obstacles
    val activePens = mutableStateListOf<PenInstance>()
    val activeObstacles = mutableStateListOf<DeskObstacle>()
    val particles = mutableStateListOf<SparkParticle>()

    private var physicsJob: Job? = null
    private var aiTurnJob: Job? = null
    private var maxClashStreak = 0

    init {
        resetPensToInitialPositions()
    }

    fun navigateTo(screen: ScreenState) {
        currentScreen = screen
        audioEffects.playClickSound()
    }

    fun startNewMatch() {
        currentRound = 1
        player1Score = 0
        player2Score = 0
        currentTurnPlayerId = 1
        matchWinner = null
        isPaused = false
        maxClashStreak = 0
        resetRound(isNewRound = false)
        navigateTo(ScreenState.GAME_ARENA)
    }

    fun resetRound(isNewRound: Boolean = true) {
        physicsJob?.cancel()
        aiTurnJob?.cancel()
        activeTrickShotBadge = null
        particles.clear()

        resetPensToInitialPositions()
        setupArenaObstacles()

        roundState = RoundState.WAITING_FOR_TURN
        isPaused = false

        if (isNewRound) {
            // Alternate starting player or start with player 1
            currentTurnPlayerId = if (currentRound % 2 == 1) 1 else 2
        }

        checkAndTriggerAiIfNeeded()
    }

    private fun resetPensToInitialPositions() {
        activePens.clear()

        // Player 1 at bottom center
        val p1 = PenInstance(
            playerId = 1,
            x = PenPhysics.TABLE_WIDTH / 2f,
            y = PenPhysics.MARGIN_BOTTOM - 200f,
            angle = -PI.toFloat() / 2f, // pointing up towards opponent
            config = player1Customization
        )

        // Player 2 at top center
        val p2 = PenInstance(
            playerId = 2,
            x = PenPhysics.TABLE_WIDTH / 2f,
            y = PenPhysics.MARGIN_TOP + 200f,
            angle = PI.toFloat() / 2f, // pointing down towards opponent
            config = player2Customization
        )

        activePens.add(p1)
        activePens.add(p2)
    }

    private fun setupArenaObstacles() {
        activeObstacles.clear()
        if (!enableDeskObstacles) return

        when (selectedArena) {
            ArenaType.WOODEN_DESK -> {
                // Vinyl Eraser on left side, Pencil Sharpener on right
                activeObstacles.add(
                    DeskObstacle(
                        id = "eraser_1",
                        kind = ObstacleKind.ERASER,
                        x = PenPhysics.MARGIN_LEFT + 140f,
                        y = PenPhysics.TABLE_HEIGHT / 2f,
                        width = 80f,
                        height = 140f,
                        angle = 15f
                    )
                )
                activeObstacles.add(
                    DeskObstacle(
                        id = "sharpener_1",
                        kind = ObstacleKind.SHARPENER,
                        x = PenPhysics.MARGIN_RIGHT - 140f,
                        y = PenPhysics.TABLE_HEIGHT / 2f,
                        width = 90f,
                        height = 90f,
                        angle = -20f
                    )
                )
            }
            ArenaType.MATH_NOTEBOOK -> {
                // Measuring Ruler near center
                activeObstacles.add(
                    DeskObstacle(
                        id = "ruler_1",
                        kind = ObstacleKind.RULER,
                        x = PenPhysics.TABLE_WIDTH / 2f,
                        y = PenPhysics.TABLE_HEIGHT / 2f,
                        width = 320f,
                        height = 42f,
                        angle = 0f
                    )
                )
            }
            ArenaType.CHEM_LAB_SLATE -> {
                // Two metal sharpeners as bumpers
                activeObstacles.add(
                    DeskObstacle(
                        id = "lab_bumper_1",
                        kind = ObstacleKind.SHARPENER,
                        x = PenPhysics.TABLE_WIDTH / 2f - 180f,
                        y = PenPhysics.TABLE_HEIGHT / 2f,
                        width = 80f,
                        height = 80f
                    )
                )
                activeObstacles.add(
                    DeskObstacle(
                        id = "lab_bumper_2",
                        kind = ObstacleKind.SHARPENER,
                        x = PenPhysics.TABLE_WIDTH / 2f + 180f,
                        y = PenPhysics.TABLE_HEIGHT / 2f,
                        width = 80f,
                        height = 80f
                    )
                )
            }
            ArenaType.CAFETERIA_TRAY -> {
                activeObstacles.add(
                    DeskObstacle(
                        id = "paperclip_1",
                        kind = ObstacleKind.PAPERCLIP,
                        x = PenPhysics.TABLE_WIDTH / 2f,
                        y = PenPhysics.TABLE_HEIGHT / 2f - 120f,
                        width = 60f,
                        height = 110f,
                        angle = 45f
                    )
                )
            }
        }
    }

    fun handlePlayerFlick(
        flickVx: Float,
        flickVy: Float,
        hitPointX: Float,
        hitPointY: Float,
        powerRatio: Float
    ) {
        if (roundState != RoundState.WAITING_FOR_TURN && roundState != RoundState.AIMING) return

        val activePen = activePens.firstOrNull { it.playerId == currentTurnPlayerId } ?: return
        if (activePen.isOffTable) return

        audioEffects.playFlickSound(powerRatio)
        penPhysics.applyFlick(activePen, flickVx, flickVy, hitPointX, hitPointY)
        startPhysicsLoop()
    }

    private fun checkAndTriggerAiIfNeeded() {
        if (selectedGameMode == GameMode.VS_AI && currentTurnPlayerId == 2 && roundState == RoundState.WAITING_FOR_TURN) {
            aiTurnJob?.cancel()
            aiTurnJob = viewModelScope.launch {
                delay(800) // AI thinking time
                if (roundState != RoundState.WAITING_FOR_TURN) return@launch

                val aiPen = activePens.firstOrNull { it.playerId == 2 }
                val playerPen = activePens.firstOrNull { it.playerId == 1 }

                if (aiPen != null && playerPen != null && !aiPen.isOffTable && !playerPen.isOffTable) {
                    val shotPlan = aiController.calculateBestShot(
                        aiPen = aiPen,
                        targetPen = playerPen,
                        difficulty = selectedAiDifficulty,
                        arena = selectedArena
                    )
                    audioEffects.playFlickSound(shotPlan.powerRatio)
                    penPhysics.applyFlick(aiPen, shotPlan.flickVx, shotPlan.flickVy, shotPlan.hitPointX, shotPlan.hitPointY)
                    startPhysicsLoop()
                }
            }
        }
    }

    private fun startPhysicsLoop() {
        roundState = RoundState.PHYSICS_SIMULATION
        physicsJob?.cancel()
        physicsJob = viewModelScope.launch {
            var stepCount = 0
            val targetFps = 60
            val dt = 1.0f / targetFps

            while (isActive) {
                stepCount++

                // Update particle life
                ParticleSystem.updateParticles(particles, dt)

                // Run physics simulation step
                penPhysics.updatePhysics(
                    dt = dt,
                    pens = activePens,
                    obstacles = activeObstacles,
                    arena = selectedArena,
                    onPenCollision = { event ->
                        audioEffects.playClashSound(event.impactIntensity)
                        particles.addAll(
                            ParticleSystem.spawnClashSparks(
                                event.contactX,
                                event.contactY,
                                event.impactIntensity
                            )
                        )
                        maxClashStreak = maxOf(maxClashStreak, event.penA.consecutiveHits)
                        if (event.impactIntensity > 0.7f) {
                            showTrickShotBadge(
                                TrickShotBadge(
                                    title = "HEAVY IMPACT!",
                                    description = "Violent clash sends opponent spinning!",
                                    icon = "💥"
                                )
                            )
                        }
                    },
                    onObstacleCollision = { event ->
                        audioEffects.playDeskThudSound()
                        particles.addAll(
                            ParticleSystem.spawnClashSparks(
                                event.contactX,
                                event.contactY,
                                event.impactIntensity * 0.6f,
                                count = 8
                            )
                        )
                    },
                    onPenFall = { fallenPen ->
                        audioEffects.playFallSound()
                        val winnerName = if (fallenPen.playerId == 1) {
                            if (selectedGameMode == GameMode.VS_AI) "AI Opponent" else "Player 2"
                        } else {
                            "Player 1"
                        }
                        showTrickShotBadge(
                            TrickShotBadge(
                                title = "DESK KNOCKOUT!",
                                description = "$winnerName knocks opponent off the table!",
                                icon = "🏆"
                            )
                        )
                    }
                )

                // Check if pens are still moving or falling
                val anyPenMoving = activePens.any { it.isMoving }
                val anyPenFalling = activePens.any { it.isOffTable && it.fallAlpha > 0.05f }

                if (!anyPenMoving && !anyPenFalling && stepCount > 10) {
                    // Simulation settled
                    delay(300)
                    evaluateRoundSettlement()
                    break
                }

                delay(16) // ~60fps
            }
        }
    }

    private fun evaluateRoundSettlement() {
        val p1 = activePens.firstOrNull { it.playerId == 1 }
        val p2 = activePens.firstOrNull { it.playerId == 2 }

        val p1Fell = p1?.isOffTable == true
        val p2Fell = p2?.isOffTable == true

        if (p1Fell && p2Fell) {
            // Draw round - replay round
            showTrickShotBadge(
                TrickShotBadge(
                    title = "DOUBLE KNOCKOUT!",
                    description = "Both pens flew off! Round resets.",
                    icon = "⚡"
                )
            )
            viewModelScope.launch {
                delay(1400)
                resetRound(isNewRound = false)
            }
        } else if (p1Fell) {
            // Player 2 wins round
            player2Score++
            checkRoundOrMatchEnd(winningPlayerId = 2)
        } else if (p2Fell) {
            // Player 1 wins round
            player1Score++
            checkRoundOrMatchEnd(winningPlayerId = 1)
        } else {
            // No one fell off, switch turn to next player!
            currentTurnPlayerId = if (currentTurnPlayerId == 1) 2 else 1
            roundState = RoundState.WAITING_FOR_TURN
            checkAndTriggerAiIfNeeded()
        }
    }

    private fun checkRoundOrMatchEnd(winningPlayerId: Int) {
        if (player1Score >= targetRoundsToWin || player2Score >= targetRoundsToWin) {
            // Match Won!
            val winner = if (player1Score >= targetRoundsToWin) "Player 1" else {
                if (selectedGameMode == GameMode.VS_AI) "AI (${selectedAiDifficulty.displayName})" else "Player 2"
            }
            matchWinner = winner
            roundState = RoundState.MATCH_OVER
            audioEffects.playWinFanfare()
            particles.addAll(ParticleSystem.spawnConfetti())

            // Record match to Room DB
            viewModelScope.launch {
                repository.recordMatch(
                    MatchHistoryEntity(
                        gameMode = selectedGameMode.name,
                        aiDifficulty = if (selectedGameMode == GameMode.VS_AI) selectedAiDifficulty.name else "",
                        player1Pen = player1Customization.style.displayName,
                        player2Pen = player2Customization.style.displayName,
                        player1Score = player1Score,
                        player2Score = player2Score,
                        winnerName = winner,
                        totalRounds = currentRound,
                        longestClashStreak = maxClashStreak,
                        arenaName = selectedArena.title
                    )
                )
            }
        } else {
            // Next Round
            roundState = RoundState.ROUND_OVER
            audioEffects.playWinFanfare()
            viewModelScope.launch {
                delay(1600)
                currentRound++
                resetRound(isNewRound = true)
            }
        }
    }

    private fun showTrickShotBadge(badge: TrickShotBadge) {
        activeTrickShotBadge = badge
        viewModelScope.launch {
            delay(2200)
            if (activeTrickShotBadge == badge) {
                activeTrickShotBadge = null
            }
        }
    }

    fun togglePause() {
        isPaused = !isPaused
        audioEffects.playClickSound()
    }

    fun toggleSound() {
        audioEffects.isSoundEnabled = !audioEffects.isSoundEnabled
    }

    fun toggleHaptics() {
        audioEffects.isHapticsEnabled = !audioEffects.isHapticsEnabled
    }

    fun clearAllMatchHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
