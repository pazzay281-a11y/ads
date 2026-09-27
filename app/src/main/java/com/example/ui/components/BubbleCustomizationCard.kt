package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BubbleClickAction
import com.example.data.BubbleColor
import com.example.ui.StringsDict
import kotlin.math.roundToInt

@Composable
fun BubbleCustomizationCard(
    bubbleSizeDp: Int,
    bubbleAlpha: Float,
    clickAction: BubbleClickAction,
    snapEdges: Boolean,
    vibrateEnabled: Boolean,
    bubbleColor: BubbleColor,
    language: String,
    onSizeChange: (Int) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onClickActionChange: (BubbleClickAction) -> Unit,
    onSnapEdgesChange: (Boolean) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onBubbleColorChange: (BubbleColor) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF8E1)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFFF57F17),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = StringsDict.customizationTitle(language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bubble Tap Mode (Direct 1-tap reset vs Quick menu)
            Text(
                text = StringsDict.clickMode(language),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = clickAction == BubbleClickAction.DIRECT_RESET,
                    onClick = { onClickActionChange(BubbleClickAction.DIRECT_RESET) },
                    label = { Text(StringsDict.clickModeDirect(language), fontSize = 12.sp) },
                    modifier = Modifier.weight(1f).testTag("mode_direct_chip"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = clickAction == BubbleClickAction.SHOW_MENU,
                    onClick = { onClickActionChange(BubbleClickAction.SHOW_MENU) },
                    label = { Text(StringsDict.clickModeMenu(language), fontSize = 12.sp) },
                    modifier = Modifier.weight(1f).testTag("mode_menu_chip"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bubble Size Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringsDict.bubbleSize(language),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${bubbleSizeDp}dp",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = bubbleSizeDp.toFloat(),
                onValueChange = { onSizeChange(it.roundToInt()) },
                valueRange = 44f..80f,
                steps = 5,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("bubble_size_slider")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bubble Opacity Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringsDict.bubbleAlpha(language),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(bubbleAlpha * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = bubbleAlpha,
                onValueChange = { onAlphaChange(it) },
                valueRange = 0.3f..1.0f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("bubble_alpha_slider")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bubble Theme Color
            Text(
                text = StringsDict.bubbleColor(language),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BubbleColor.entries.forEach { colorOption ->
                    val isSelected = bubbleColor == colorOption
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(colorOption.hex))
                            .clickable { onBubbleColorChange(colorOption) }
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .testTag("bubble_color_${colorOption.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Snap to Edges Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringsDict.snapEdges(language),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = snapEdges,
                    onCheckedChange = onSnapEdgesChange,
                    modifier = Modifier.testTag("snap_edges_switch")
                )
            }

            // Vibrate on tap Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringsDict.vibrateOnTap(language),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = vibrateEnabled,
                    onCheckedChange = onVibrateChange,
                    modifier = Modifier.testTag("vibrate_switch")
                )
            }
        }
    }
}
