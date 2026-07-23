package com.f1telemetry.viewer.ui.screens

import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import com.f1telemetry.viewer.ui.components.brakeTempColor
import com.f1telemetry.viewer.ui.components.tempColor
import com.f1telemetry.viewer.ui.components.wearColor
import com.f1telemetry.viewer.ui.findActivity
import com.f1telemetry.viewer.ui.theme.AccentCyan
import com.f1telemetry.viewer.ui.theme.AccentGreen
import com.f1telemetry.viewer.ui.theme.AccentPurple
import com.f1telemetry.viewer.ui.theme.AccentYellow
import com.f1telemetry.viewer.ui.theme.F1Red
import com.f1telemetry.viewer.ui.theme.TextDim
import kotlin.math.min

private val PANEL = Color(0xFF12121A)
private val CELL = Color(0xFF1C1C28)
private val TITLE = Color(0xFF25C5F0)

/**
 * Full-screen, single-page broadcast-style race dashboard modelled on the F1 TV
 * world-feed layout: track map + track/weather info (left), car information with
 * inputs, gear, ERS/brake/throttle bars, steering, fuel and per-tyre temps
 * (centre), and the full timing tower with a focused-driver banner (right).
 * Auto-scales to the device and locks to landscape; nothing scrolls.
 */
@Composable
fun BroadcastScreen(state: TelemetryState, onExit: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { activity?.requestedOrientation = previous ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF07070B))) {
        val s = min(maxWidth.value / 1024f, maxHeight.value / 560f).coerceIn(0.45f, 3f)
        val gap = (5 * s).dp
        Column(Modifier.fillMaxSize().padding((5 * s).dp), verticalArrangement = Arrangement.spacedBy(gap)) {
            TopBar(state, s)
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                LeftColumn(state, s, Modifier.weight(1f).fillMaxHeight())
                CenterColumn(state, s, Modifier.weight(0.92f).fillMaxHeight())
                RightColumn(state, s, Modifier.weight(1.18f).fillMaxHeight())
            }
        }
        Text(
            "✕", color = TextDim, fontSize = (15 * s).sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding((2 * s).dp)
                .clip(RoundedCornerShape(6.dp)).background(Color(0x66000000))
                .clickable { onExit() }.padding(horizontal = (8 * s).dp, vertical = (2 * s).dp),
        )
    }
}

@Composable
private fun TopBar(state: TelemetryState, s: Float) {
    val lap = state.lap
    val total = if (state.session.totalLaps > 0) "/${state.session.totalLaps}" else ""
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(PANEL).padding(horizontal = (10 * s).dp, vertical = (5 * s).dp), verticalAlignment = Alignment.CenterVertically) {
        Text("F1", color = Color.White, fontSize = (22 * s).sp, fontWeight = FontWeight.Black, modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(F1Red).padding(horizontal = (5 * s).dp))
        Spacer(Modifier.width((8 * s).dp))
        Text("RACE", color = Color.White, fontSize = (20 * s).sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width((10 * s).dp))
        Text("LAP ${lap.currentLapNum}$total", color = TextDim, fontSize = (14 * s).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(F1Constants.track(state.session.trackId).uppercase(), color = Color.White, fontSize = (16 * s).sp, fontWeight = FontWeight.Black)
            Text(F1Constants.sessionType(state.session.sessionType), color = TextDim, fontSize = (10 * s).sp)
        }
        Spacer(Modifier.weight(1f))
        Text(clock(state.session.sessionTimeLeft), color = Color.White, fontSize = (22 * s).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

// ---------------- LEFT ----------------

@Composable
private fun LeftColumn(state: TelemetryState, s: Float, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy((5 * s).dp)) {
        Panel("LIVE TRACK MAP", s, Modifier.fillMaxWidth().weight(1.7f)) {
            if (state.trackPath.size < 4) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Drive a lap to build the map", color = TextDim, fontSize = (11 * s).sp)
                }
            } else {
                LiveTrackMap(state.trackPath, state.cars, Modifier.fillMaxWidth().weight(1f))
            }
            TrackStatsRow(state, s)
        }
        Panel("LIVE TRACK INFORMATION", s, Modifier.fillMaxWidth().weight(0.6f)) {
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Stat("WARNINGS", "${state.lap.totalWarnings}", Color.White, s, Modifier.weight(1f))
                Stat("PENALTIES", "${state.lap.penaltiesSec}s", if (state.lap.penaltiesSec > 0) F1Red else Color.White, s, Modifier.weight(1f))
                Stat("REJOIN POS", if (state.lap.carPosition > 0) "${state.lap.carPosition}" else "-", AccentYellow, s, Modifier.weight(1f))
                Stat("PIT WINDOW", pitWindow(state), AccentCyan, s, Modifier.weight(1.1f))
            }
        }
        Panel("WEATHER INFORMATION", s, Modifier.fillMaxWidth().weight(0.75f)) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy((5 * s).dp)) {
                WeatherCell("NOW", state.session.weather, state.session.rainNowPct, s, Modifier.weight(1f))
                WeatherCell("+5 MIN", state.session.weather5, state.session.rain5Pct, s, Modifier.weight(1f))
                WeatherCell("+10 MIN", state.session.weather10, state.session.rain10Pct, s, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TrackStatsRow(state: TelemetryState, s: Float) {
    Row(Modifier.fillMaxWidth().padding(top = (3 * s).dp)) {
        Stat("TURNS", "${F1Constants.trackTurns(state.session.trackId)}", Color.White, s, Modifier.weight(1f))
        Stat("FULL THR", "${fullThrottlePct(state)}%", AccentGreen, s, Modifier.weight(1f))
        Stat("DOWNFORCE", downforce(state), AccentCyan, s, Modifier.weight(1f))
        Stat("TYRE WEAR", tyreWearLabel(state), AccentYellow, s, Modifier.weight(1f))
    }
}

@Composable
private fun WeatherCell(label: String, weather: Int, rain: Int, s: Float, modifier: Modifier) {
    Column(modifier.fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(CELL).padding((5 * s).dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(label, color = TextDim, fontSize = (9 * s).sp)
        Text(weatherIcon(weather), fontSize = (18 * s).sp)
        Text(F1Constants.weather(weather), color = Color.White, fontSize = (9 * s).sp, maxLines = 1, overflow = TextOverflow.Clip)
        Text("$rain%", color = AccentCyan, fontSize = (11 * s).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

// ---------------- CENTER ----------------

@Composable
private fun CenterColumn(state: TelemetryState, s: Float, modifier: Modifier) {
    val t = state.telemetry
    Panel("CAR INFORMATION", s, modifier) {
        InputTrace(state, s, Modifier.fillMaxWidth().weight(0.6f))
        Spacer(Modifier.height((2 * s).dp))
        GearBar(t.gear, s)
        Spacer(Modifier.height((3 * s).dp))
        Row(Modifier.fillMaxWidth().weight(0.8f), horizontalArrangement = Arrangement.spacedBy((6 * s).dp)) {
            VBar("ERS", (state.status.ersStoreEnergy / 40000f).coerceIn(0f, 100f) / 100f, AccentGreen, s, Modifier.weight(1f))
            VBar("BRAKE", t.brake, F1Red, s, Modifier.weight(1f))
            VBar("THROT", t.throttle, AccentGreen, s, Modifier.weight(1f))
            SteeringWheel(t.steer, s, Modifier.weight(1.6f).fillMaxHeight())
        }
        Spacer(Modifier.height((3 * s).dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("FUEL", "%.1f L".format(state.status.fuelInTank), Color.White, s, Modifier.weight(1f))
            Stat("LAPS", "%+.2f".format(state.status.fuelRemainingLaps), if (state.status.fuelRemainingLaps < 0) F1Red else AccentGreen, s, Modifier.weight(1f))
            Stat("BIAS", "${state.status.frontBrakeBias}%", Color.White, s, Modifier.weight(1f))
            Stat("DIFF", "${state.setup.onThrottleDiff}%", Color.White, s, Modifier.weight(1f))
            Stat("ERS", F1Constants.ersMode(state.status.ersDeployMode), AccentCyan, s, Modifier.weight(1.3f))
        }
        Spacer(Modifier.height((3 * s).dp))
        CarTyres(state, s, Modifier.fillMaxWidth().weight(1.15f))
    }
}

@Composable
private fun InputTrace(state: TelemetryState, s: Float, modifier: Modifier) {
    val trace = state.currentTrace ?: state.bestTrace
    Box(modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF0C0C12))) {
        if (trace == null || trace.samples.size < 2) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("input trace", color = TextDim, fontSize = (10 * s).sp)
            }
            return@Box
        }
        Canvas(Modifier.fillMaxSize().padding((3 * s).dp)) {
            val n = trace.samples.size
            fun draw(sel: (Int) -> Float, color: Color) {
                var started = false
                val path = androidx.compose.ui.graphics.Path()
                trace.samples.forEachIndexed { i, _ ->
                    val x = i.toFloat() / (n - 1) * size.width
                    val y = size.height - sel(i).coerceIn(0f, 1f) * size.height
                    if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(width = 2f))
            }
            draw({ trace.samples[it].throttle }, AccentGreen)
            draw({ trace.samples[it].brake }, F1Red)
        }
    }
}

@Composable
private fun GearBar(gear: Int, s: Float) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy((2 * s).dp)) {
        val labels = listOf("N", "1", "2", "3", "4", "5", "6", "7", "8")
        labels.forEachIndexed { idx, lbl ->
            val current = idx == gear.coerceIn(0, 8)
            val col = when {
                current && idx >= 6 -> AccentPurple
                current -> AccentGreen
                else -> CELL
            }
            Box(Modifier.weight(1f).height((16 * s).dp).clip(RoundedCornerShape(2.dp)).background(col), contentAlignment = Alignment.Center) {
                Text(lbl, color = if (current) Color.Black else TextDim, fontSize = (10 * s).sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VBar(label: String, fraction: Float, color: Color, s: Float, modifier: Modifier) {
    Column(modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${(fraction * 100).toInt()}%", color = Color.White, fontSize = (10 * s).sp, fontFamily = FontFamily.Monospace)
        Box(Modifier.width((16 * s).dp).weight(1f).clip(RoundedCornerShape(3.dp)).background(CELL), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(fraction.coerceIn(0f, 1f)).clip(RoundedCornerShape(3.dp)).background(color))
        }
        Text(label, color = TextDim, fontSize = (8 * s).sp)
    }
}

@Composable
private fun SteeringWheel(steer: Float, s: Float, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding((4 * s).dp)) {
            val cx = size.width / 2f; val cy = size.height / 2f
            val r = min(size.width, size.height) / 2.4f
            rotate(degrees = steer * 100f, pivot = Offset(cx, cy)) {
                drawCircle(Color(0xFF2A2A36), radius = r, center = Offset(cx, cy), style = Stroke(width = r * 0.28f))
                // top grips
                drawRoundRect(
                    color = F1Red,
                    topLeft = Offset(cx - r, cy - r * 0.35f),
                    size = androidx.compose.ui.geometry.Size(r * 2f, r * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.2f),
                )
                drawCircle(AccentCyan, radius = r * 0.16f, center = Offset(cx, cy))
            }
        }
    }
}

@Composable
private fun CarTyres(state: TelemetryState, s: Float, modifier: Modifier) {
    val t = state.telemetry
    val d = state.damage
    // Structured layout (front pair over rear pair, slim car in the middle) so
    // nothing overlaps or clips regardless of available height.
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
            TyreReadout("FL", 2, t, d, s)
            TyreReadout("RL", 0, t, d, s)
        }
        Box(Modifier.weight(0.45f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxHeight(0.9f).width((22 * s).dp)) {
                drawRoundRect(
                    color = Color(0xFF20202C),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.45f),
                )
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
            TyreReadout("FR", 3, t, d, s)
            TyreReadout("RR", 1, t, d, s)
        }
    }
}

@Composable
private fun TyreReadout(
    label: String,
    i: Int,
    t: com.f1telemetry.viewer.telemetry.CarTelemetry,
    d: com.f1telemetry.viewer.telemetry.CarDamage,
    s: Float,
) {
    val surface = t.tyreSurfaceTempC.getOrElse(i) { 0 }
    val inner = t.tyreInnerTempC.getOrElse(i) { 0 }
    val wear = d.tyreWearPct.getOrElse(i) { 0f }
    val brake = t.brakesTempC.getOrElse(i) { 0 }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width((8 * s).dp).height((26 * s).dp).clip(RoundedCornerShape(3.dp)).background(tempColor(surface)))
        Spacer(Modifier.width((5 * s).dp))
        Column {
            Text("$label  $surface°", color = Color.White, fontSize = (11 * s).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("wear ${wear.toInt()}%  core $inner°", color = wearColor(wear), fontSize = (8.5f * s).sp, fontFamily = FontFamily.Monospace)
            Text("brake $brake°", color = brakeTempColor(brake), fontSize = (8.5f * s).sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// ---------------- RIGHT ----------------

@Composable
private fun RightColumn(state: TelemetryState, s: Float, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy((5 * s).dp)) {
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(6.dp)).background(PANEL).padding((6 * s).dp)) {
            Column(Modifier.fillMaxSize()) {
                TowerHeaderRow(s)
                if (state.cars.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Waiting for the field…", color = TextDim, fontSize = (11 * s).sp)
                    }
                } else {
                    state.cars.forEach { car ->
                        TowerRow(car, car.index == state.fastestLapCarIndex, s, Modifier.weight(1f))
                    }
                }
            }
        }
        FocusBanner(state, s)
    }
}

@Composable
private fun TowerHeaderRow(s: Float) {
    Row(Modifier.fillMaxWidth().padding(bottom = (2 * s).dp), verticalAlignment = Alignment.CenterVertically) {
        TCell("", 0.7f, s); TCell("", 1.6f, s)
        TCell("", 0.5f, s); TCell("AGE", 0.6f, s); TCell("ERS", 0.9f, s); TCell("DRS", 0.7f, s)
        TCell("INTERVAL", 1.2f, s); TCell("LAST LAP", 1.2f, s); TCell("FASTEST", 1.2f, s)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TCell(text: String, weight: Float, s: Float) {
    Text(text, color = TextDim, fontSize = (8 * s).sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(weight))
}

@Composable
private fun ColumnScope.TowerRow(car: LiveCar, isFastest: Boolean, s: Float, modifier: Modifier) {
    val bg = if (car.isPlayer) Color(0xFF2A2A3A) else Color.Transparent
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)).background(bg), verticalAlignment = Alignment.CenterVertically) {
        Text("${car.position}", color = Color.White, fontSize = (11 * s).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, modifier = Modifier.weight(0.7f))
        Row(Modifier.weight(1.6f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width((3 * s).dp).height((13 * s).dp).background(Color(F1Constants.teamColor(car.teamId))))
            Spacer(Modifier.width((3 * s).dp))
            Text(car.abbrev, color = Color.White, fontSize = (12 * s).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Clip)
        }
        Text(F1Constants.tyreLetter(car.visualTyre), color = Color(F1Constants.tyreColor(car.visualTyre)), fontSize = (12 * s).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, modifier = Modifier.weight(0.5f))
        Text("${car.tyreAge}", color = TextDim, fontSize = (10 * s).sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f))
        Box(Modifier.weight(0.9f).padding(horizontal = (2 * s).dp)) {
            Box(Modifier.fillMaxWidth().height((10 * s).dp).clip(RoundedCornerShape(2.dp)).background(CELL))
            Box(Modifier.fillMaxWidth(car.ersPct / 100f).height((10 * s).dp).clip(RoundedCornerShape(2.dp)).background(if (car.ersPct > 25) AccentGreen else F1Red))
        }
        Box(Modifier.weight(0.7f), contentAlignment = Alignment.Center) {
            val c = if (car.drsOpen) AccentGreen else if (car.drsAllowed) Color(0xFF2E7DD1) else CELL
            Text("DRS", color = if (car.drsOpen) Color.Black else TextDim, fontSize = (7 * s).sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(2.dp)).background(c).padding(horizontal = (3 * s).dp))
        }
        Text(if (car.position <= 1) "INTERVAL" else if (car.pitting) "PIT" else "+${Fmt.sector(car.deltaAheadMs)}", color = if (car.pitting) AccentYellow else TextDim, fontSize = (9 * s).sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1.2f))
        Text(shortLap(car.lastLapMs), color = Color.White, fontSize = (9 * s).sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1.2f))
        Text(shortLap(car.bestLapMs), color = if (isFastest) AccentPurple else AccentCyan, fontSize = (9 * s).sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1.2f))
    }
}

@Composable
private fun FocusBanner(state: TelemetryState, s: Float) {
    val player = state.cars.firstOrNull { it.isPlayer }
    val idx = state.header?.playerCarIndex ?: -1
    val number = state.participants.getOrNull(idx)?.raceNumber ?: 0
    val teamId = player?.teamId ?: 255
    val teamColor = Color(F1Constants.teamColor(teamId))
    val name = state.playerName
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    val first = parts.firstOrNull() ?: name
    val last = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(PANEL).padding((7 * s).dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width((5 * s).dp).height((34 * s).dp).background(teamColor))
        Spacer(Modifier.width((8 * s).dp))
        Text("$number", color = teamColor, fontSize = (30 * s).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.width((10 * s).dp))
        Column(Modifier.weight(1f)) {
            if (last.isNotBlank()) {
                Text(first.uppercase(), color = TextDim, fontSize = (11 * s).sp)
            }
            Text((if (last.isNotBlank()) last else first).uppercase(), color = Color.White, fontSize = (20 * s).sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Clip)
            Text(F1Constants.team(teamId), color = TextDim, fontSize = (9 * s).sp)
        }
        Text(if (player != null) "P${player.position}" else "P-", color = teamColor, fontSize = (34 * s).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

// ---------------- shared ----------------

@Composable
private fun Panel(title: String, s: Float, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(6.dp)).background(PANEL).padding((6 * s).dp)) {
        Text(title, color = TITLE, fontSize = (11 * s).sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height((4 * s).dp))
        content()
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color, s: Float, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextDim, fontSize = (8 * s).sp, maxLines = 1)
        Text(value, color = color, fontSize = (14 * s).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1)
    }
}

private fun clock(secondsLeft: Int): String {
    if (secondsLeft <= 0) return "--:--"
    return "%d:%02d".format(secondsLeft / 60, secondsLeft % 60)
}

private fun shortLap(ms: Long): String {
    if (ms <= 0) return "—"
    return "%d:%02d.%03d".format(ms / 60000, (ms % 60000) / 1000, ms % 1000)
}

private fun fullThrottlePct(state: TelemetryState): Int {
    val tr = state.currentTrace ?: state.bestTrace ?: return 0
    if (tr.samples.isEmpty()) return 0
    val full = tr.samples.count { it.throttle > 0.9f }
    return full * 100 / tr.samples.size
}

private fun downforce(state: TelemetryState): String {
    val wings = state.setup.frontWing + state.setup.rearWing
    return when {
        wings <= 0 -> "—"
        wings < 25 -> "LOW"
        wings < 45 -> "MED"
        else -> "HIGH"
    }
}

private fun tyreWearLabel(state: TelemetryState): String {
    val w = state.damage.tyreWearPct
    if (w.all { it == 0f }) return "—"
    val avg = w.average()
    return when {
        avg < 15 -> "LOW"
        avg < 40 -> "MEDIUM"
        else -> "HIGH"
    }
}

private fun pitWindow(state: TelemetryState): String {
    // Rough estimate: a stint-length window centred on tyre life, no strategy data available.
    val total = state.session.totalLaps
    if (total <= 0) return "-"
    val cur = state.lap.currentLapNum
    val start = (cur + 3).coerceAtMost(total)
    val end = (cur + 9).coerceAtMost(total)
    return if (start >= end) "-" else "$start-$end"
}

private fun weatherIcon(weather: Int): String = when (weather) {
    0 -> "☀"; 1 -> "🌤"; 2 -> "☁"; 3 -> "🌦"; 4 -> "🌧"; 5 -> "⛈"; else -> "☁"
}
