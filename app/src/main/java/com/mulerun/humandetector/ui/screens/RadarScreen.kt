package com.mulerun.humandetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mulerun.humandetector.ui.components.HudBar
import com.mulerun.humandetector.ui.components.Radar
import com.mulerun.humandetector.ui.components.SensorStatusStrip
import com.mulerun.humandetector.ui.theme.*
import com.mulerun.humandetector.viewmodel.RadarViewModel

@Composable
fun RadarScreen(vm: RadarViewModel, onOpenSettings: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())
    ) {
        TopBar(onOpenSettings)
        HudBar(
            count = state.targets.size,
            signalPct = state.signalPct,
            batteryPct = state.batteryPct,
            scanning = state.scanning
        )
        SensorStatusStrip(state.statuses.values.toList(), Modifier.padding(vertical = 6.dp))
        Radar(state.targets)
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { if (state.scanning) vm.stopScanning() else vm.startScanning() },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (state.scanning) Warn else RadarGreen)
            ) {
                Icon(if (state.scanning) Icons.Filled.Stop else Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.scanning) "STOP SCAN" else "START SCAN",
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Settings, null, tint = RadarGreen)
            }
        }
        // Target list
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            if (state.targets.isEmpty()) {
                Text("NO TARGETS", color = Muted, fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
            } else {
                state.targets.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Panel)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("TGT #${t.id}", color = Warn, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("via ${t.kinds.joinToString("+") { it.label }}", color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${"%.1f".format(t.distanceM)} m", color = RadarGreen, fontFamily = FontFamily.Monospace)
                            Text("${(t.confidence * 100).toInt()}% conf.", color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TopBar(onOpenSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "HUMAN DETECTOR ",
            color = RadarGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp
        )
        Text(
            "AI • PRO",
            color = Amber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp
        )
        Spacer(Modifier.weight(1f))
        Text("v1.0.0", color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
