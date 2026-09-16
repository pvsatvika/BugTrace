package com.bugtrace.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugtrace.app.model.StatusLevel
import com.bugtrace.app.model.TelemetryItem
import com.bugtrace.app.ui.theme.*

@Composable
fun TelemetryCard(
    item: TelemetryItem,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (item.key) {
        "battery" -> if (item.statusLevel == StatusLevel.ALERT) Icons.Default.BatteryAlert else Icons.Default.BatteryFull
        "orientation" -> Icons.Default.ScreenRotation
        "network" -> when (item.value.lowercase()) {
            "wi-fi" -> Icons.Default.Wifi
            "mobile data" -> Icons.Default.SignalCellularAlt
            "offline" -> Icons.Default.WifiOff
            else -> Icons.Default.Wifi
        }
        else -> Icons.Default.DeveloperBoard
    }

    val accentColor = when (item.statusLevel) {
        StatusLevel.WARNING -> StatusAmber
        StatusLevel.ALERT -> StatusRed
        StatusLevel.NORMAL -> StatusBlue
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE SENSOR",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = StatusGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.value,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = item.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = accentColor
                )
            }
        }
    }
}
