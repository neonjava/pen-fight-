package com.example.data

import kotlinx.coroutines.flow.Flow

class PenFightRepository(private val matchHistoryDao: MatchHistoryDao) {
    val allMatches: Flow<List<MatchHistoryEntity>> = matchHistoryDao.getAllMatches()
    val totalMatches: Flow<Int> = matchHistoryDao.getTotalMatchesCount()
    val player1Wins: Flow<Int> = matchHistoryDao.getPlayer1WinsCount()

    suspend fun recordMatch(match: MatchHistoryEntity): Long {
        return matchHistoryDao.insertMatch(match)
    }

    suspend fun clearHistory() {
        matchHistoryDao.clearAllHistory()
    }
}
