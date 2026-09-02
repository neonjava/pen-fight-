package com.example.model

import androidx.compose.ui.graphics.Color

enum class PenStyle(
    val displayName: String,
    val description: String,
    val baseLength: Float, // in virtual table units
    val baseWidth: Float,
    val mass: Float, // impacts collision inertia
    val speedMultiplier: Float, // flick speed
    val spinMultiplier: Float, // spin susceptibility & torque
    val frictionMultiplier: Float,
    val defaultBodyColor: Long,
    val defaultCapColor: Long,
    val defaultGripColor: Long,
    val defaultNibColor: Long
) {
    BALLPOINT(
        displayName = "Classic Ballpoint",
        description = "Lightweight, aerodynamic and fast. The undisputed classroom classic.",
        baseLength = 230f,
        baseWidth = 32f,
        mass = 1.0f,
        speedMultiplier = 1.15f,
        spinMultiplier = 1.1f,
        frictionMultiplier = 0.95f,
        defaultBodyColor = 0xFF1976D2,
        defaultCapColor = 0xFF0D47A1,
        defaultGripColor = 0xFF90CAF9,
        defaultNibColor = 0xFFB0BEC5
    ),
    FOUNTAIN_PEN(
        displayName = "Royal Fountain Pen",
        description = "Heavy metallic body with golden nib. Devastating knockout power.",
        baseLength = 240f,
        baseWidth = 36f,
        mass = 1.45f,
        speedMultiplier = 0.9f,
        spinMultiplier = 1.25f,
        frictionMultiplier = 0.9f,
        defaultBodyColor = 0xFF1E293B,
        defaultCapColor = 0xFF0F172A,
        defaultGripColor = 0xFFD4AF37,
        defaultNibColor = 0xFFFFD700
    ),
    GEL_CLICKER(
        displayName = "Speed Gel Clicker",
        description = "Retractable spring-loaded clicker with smooth glide and swift rebound.",
        baseLength = 220f,
        baseWidth = 30f,
        mass = 0.88f,
        speedMultiplier = 1.3f,
        spinMultiplier = 1.05f,
        frictionMultiplier = 0.85f,
        defaultBodyColor = 0xFFE11D48,
        defaultCapColor = 0xFF9F1239,
        defaultGripColor = 0xFF334155,
        defaultNibColor = 0xFFCBD5E1
    ),
    HIGHLIGHTER(
        displayName = "Chubby Highlighter",
        description = "Broad thick silhouette with high desk grip. Hard for opponents to budge.",
        baseLength = 205f,
        baseWidth = 46f,
        mass = 1.55f,
        speedMultiplier = 0.8f,
        spinMultiplier = 0.7f,
        frictionMultiplier = 1.25f,
        defaultBodyColor = 0xFF84CC16,
        defaultCapColor = 0xFF4D7C0F,
        defaultGripColor = 0xFFA3E635,
        defaultNibColor = 0xFFBEF264
    ),
    FINELINER(
        displayName = "Precision Fineliner",
        description = "Fine needle tip with maximum rotational torque. Deadly spin master.",
        baseLength = 235f,
        baseWidth = 28f,
        mass = 0.92f,
        speedMultiplier = 1.2f,
        spinMultiplier = 1.6f,
        frictionMultiplier = 0.9f,
        defaultBodyColor = 0xFF6366F1,
        defaultCapColor = 0xFF3730A3,
        defaultGripColor = 0xFF818CF8,
        defaultNibColor = 0xFF94A3B8
    ),
    CHUNKY_4COLOR(
        displayName = "Titan 4-in-1 Chunky",
        description = "Ultra heavyweight 4-color pen. Hits like a freight train on collision.",
        baseLength = 245f,
        baseWidth = 44f,
        mass = 1.75f,
        speedMultiplier = 0.85f,
        spinMultiplier = 0.9f,
        frictionMultiplier = 1.05f,
        defaultBodyColor = 0xFF0284C7,
        defaultCapColor = 0xFF0369A1,
        defaultGripColor = 0xFF38BDF8,
        defaultNibColor = 0xFF64748B
    )
}

data class PenCustomization(
    val style: PenStyle = PenStyle.BALLPOINT,
    val bodyColor: Long = style.defaultBodyColor,
    val capColor: Long = style.defaultCapColor,
    val gripColor: Long = style.defaultGripColor,
    val nibColor: Long = style.defaultNibColor,
    val customName: String = style.displayName
)

data class PenInstance(
    val playerId: Int, // 1 or 2
    var x: Float, // Center X in virtual table coords (0..1000)
    var y: Float, // Center Y in virtual table coords (0..1600)
    var vx: Float = 0f,
    var vy: Float = 0f,
    var angle: Float = 0f, // in radians
    var angularVelocity: Float = 0f, // in radians/sec
    val config: PenCustomization = PenCustomization(),
    var isOffTable: Boolean = false,
    var fallScale: Float = 1f,
    var fallAlpha: Float = 1f,
    var fallRotation: Float = 0f,
    var isMoving: Boolean = false,
    var consecutiveHits: Int = 0
) {
    val length: Float get() = config.style.baseLength
    val width: Float get() = config.style.baseWidth
    val mass: Float get() = config.style.mass
    val momentOfInertia: Float get() = (mass * (length * length + width * width)) / 12f

    // Tip position (one end)
    fun getTipX(): Float = x + (length / 2f) * kotlin.math.cos(angle)
    fun getTipY(): Float = y + (length / 2f) * kotlin.math.sin(angle)

    // Cap position (opposite end)
    fun getCapX(): Float = x - (length / 2f) * kotlin.math.cos(angle)
    fun getCapY(): Float = y - (length / 2f) * kotlin.math.sin(angle)

    fun resetState(startX: Float, startY: Float, startAngle: Float) {
        x = startX
        y = startY
        vx = 0f
        vy = 0f
        angle = startAngle
        angularVelocity = 0f
        isOffTable = false
        fallScale = 1f
        fallAlpha = 1f
        fallRotation = 0f
        isMoving = false
        consecutiveHits = 0
    }
}
