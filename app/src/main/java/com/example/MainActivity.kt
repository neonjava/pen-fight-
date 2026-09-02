package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.PenFightDatabase
import com.example.data.PenFightRepository
import com.example.model.ScreenState
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.GameViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = PenFightDatabase.getInstance(this)
        val repository = PenFightRepository(database.matchHistoryDao())
        val factory = GameViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory)[GameViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(targetState = viewModel.currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            ScreenState.MAIN_MENU -> MainMenuScreen(viewModel = viewModel)
                            ScreenState.GAME_ARENA -> GameArenaScreen(viewModel = viewModel)
                            ScreenState.PEN_GARAGE -> PenGarageScreen(viewModel = viewModel)
                            ScreenState.ARENA_SELECT -> ArenaSelectScreen(viewModel = viewModel)
                            ScreenState.MATCH_STATS -> MatchStatsScreen(viewModel = viewModel)
                            ScreenState.SETTINGS -> ArenaSelectScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
