package com.f1telemetry.viewer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.f1telemetry.viewer.data.TelemetryState
import com.f1telemetry.viewer.telemetry.F1Constants
import com.f1telemetry.viewer.ui.Fmt
import com.f1telemetry.viewer.ui.components.BarMeter
import com.f1telemetry.viewer.ui.components.LabeledValue
import com.f1telemetry.viewer.ui.components.SectionCard
import com.f1telemetry.viewer.ui.components.StatTile
import com.f1telemetry.viewer.ui.theme.AccentCyan
import com.f1telemetry.viewer.ui.theme.AccentGreen
import com.f1telemetry.viewer.ui.theme.AccentPurple
import com.f1telemetry.viewer.ui.theme.AccentYellow
import com.f1telemetry.viewer.ui.theme.F1Red
import com.f1telemetry.viewer.ui.theme.TextDim

@Composable
fun DashboardScreen(state: TelemetryState) {
    val t = state.telemetry
    val lap = state.lap
    val status = state.status
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Gear / speed / rpm hero
        SectionCard("Live") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        gearLabel(t.gear),
                        color = Color.White,
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text("GEAR", color = TextDim, fontSize = 11.sp)
                }
                Column(Modifier.weight(1.4f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${t.speedKmh}",
                        color = AccentCyan,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text("KM/H", color = TextDim, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    RevBar(t.revLightsPercent, t.drs)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${t.engineRpm}",
                        color = if (t.revLightsPercent > 90) F1Red else Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text("RPM", color = TextDim, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            PedalRow("Throttle", t.throttle, AccentGreen)
            Spacer(Modifier.height(6.dp))
            PedalRow("Brake", t.brake, F1Red)
        }

        // Timing
        SectionCard("Timing") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Position", if (lap.carPosition > 0) "P${lap.carPosition}" else "--", accent = AccentYellow, modifier = Modifier.weight(1f))
                StatTile("Lap", "${lap.currentLapNum}${if (state.session.totalLaps > 0) "/${state.session.totalLaps}" else ""}", modifier = Modifier.weight(1f))
                StatTile("Sector", "${F1Constants.sector(lap.sector)}", accent = AccentCyan, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            LabeledValue("Current lap", Fmt.lapTime(lap.currentLapTimeMs), AccentCyan)
            LabeledValue("Last lap", Fmt.lapTime(lap.lastLapTimeMs))
            LabeledValue("Best lap", Fmt.lapTime(state.bestLapTimeMs), AccentPurple)
            LabeledValue("Sector 1 / 2", "${Fmt.sector(lap.sector1Ms)} / ${Fmt.sector(lap.sector2Ms)}")
            LabeledValue("Delta to leader", Fmt.delta(lap.deltaToLeaderMs))
            LabeledValue("Delta to car ahead", Fmt.delta(lap.deltaToCarAheadMs))
        }

        // Car status quick view
        SectionCard("Car") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Tyre", F1Constants.visualTyre(status.visualTyreCompound), accent = AccentYellow, modifier = Modifier.weight(1f))
                StatTile("Tyre age", "${status.tyresAgeLaps}", unit = "lap", modifier = Modifier.weight(1f))
                StatTile("ERS", "${(status.ersStoreEnergy / 40000f).coerceIn(0f, 100f).toInt()}", unit = "%", accent = AccentGreen, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            LabeledValue("Fuel", "%.1f kg (%.2f laps)".format(status.fuelInTank, status.fuelRemainingLaps))
            LabeledValue("Fuel mix", F1Constants.fuelMix(status.fuelMix))
            LabeledValue("ERS mode", F1Constants.ersMode(status.ersDeployMode))
            LabeledValue("Front brake bias", "${status.frontBrakeBias}%")
            LabeledValue("DRS", if (t.drs) "OPEN" else F1Constants.drsFault(status.drsAllowed), if (t.drs) AccentGreen else TextDim)
        }

        // Session / conditions
        SectionCard("Session") {
            LabeledValue("Track", F1Constants.track(state.session.trackId))
            LabeledValue("Type", F1Constants.sessionType(state.session.sessionType))
            LabeledValue("Weather", F1Constants.weather(state.session.weather))
            LabeledValue("Track / Air temp", "${state.session.trackTempC}° / ${state.session.airTempC}°C")
            LabeledValue("Warnings", "${lap.totalWarnings} (track limits ${lap.cornerCuttingWarnings})")
            LabeledValue("Penalties", "${lap.penaltiesSec}s")
        }
    }
}

@Composable
private fun PedalRow(label: String, value: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextDim, fontSize = 12.sp, modifier = Modifier.width(70.dp))
        Box(Modifier.weight(1f)) { BarMeter(value, color) }
        Text("${(value * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun RevBar(pct: Int, drs: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        val segments = 12
        for (i in 0 until segments) {
            val on = pct >= (i + 1) * (100 / segments)
            val col = when {
                !on -> Color(0xFF33333F)
                i < 5 -> AccentGreen
                i < 9 -> AccentYellow
                else -> F1Red
            }
            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(2.dp)).background(col))
        }
    }
}

private fun gearLabel(gear: Int): String = when (gear) {
    -1 -> "R"
    0 -> "N"
    else -> "$gear"
}
