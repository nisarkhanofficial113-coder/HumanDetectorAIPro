package com.mulerun.humandetector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mulerun.humandetector.ui.theme.*

@Composable
fun HudBar(
    count: Int,
    signalPct: Int,
    batteryPct: Int,
    scanning: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HudTile("TARGETS", "$count", RadarGreen, Icons.Filled.PersonPin, Modifier.weight(1f))
        HudTile("SIGNAL", "${signalPct}%", if (signalPct > 60) RadarGreen else Amber, Icons.Filled.NetworkCheck, Modifier.weight(1f))
        HudTile(if (scanning) "SCAN" else "IDLE", if (scanning) "LIVE" else "OFF",
            if (scanning) RadarGreen else Muted, Icons.Filled.RadioButtonChecked, Modifier.weight(1f))
        HudTile("BATT", if (batteryPct >= 0) "${batteryPct}%" else "—",
            if (batteryPct > 20) RadarGreen else Warn, Icons.Filled.BatteryFull, Modifier.weight(1f))
    }
}

@Composable
private fun HudTile(label: String, value: String, tint: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Panel)
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Text(label, color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Text(value, color = tint, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
