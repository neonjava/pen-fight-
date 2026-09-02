package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HowToPlayDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🖊️", fontSize = 24.sp)
                Text(
                    text = "HOW TO PLAY PEN FIGHT",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RuleItem(
                    step = "1",
                    title = "The Objective",
                    desc = "Knock your opponent's pen completely off the desk edges while keeping your own pen safely on the table surface!"
                )

                RuleItem(
                    step = "2",
                    title = "Slingshot Flick Controls",
                    desc = "On your turn, touch near your pen and drag backward like a slingshot. The dotted arrow shows your aim direction and power meter. Release to FLICK!"
                )

                RuleItem(
                    step = "3",
                    title = "Cap vs Nib Spin Physics",
                    desc = "Hitting near the cap or nib applies heavy rotational torque, making your pen pirouette into the opponent! Striking dead center delivers maximum forward knockback."
                )

                RuleItem(
                    step = "4",
                    title = "Desk Hazards & Bank Shots",
                    desc = "Use classroom obstacles like erasers and rulers to execute ricochet trick shots around obstacles."
                )

                RuleItem(
                    step = "5",
                    title = "Winning the Duel",
                    desc = "First player to reach the target round wins (Best of 1, 3, 5, or 7) claims the Classroom Champion title!"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("rules_ok_button")
            ) {
                Text("Got It, Let's Fight!")
            }
        }
    )
}

@Composable
private fun RuleItem(
    step: String,
    title: String,
    desc: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = step,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
