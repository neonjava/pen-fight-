package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchHistoryDao {
    @Query("SELECT * FROM match_history ORDER BY timestamp DESC LIMIT 50")
    fun getAllMatches(): Flow<List<MatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchHistoryEntity): Long

    @Query("SELECT COUNT(*) FROM match_history")
    fun getTotalMatchesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM match_history WHERE winnerName = 'Player 1'")
    fun getPlayer1WinsCount(): Flow<Int>

    @Query("DELETE FROM match_history")
    suspend fun clearAllHistory()
}
