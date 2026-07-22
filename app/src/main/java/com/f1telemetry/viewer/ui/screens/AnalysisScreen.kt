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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1telemetry.viewer.data.Insight
import com.f1telemetry.viewer.data.InsightLevel
import com.f1telemetry.viewer.data.TelemetryState
import com.f1telemetry.viewer.telemetry.F1Constants
import com.f1telemetry.viewer.ui.components.LabeledValue
import com.f1telemetry.viewer.ui.components.SectionCard
import com.f1telemetry.viewer.ui.theme.AccentCyan
import com.f1telemetry.viewer.ui.theme.AccentGreen
import com.f1telemetry.viewer.ui.theme.AccentYellow
import com.f1telemetry.viewer.ui.theme.F1Red
import com.f1telemetry.viewer.ui.theme.TextDim

@Composable
fun AnalysisScreen(state: TelemetryState) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val mistakes = state.insights.filter { it.level == InsightLevel.MISTAKE || it.level == InsightLevel.WARNING }
        val tips = state.insights.filter { it.level == InsightLevel.TIP || it.level == InsightLevel.INFO }

        SectionCard("Mistakes & warnings") {
            if (mistakes.isEmpty()) {
                Text("No mistakes detected yet. Drive a lap and lock-ups, throttle corrections, track-limit and time-loss findings will appear here.", color = TextDim, fontSize = 13.sp)
            } else {
                mistakes.forEach { InsightRow(it) }
            }
        }

        SectionCard("Setup & driving suggestions") {
            if (tips.isEmpty()) {
                Text("Setup advice appears once tyre/brake temps and handling data are flowing.", color = TextDim, fontSize = 13.sp)
            } else {
                tips.forEach { InsightRow(it) }
            }
        }

        SectionCard("Current setup") {
            val s = state.setup
            LabeledValue("Front / rear wing", "${s.frontWing} / ${s.rearWing}")
            LabeledValue("Diff on / off throttle", "${s.onThrottleDiff}% / ${s.offThrottleDiff}%")
            LabeledValue("Front / rear camber", "%.2f° / %.2f°".format(s.frontCamber, s.rearCamber))
            LabeledValue("Front / rear toe", "%.2f° / %.2f°".format(s.frontToe, s.rearToe))
            LabeledValue("Susp. front / rear", "${s.frontSuspension} / ${s.rearSuspension}")
            LabeledValue("Anti-roll bar F / R", "${s.frontAntiRollBar} / ${s.rearAntiRollBar}")
            LabeledValue("Ride height F / R", "${s.frontRideHeight} / ${s.rearRideHeight}")
            LabeledValue("Brake pressure / bias", "${s.brakePressure}% / ${s.brakeBias}%")
            LabeledValue("Tyre psi FL/FR", "%.1f / %.1f".format(s.frontLeftPressure, s.frontRightPressure))
            LabeledValue("Tyre psi RL/RR", "%.1f / %.1f".format(s.rearLeftPressure, s.rearRightPressure))
        }

        SectionCard("Event feed") {
            if (state.events.isEmpty()) {
                Text("Session events (fastest laps, penalties, DRS, safety car, overtakes…) will stream here.", color = TextDim, fontSize = 13.sp)
            } else {
                state.events.forEach { e ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(F1Constants.eventName(e.code), color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(140.dp))
                        Text(e.detail, color = TextDim, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightRow(insight: Insight) {
    val color = when (insight.level) {
        InsightLevel.MISTAKE -> F1Red
        InsightLevel.WARNING -> AccentYellow
        InsightLevel.TIP -> AccentGreen
        InsightLevel.INFO -> AccentCyan
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Box(Modifier.width(4.dp).height(38.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.fillMaxWidth()) {
            Row {
                Text(insight.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                insight.atDistance?.let {
                    Text("  @ ${it.toInt()} m", color = TextDim, fontSize = 11.sp)
                }
                insight.lap?.let {
                    Text("  (lap $it)", color = TextDim, fontSize = 11.sp)
                }
            }
            Text(insight.detail, color = TextDim, fontSize = 12.sp)
        }
    }
}
