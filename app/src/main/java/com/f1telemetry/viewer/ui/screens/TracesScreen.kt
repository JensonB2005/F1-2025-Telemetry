package com.f1telemetry.viewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1telemetry.viewer.data.LapTrace
import com.f1telemetry.viewer.data.TelemetryState
import com.f1telemetry.viewer.ui.Fmt
import com.f1telemetry.viewer.ui.components.ComparisonChart
import com.f1telemetry.viewer.ui.components.MultiLineChart
import com.f1telemetry.viewer.ui.components.SectionCard
import com.f1telemetry.viewer.ui.components.Series
import com.f1telemetry.viewer.ui.theme.AccentCyan
import com.f1telemetry.viewer.ui.theme.AccentGreen
import com.f1telemetry.viewer.ui.theme.AccentPurple
import com.f1telemetry.viewer.ui.theme.AccentYellow
import com.f1telemetry.viewer.ui.theme.F1Red
import com.f1telemetry.viewer.ui.theme.TextDim

@Composable
fun TracesScreen(state: TelemetryState) {
    val trace = state.currentTrace
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (trace == null || trace.samples.size < 2) {
            SectionCard("Traces") {
                Text(
                    "Waiting for lap data… Start driving (or replay a session) and the speed, " +
                        "throttle, brake and g-force traces will build here, comparing your current " +
                        "lap against your best.",
                    color = TextDim, fontSize = 13.sp,
                )
            }
            return
        }
        val best = state.bestTrace

        SectionCard("Speed — current vs best (lap ${trace.lapNumber})") {
            ComparisonChart(
                label = "Current",
                current = trace.samples.map { it.distance to it.speed.toFloat() },
                reference = best?.samples?.map { it.distance to it.speed.toFloat() },
                yMin = 0f, yMax = 360f,
                currentColor = AccentCyan, referenceColor = AccentPurple,
                heightDp = 160,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Best lap: ${Fmt.lapTime(best?.lapTimeMs ?: 0)}  •  current lap time ${Fmt.lapTime(trace.lapTimeMs)}",
                color = TextDim, fontSize = 11.sp,
            )
        }

        SectionCard("Inputs — throttle & brake") {
            MultiLineChart(
                listOf(
                    Series("Throttle", AccentGreen, trace.samples.map { it.distance to it.throttle }, 0f, 1f),
                    Series("Brake", F1Red, trace.samples.map { it.distance to it.brake }, 0f, 1f),
                    Series("Steer", AccentYellow, trace.samples.map { it.distance to (it.steer + 1f) / 2f }, 0f, 1f),
                ),
                heightDp = 150,
            )
        }

        SectionCard("Gear") {
            MultiLineChart(
                listOf(Series("Gear", AccentCyan, trace.samples.map { it.distance to it.gear.toFloat() }, 0f, 8f)),
                heightDp = 110,
            )
        }

        SectionCard("G-force") {
            MultiLineChart(
                listOf(
                    Series("Lateral", AccentYellow, trace.samples.map { it.distance to it.gLat }, -6f, 6f),
                    Series("Longitudinal", AccentCyan, trace.samples.map { it.distance to it.gLon }, -6f, 6f),
                ),
                heightDp = 130,
            )
        }

        SectionCard("RPM") {
            MultiLineChart(
                listOf(Series("RPM", F1Red, trace.samples.map { it.distance to it.rpm.toFloat() }, 0f, 15000f)),
                heightDp = 110,
            )
        }
    }
}
