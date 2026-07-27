package com.mulerun.humandetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mulerun.humandetector.model.SensorAvailability
import com.mulerun.humandetector.model.SensorKind
import com.mulerun.humandetector.ui.theme.*
import com.mulerun.humandetector.viewmodel.RadarViewModel

@Composable
fun SettingsScreen(vm: RadarViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        // top bar
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.Filled.ArrowBack, null, tint = RadarGreen) }
            Text("SETTINGS", color = RadarGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Section("SENSORS") {
            SensorKind.entries.forEach { k ->
                val status = state.statuses[k]
                val available = status?.availability == SensorAvailability.AVAILABLE
                val checked = state.enabled[k] == true
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp)).background(Panel).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(k.label, color = if (available) RadarGreen else Muted,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                        Text(status?.note ?: "unavailable", color = Muted, fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace)
                    }
                    Switch(
                        checked = checked && available,
                        enabled = available,
                        onCheckedChange = { vm.setEnabled(k, it) }
                    )
                }
            }
        }
        Section("BEHAVIOR") {
            SwitchRow("Voice announcements", state.voice) { vm.setVoice(it) }
            SwitchRow("Sound effects",       state.sound) { vm.setSound(it) }
            SwitchRow("Vibration",           state.vibration) { vm.setVibration(it) }
            SwitchRow("Night mode",          state.nightMode) { vm.setNightMode(it) }
            SwitchRow("Battery saver",       state.batterySaver) { vm.setBatterySaver(it) }
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(10.dp)).background(Panel).padding(12.dp)) {
                Text("Detection sensitivity: ${(state.sensitivity * 100).toInt()}%",
                    color = RadarGreen, fontFamily = FontFamily.Monospace)
                Slider(
                    value = state.sensitivity,
                    onValueChange = { vm.setSensitivity(it) },
                    colors = SliderDefaults.colors(thumbColor = RadarGreen, activeTrackColor = RadarGreen)
                )
                Text("Higher sensitivity accepts weaker signals — may increase false positives.",
                    color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(title, color = Amber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 4.dp))
        content()
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp)).background(Panel).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = androidx.compose.ui.graphics.Color(0xFFE6F0EA),
            fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
