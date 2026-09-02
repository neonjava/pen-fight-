package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PenCustomization
import com.example.model.PenInstance
import com.example.model.PenStyle
import com.example.model.ScreenState
import com.example.ui.components.PenRenderer
import com.example.ui.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PenGarageScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    var selectedPlayerTab by remember { mutableIntStateOf(1) } // 1 or 2

    val activeCustomization = if (selectedPlayerTab == 1) {
        viewModel.player1Customization
    } else {
        viewModel.player2Customization
    }

    val updateCustomization: (PenCustomization) -> Unit = { updated ->
        if (selectedPlayerTab == 1) {
            viewModel.player1Customization = updated
        } else {
            viewModel.player2Customization = updated
        }
    }

    val colorPalette = listOf(
        0xFF1976D2, 0xFF0D47A1, 0xFFE11D48, 0xFF9F1239,
        0xFF16A34A, 0xFF84CC16, 0xFFF59E0B, 0xFFD97706,
        0xFF8B5CF6, 0xFF3730A3, 0xFF0F172A, 0xFF334155,
        0xFFD4AF37, 0xFFFFD700, 0xFFEC4899, 0xFF06B6D4
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PEN GARAGE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenState.MAIN_MENU) },
                        modifier = Modifier.testTag("garage_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Menu"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Player 1 / Player 2 Selector Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedPlayerTab - 1,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Tab(
                        selected = selectedPlayerTab == 1,
                        onClick = { selectedPlayerTab = 1 },
                        text = {
                            Text(
                                text = "PLAYER 1 PEN",
                                fontWeight = if (selectedPlayerTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedPlayerTab == 1) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.testTag("tab_player_1")
                    )
                    Tab(
                        selected = selectedPlayerTab == 2,
                        onClick = { selectedPlayerTab = 2 },
                        text = {
                            Text(
                                text = "PLAYER 2 PEN",
                                fontWeight = if (selectedPlayerTab == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedPlayerTab == 2) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.testTag("tab_player_2")
                    )
                }
            }

            // Live Pen Stage / Preview Showcase
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val dummyPen = PenInstance(
                                playerId = selectedPlayerTab,
                                x = size.width / 2f,
                                y = size.height / 2f,
                                angle = 0f,
                                config = activeCustomization
                            )
                            PenRenderer.drawPen(
                                scope = this,
                                pen = dummyPen,
                                isCurrentTurn = true
                            )
                        }
                    }
                }
            }

            // Pen Model Selector Chips
            item {
                Text(
                    text = "SELECT PEN MODEL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (style in PenStyle.entries) {
                        val isSelected = activeCustomization.style == style
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    updateCustomization(
                                        activeCustomization.copy(
                                            style = style,
                                            bodyColor = style.defaultBodyColor,
                                            capColor = style.defaultCapColor,
                                            gripColor = style.defaultGripColor,
                                            nibColor = style.defaultNibColor,
                                            customName = style.displayName
                                        )
                                    )
                                }
                                .testTag("pen_model_${style.name}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(style.defaultBodyColor))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = style.displayName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = style.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Performance Attributes / Stat Bars
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PEN ATTRIBUTES",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        val style = activeCustomization.style
                        StatBar(label = "Mass / Knockback Weight", valueRatio = (style.mass / 1.8f).coerceIn(0.1f, 1f), color = Color(0xFFEF4444))
                        StatBar(label = "Flick Speed & Glide", valueRatio = (style.speedMultiplier / 1.4f).coerceIn(0.1f, 1f), color = Color(0xFF3B82F6))
                        StatBar(label = "Rotational Spin Torque", valueRatio = (style.spinMultiplier / 1.7f).coerceIn(0.1f, 1f), color = Color(0xFF8B5CF6))
                        StatBar(label = "Desk Grip / Stability", valueRatio = (style.frictionMultiplier / 1.3f).coerceIn(0.1f, 1f), color = Color(0xFF10B981))
                    }
                }
            }

            // Custom Barrel Color Picker
            item {
                Text(
                    text = "CUSTOM BARREL COLOR",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(colorPalette) { colorLong ->
                        val isColorSelected = activeCustomization.bodyColor == colorLong
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(colorLong))
                                .clickable {
                                    updateCustomization(activeCustomization.copy(bodyColor = colorLong))
                                }
                                .border(
                                    width = if (isColorSelected) 3.dp else 1.dp,
                                    color = if (isColorSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isColorSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBar(
    label: String,
    valueRatio: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(valueRatio * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { valueRatio },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}
