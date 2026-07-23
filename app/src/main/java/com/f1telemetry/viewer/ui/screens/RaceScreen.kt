package com.f1telemetry.viewer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1telemetry.viewer.data.LiveCar
import com.f1telemetry.viewer.data.TelemetryState
import com.f1telemetry.viewer.telemetry.F1Constants
import com.f1telemetry.viewer.ui.Fmt
import com.f1telemetry.viewer.ui.components.LiveTrackMap
import com.f1telemetry.viewer.ui.components.SectionCard
import com.f1telemetry.viewer.ui.theme.AccentCyan
import com.f1telemetry.viewer.ui.theme.AccentGreen
import com.f1telemetry.viewer.ui.theme.AccentPurple
import com.f1telemetry.viewer.ui.theme.AccentYellow
import com.f1telemetry.viewer.ui.theme.F1Card
import com.f1telemetry.viewer.ui.theme.F1Red
import com.f1telemetry.viewer.ui.theme.TextDim

private val W_POS = 26.dp
private val W_DRIVER = 66.dp
private val W_TYRE = 30.dp
private val W_ERS = 40.dp
private val W_DRS = 34.dp
private val W_INT = 60.dp
private val W_LAST = 66.dp
private val W_FAST = 66.dp

@Composable
fun RaceScreen(state: TelemetryState) {
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxWidth().verticalScroll(scroll).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RaceHeader(state)

        SectionCard("Live track map") {
            if (state.trackPath.size < 4) {
                Text(
                    "Building the track map… complete a lap (or replay one) and the circuit outline " +
                        "with every car will appear here.",
                    color = TextDim, fontSize = 13.sp,
                )
            } else {
                LiveTrackMap(path = state.trackPath, cars = state.cars)
                Text(
                    "${state.cars.size} cars • outline traced from live world position",
                    color = TextDim, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        TimingTower(state)

        WeatherRow(state)
    }
}

@Composable
private fun RaceHeader(state: TelemetryState) {
    val s = state.session
    val lap = state.lap
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(F1Card).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(F1Constants.sessionType(s.sessionType).uppercase(), color = F1Red, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text(F1Constants.track(s.trackId), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.End) {
            val total = if (s.totalLaps > 0) "/${s.totalLaps}" else ""
            Text("LAP ${lap.currentLapNum}$total", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("${F1Constants.weather(s.weather)} • ${s.trackTempC}°/${s.airTempC}°C", color = TextDim, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TimingTower(state: TelemetryState) {
    SectionCard("Timing tower") {
        if (state.cars.isEmpty()) {
            Text(
                "Waiting for the field… the full running order with tyres, ERS, DRS, intervals and lap " +
                    "times streams here once the session is live.",
                color = TextDim, fontSize = 13.sp,
            )
            return@SectionCard
        }
        val hScroll = rememberScrollState()
        Column(Modifier.horizontalScroll(hScroll)) {
            TowerHeader()
            state.cars.forEach { car ->
                TowerRow(car, isFastest = car.index == state.fastestLapCarIndex)
            }
        }
    }
}

@Composable
private fun TowerHeader() {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HCell("", W_POS)
        HCell("DRIVER", W_DRIVER)
        HCell("TYRE", W_TYRE)
        HCell("ERS", W_ERS)
        HCell("DRS", W_DRS)
        HCell("INT", W_INT)
        HCell("LAST", W_LAST)
        HCell("FASTEST", W_FAST)
    }
}

@Composable
private fun HCell(text: String, w: androidx.compose.ui.unit.Dp) {
    Text(text, color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(w), textAlign = TextAlign.Center)
}

@Composable
private fun TowerRow(car: LiveCar, isFastest: Boolean) {
    val bg = if (car.isPlayer) Color(0xFF33333F) else Color.Transparent
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(bg).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Position
        Text("${car.position}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, modifier = Modifier.width(W_POS), textAlign = TextAlign.Center)

        // Team colour bar + abbrev
        Row(Modifier.width(W_DRIVER), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(Color(F1Constants.teamColor(car.teamId))))
            Spacer(Modifier.width(4.dp))
            Text(car.abbrev, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Clip)
        }

        // Tyre + age
        Row(Modifier.width(W_TYRE), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(F1Constants.tyreLetter(car.visualTyre), color = Color(F1Constants.tyreColor(car.visualTyre)), fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Text("${car.tyreAge}", color = TextDim, fontSize = 9.sp, modifier = Modifier.padding(start = 2.dp))
        }

        // ERS bar
        Box(Modifier.width(W_ERS).padding(horizontal = 3.dp)) {
            Box(Modifier.fillMaxWidth().height(11.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF33333F)))
            Box(Modifier.fillMaxWidth(car.ersPct / 100f).height(11.dp).clip(RoundedCornerShape(3.dp)).background(ersColor(car.ersPct)))
            Text("${car.ersPct}", color = Color.White, fontSize = 8.sp, modifier = Modifier.align(Alignment.Center))
        }

        // DRS
        Box(Modifier.width(W_DRS), contentAlignment = Alignment.Center) {
            val (c, t) = when {
                car.drsOpen -> AccentGreen to Color.Black
                car.drsAllowed -> Color(0xFF2E7DD1) to Color.White
                else -> Color(0xFF33333F) to TextDim
            }
            Box(Modifier.clip(RoundedCornerShape(3.dp)).background(c).padding(horizontal = 4.dp, vertical = 1.dp)) {
                Text("DRS", color = t, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Interval (to car ahead)
        Text(
            if (car.position <= 1) "LEADER" else if (car.pitting) "PIT" else "+${Fmt.sector(car.deltaAheadMs)}",
            color = if (car.pitting) AccentYellow else TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(W_INT), textAlign = TextAlign.Center, maxLines = 1,
        )

        // Last lap
        Text(shortLap(car.lastLapMs), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(W_LAST), textAlign = TextAlign.Center, maxLines = 1)

        // Fastest lap
        Text(shortLap(car.bestLapMs), color = if (isFastest) AccentPurple else AccentCyan, fontSize = 11.sp,
            fontFamily = FontFamily.Monospace, modifier = Modifier.width(W_FAST), textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun WeatherRow(state: TelemetryState) {
    val s = state.session
    SectionCard("Track information") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Info("Weather", F1Constants.weather(s.weather))
            Info("Track", "${s.trackTempC}°C")
            Info("Air", "${s.airTempC}°C")
            Info("Pit limit", "${s.pitSpeedLimit}")
        }
    }
}

@Composable
private fun Info(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), color = TextDim, fontSize = 9.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

private fun ersColor(pct: Int): Color = when {
    pct > 60 -> AccentGreen
    pct > 25 -> AccentYellow
    else -> F1Red
}

private fun shortLap(ms: Long): String {
    if (ms <= 0) return "—"
    val m = ms / 60000
    val s = (ms % 60000) / 1000
    val mmm = ms % 1000
    return "%d:%02d.%03d".format(m, s, mmm)
}
