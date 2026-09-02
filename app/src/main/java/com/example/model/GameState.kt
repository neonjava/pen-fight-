package com.example.model

enum class GameMode(val displayName: String, val subtitle: String) {
    LOCAL_DUO("Local 2-Player Duo", "Pass & Play on same screen / tabletop clash"),
    VS_AI("Solo vs Smart AI", "Battle against 4 classroom AI difficulties")
}

enum class AiDifficulty(val displayName: String, val title: String, val accuracy: Float, val powerError: Float) {
    ROOKIE("Novice", "Classroom Rookie", 0.65f, 0.35f),
    DESK_BATTLER("Adept", "Desk Champion", 0.82f, 0.20f),
    CLASSROOM_PRO("Expert", "Classroom Pro", 0.93f, 0.10f),
    PEN_NINJA("Master", "Pen Ninja", 0.98f, 0.04f)
}

enum class RoundState {
    WAITING_FOR_TURN,
    AIMING,
    PHYSICS_SIMULATION,
    ROUND_OVER,
    MATCH_OVER
}

enum class ScreenState {
    MAIN_MENU,
    GAME_ARENA,
    PEN_GARAGE,
    ARENA_SELECT,
    MATCH_STATS,
    SETTINGS
}

data class SparkParticle(
    val id: Long,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Long,
    var alpha: Float = 1f,
    val size: Float = 6f,
    var life: Float = 1f,
    val maxLife: Float = 0.5f
)

data class TrickShotBadge(
    val title: String,
    val description: String,
    val icon: String,
    val timestamp: Long = System.currentTimeMillis()
)
