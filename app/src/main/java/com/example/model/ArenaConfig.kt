package com.example.model

enum class ArenaType(
    val title: String,
    val subtitle: String,
    val friction: Float,
    val elasticity: Float,
    val deskBgResId: Int?,
    val borderColor: Long,
    val surfaceThemeColor: Long,
    val surfaceAccentColor: Long,
    val gridOrPattern: String // "WOOD", "GRID", "LAB", "CAFETERIA"
) {
    WOODEN_DESK(
        title = "Classroom Oak Desk",
        subtitle = "Standard grain, pencil marks & balanced physics",
        friction = 1.0f,
        elasticity = 0.72f,
        deskBgResId = null, // Can use procedural wood grain or img_desk_wood
        borderColor = 0xFF4E342E,
        surfaceThemeColor = 0xFFD7CCC8,
        surfaceAccentColor = 0xFF8D6E63,
        gridOrPattern = "WOOD"
    ),
    MATH_NOTEBOOK(
        title = "Graph Notebook Desk",
        subtitle = "Grid lines & compass sketches with crisp friction",
        friction = 1.15f,
        elasticity = 0.65f,
        deskBgResId = null,
        borderColor = 0xFF1E3A8A,
        surfaceThemeColor = 0xFFF8FAFC,
        surfaceAccentColor = 0xFF3B82F6,
        gridOrPattern = "GRID"
    ),
    CHEM_LAB_SLATE(
        title = "Science Lab Table",
        subtitle = "Polished black resin, high speed & extreme slide",
        friction = 0.68f,
        elasticity = 0.88f,
        deskBgResId = null,
        borderColor = 0xFF0F172A,
        surfaceThemeColor = 0xFF1E293B,
        surfaceAccentColor = 0xFF06B6D4,
        gridOrPattern = "LAB"
    ),
    CAFETERIA_TRAY(
        title = "Cafeteria Table",
        subtitle = "Bright molded plastic with high bumper bounce",
        friction = 0.88f,
        elasticity = 0.95f,
        deskBgResId = null,
        borderColor = 0xFFC2410C,
        surfaceThemeColor = 0xFFFFF7ED,
        surfaceAccentColor = 0xFFF97316,
        gridOrPattern = "CAFETERIA"
    )
}

enum class ObstacleKind {
    ERASER,
    RULER,
    SHARPENER,
    PAPERCLIP
}

data class DeskObstacle(
    val id: String,
    val kind: ObstacleKind,
    var x: Float, // Center X in virtual table coords (0..1000)
    var y: Float, // Center Y in virtual table coords (0..1600)
    val width: Float,
    val height: Float,
    var angle: Float = 0f,
    val isMovable: Boolean = false,
    var vx: Float = 0f,
    var vy: Float = 0f
)
