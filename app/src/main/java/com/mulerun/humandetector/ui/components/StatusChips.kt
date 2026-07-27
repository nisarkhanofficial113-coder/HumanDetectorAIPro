package com.mulerun.humandetector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mulerun.humandetector.model.SensorAvailability
import com.mulerun.humandetector.model.SensorStatus
import com.mulerun.humandetector.ui.theme.Line
import com.mulerun.humandetector.ui.theme.Muted
import com.mulerun.humandetector.ui.theme.Panel
import com.mulerun.humandetector.ui.theme.RadarGreen
import com.mulerun.humandetector.ui.theme.Warn

@Composable
fun SensorStatusStrip(statuses: List<SensorStatus>, modifier: Modifier = Modifier) {
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(statuses) { s ->
            StatusChip(label = s.kind.label, status = s.availability, note = s.note)
        }
    }
}

@Composable
private fun StatusChip(label: String, status: SensorAvailability, note: String) {
    val (dot, ring) = when (status) {
        SensorAvailability.AVAILABLE -> RadarGreen to RadarGreen.copy(alpha = 0.35f)
        SensorAvailability.PERMISSION_REQUIRED -> Color(0xFFFFB454) to Color(0xFFFFB454).copy(alpha = 0.35f)
        SensorAvailability.DISABLED_BY_USER -> Muted to Line
        SensorAvailability.MISSING_HW -> Warn.copy(alpha = 0.55f) to Warn.copy(alpha = 0.25f)
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Panel)
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Canvas(Modifier.padding(end = 8.dp)) {
            drawCircle(ring, 8f)
            drawCircle(dot, 4f)
        }
        Column {
            Text(label, color = Color(0xFFE6F0EA), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
            Text(
                when (status) {
                    SensorAvailability.AVAILABLE -> "LIVE"
                    SensorAvailability.PERMISSION_REQUIRED -> "PERM"
                    SensorAvailability.DISABLED_BY_USER -> "OFF"
                    SensorAvailability.MISSING_HW -> "N/A"
                },
                color = when (status) {
                    SensorAvailability.AVAILABLE -> RadarGreen
                    SensorAvailability.PERMISSION_REQUIRED -> Color(0xFFFFB454)
                    SensorAvailability.DISABLED_BY_USER -> Muted
                    SensorAvailability.MISSING_HW -> Warn
                },
                fontSize = 9.sp, fontFamily = FontFamily.Monospace
            )
        }
    }
}
