package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val gameMode: String, // "LOCAL_DUO" or "VS_AI"
    val aiDifficulty: String = "",
    val player1Pen: String,
    val player2Pen: String,
    val player1Score: Int,
    val player2Score: Int,
    val winnerName: String,
    val totalRounds: Int,
    val longestClashStreak: Int = 0,
    val knockoutsCount: Int = 0,
    val arenaName: String = "Classic Wood"
)
