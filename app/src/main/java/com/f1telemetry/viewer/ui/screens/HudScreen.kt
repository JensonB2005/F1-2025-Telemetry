package com.f1telemetry.viewer.ui.screens

import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f1telemetry.viewer.data.TelemetryState
import com.f1telemetry.viewer.telemetry.F1Constants
import com.f1telemetry.viewer.ui.Fmt
import com.f1telemetry.viewer.ui.components.brakeTempColor
import com.f1telemetry.viewer.ui.components.tempColor
import com.f1telemetry.viewer.ui.components.wearColor
import com.f1telemetry.viewer.ui.findActivity
import com.f1telemetry.viewer.ui.theme.AccentCyan
import com.f1telemetry.viewer.ui.theme.AccentGreen
import com.f1telemetry.viewer.ui.theme.AccentPurple
import com.f1telemetry.viewer.ui.theme.AccentYellow
import com.f1telemetry.viewer.ui.theme.F1Dark
import com.f1telemetry.viewer.ui.theme.F1Red
import com.f1telemetry.viewer.ui.theme.TextDim
import kotlin.math.min

/**
 * Full-screen, no-scroll driving HUD. Everything that streams live fits on one
 * landscape screen, auto-scaled to the device so nothing is ever clipped or
 * requires touching the screen. Keeps the display awake and locks to landscape
 * while active.
 */
@Composable
fun HudScreen(state: TelemetryState, onExit: () -> Unit) {
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
        onDispose {
            activity?.requestedOrientation = previous ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(F1Dark)) {
        // Scale fonts/spacing to the actual screen so it always fits without scrolling.
        val s = min(maxWidth.value / 820f, maxHeight.value / 380f).coerceIn(0.5f, 2.6f)
        val gap = (5 * s).dp

        Column(Modifier.fillMaxSize().padding((6 * s).dp), verticalArrangement = Arrangement.spacedBy(gap)) {
            TopStrip(state, s)
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                SpeedBlock(state, s, Modifier.weight(1f).fillMaxHeight())
                TimingBlock(state, s, Modifier.weight(1.05f).fillMaxHeight())
                TyreBlock(state, s, Modifier.weight(1.3f).fillMaxHeight())
            }
            BottomStrip(state, s)
        }

        // Exit affordance (tap before/after driving).
        Text(
            "✕",
            color = TextDim,
            fontSize = (16 * s).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding((2 * s).dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x66000000))
                .clickable { onExit() }
                .padding(horizontal = (8 * s).dp, vertical = (2 * s).dp),
        )
    }
}

@Composable
private fun TopStrip(state: TelemetryState, s: Float) {
    val t = state.telemetry
    val lap = state.lap
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (lap.carPosition > 0) "P${lap.carPosition}" else "P-",
            color = AccentYellow, fontSize = (26 * s).sp, fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.width((10 * s).dp))
        // Rev lights
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy((2 * s).dp)) {
            val segments = 15
            for (i in 0 until segments) {
                val on = t.revLightsPercent >= (i + 1) * (100 / segments)
                val col = when {
                    !on -> Color(0xFF2A2A36)
                    i < 6 -> AccentGreen
                    i < 11 -> F1Red
                    else -> AccentPurple
                }
                Box(Modifier.weight(1f).height((10 * s).dp).clip(RoundedCornerShape(2.dp)).background(col))
            }
        }
        Spacer(Modifier.width((10 * s).dp))
        val total = if (state.session.totalLaps > 0) "/${state.session.totalLaps}" else ""
        Text(
            "L${lap.currentLapNum}$total", color = Color.White, fontSize = (22 * s).sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.width((12 * s).dp))
        Chip("DRS", if (t.drs) Color.Black else TextDim, if (t.drs) AccentGreen else Color(0xFF2A2A36), s)
    }
}

@Composable
private fun SpeedBlock(state: TelemetryState, s: Float, modifier: Modifier) {
    val t = state.telemetry
    Panel(modifier) {
        Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(gear(t.gear), color = Color.White, fontSize = (60 * s).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                Cap("GEAR", s)
            }
            Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${t.speedKmh}", color = AccentCyan, fontSize = (58 * s).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                Cap("KM/H", s)
            }
        }
        Spacer(Modifier.height((4 * s).dp))
        Bar("THR", t.throttle, AccentGreen, s)
        Spacer(Modifier.height((3 * s).dp))
        Bar("BRK", t.brake, F1Red, s)
    }
}

@Composable
private fun TimingBlock(state: TelemetryState, s: Float, modifier: Modifier) {
    val lap = state.lap
    Panel(modifier) {
        Cap("CURRENT LAP", s)
        Text(Fmt.lapTime(lap.currentLapTimeMs), color = Color.White, fontSize = (34 * s).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height((3 * s).dp))
        Row(Modifier.fillMaxWidth()) {
            KV("LAST", Fmt.lapTime(lap.lastLapTimeMs), AccentCyan, s, Modifier.weight(1f))
            KV("BEST", Fmt.lapTime(state.bestLapTimeMs), AccentPurple, s, Modifier.weight(1f))
        }
        Spacer(Modifier.height((2 * s).dp))
        Row(Modifier.fillMaxWidth()) {
            KV("S1", Fmt.sector(lap.sector1Ms), TextDim, s, Modifier.weight(1f))
            KV("S2", Fmt.sector(lap.sector2Ms), TextDim, s, Modifier.weight(1f))
            KV("GAP LDR", Fmt.delta(lap.deltaToLeaderMs), AccentYellow, s, Modifier.weight(1.2f))
        }
    }
}

@Composable
private fun TyreBlock(state: TelemetryState, s: Float, modifier: Modifier) {
    val t = state.telemetry
    val d = state.damage
    Panel(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Cap("TYRES  ${F1Constants.visualTyre(state.status.visualTyreCompound)} • ${state.status.tyresAgeLaps}L", s)
        }
        Spacer(Modifier.height((3 * s).dp))
        // FL FR
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy((4 * s).dp)) {
            TyreCell(2, t, d, s, Modifier.weight(1f))
            TyreCell(3, t, d, s, Modifier.weight(1f))
        }
        Spacer(Modifier.height((4 * s).dp))
        // RL RR
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy((4 * s).dp)) {
            TyreCell(0, t, d, s, Modifier.weight(1f))
            TyreCell(1, t, d, s, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TyreCell(
    i: Int,
    t: com.f1telemetry.viewer.telemetry.CarTelemetry,
    d: com.f1telemetry.viewer.telemetry.CarDamage,
    s: Float,
    modifier: Modifier,
) {
    val temp = t.tyreSurfaceTempC.getOrElse(i) { 0 }
    val wear = d.tyreWearPct.getOrElse(i) { 0f }
    val brake = t.brakesTempC.getOrElse(i) { 0 }
    val label = listOf("RL", "RR", "FL", "FR")[i]
    Row(
        modifier.fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(Color(0xFF20202C)).padding((4 * s).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.height((30 * s).dp).width((7 * s).dp).clip(RoundedCornerShape(2.dp)).background(tempColor(temp)))
        Spacer(Modifier.width((5 * s).dp))
        Column {
            Text(label, color = TextDim, fontSize = (9 * s).sp)
            Text("$temp°", color = Color.White, fontSize = (17 * s).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("${wear.toInt()}% • ${brake}°", color = wearColor(wear), fontSize = (10 * s).sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun BottomStrip(state: TelemetryState, s: Float) {
    val st = state.status
    val t = state.telemetry
    val ersPct = (st.ersStoreEnergy / 40000f).coerceIn(0f, 100f).toInt()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy((5 * s).dp)) {
        MiniStat("ERS", "$ersPct%", if (ersPct > 25) AccentGreen else F1Red, s, Modifier.weight(1f))
        MiniStat("MODE", F1Constants.ersMode(st.ersDeployMode), AccentCyan, s, Modifier.weight(1f))
        MiniStat("FUEL", "%.1fkg".format(st.fuelInTank), Color.White, s, Modifier.weight(1f))
        MiniStat("Δ FUEL", "%.1f".format(st.fuelRemainingLaps), if (st.fuelRemainingLaps < 0) F1Red else AccentGreen, s, Modifier.weight(1f))
        MiniStat("MIX", F1Constants.fuelMix(st.fuelMix), Color.White, s, Modifier.weight(1f))
        MiniStat("BIAS", "${st.frontBrakeBias}%", Color.White, s, Modifier.weight(1f))
        MiniStat("ENG", "${t.engineTempC}°", if (t.engineTempC > 130) AccentYellow else Color.White, s, Modifier.weight(1f))
        MiniStat("WARN", "${state.lap.totalWarnings}", if (state.lap.totalWarnings > 0) AccentYellow else Color.White, s, Modifier.weight(1f))
        MiniStat("PEN", "${state.lap.penaltiesSec}s", if (state.lap.penaltiesSec > 0) F1Red else Color.White, s, Modifier.weight(1f))
    }
}

// --- small building blocks ---

@Composable
private fun Panel(modifier: Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF1B1B26)).padding(8.dp),
        content = content,
    )
}

@Composable
private fun Cap(text: String, s: Float) {
    Text(text, color = TextDim, fontSize = (10 * s).sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
}

@Composable
private fun KV(label: String, value: String, color: Color, s: Float, modifier: Modifier = Modifier) {
    Column(modifier) {
        Cap(label, s)
        Text(value, color = color, fontSize = (16 * s).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1)
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color, s: Float, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1B1B26)).padding(vertical = (5 * s).dp, horizontal = (4 * s).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = TextDim, fontSize = (9 * s).sp, maxLines = 1)
        Text(value, color = color, fontSize = (15 * s).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1)
    }
}

@Composable
private fun Bar(label: String, value: Float, color: Color, s: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextDim, fontSize = (10 * s).sp, modifier = Modifier.width((30 * s).dp))
        Box(Modifier.weight(1f).height((12 * s).dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF2A2A36))) {
            Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height((12 * s).dp).clip(RoundedCornerShape(3.dp)).background(color))
        }
        Text("${(value * 100).toInt()}", color = Color.White, fontSize = (11 * s).sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width((26 * s).dp).padding(start = (4 * s).dp))
    }
}

@Composable
private fun Chip(text: String, fg: Color, bg: Color, s: Float) {
    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(bg).padding(horizontal = (6 * s).dp, vertical = (2 * s).dp)) {
        Text(text, color = fg, fontSize = (12 * s).sp, fontWeight = FontWeight.Bold)
    }
}

private fun gear(g: Int): String = when (g) {
    -1 -> "R"; 0 -> "N"; else -> "$g"
}
