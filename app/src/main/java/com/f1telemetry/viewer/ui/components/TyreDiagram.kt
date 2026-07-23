package com.f1telemetry.viewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.f1telemetry.viewer.ui.theme.TextDim

/**
 * Four-corner car layout. Values arrive in the F1 array order [RL, RR, FL, FR]
 * and are rendered in their physical positions with a heat/severity colour.
 */
@Composable
fun CornerGrid(
    title: String,
    values: List<String>,     // [RL, RR, FL, FR]
    colors: List<Color>,      // [RL, RR, FL, FR]
    subLabels: List<String>? = null,
) {
    // Physical layout:  FL FR / RL RR
    Column(Modifier.fillMaxWidth()) {
        Text(title.uppercase(), color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Corner("FL", values[2], colors[2], subLabels?.getOrNull(2), Modifier.weight(1f))
            Corner("FR", values[3], colors[3], subLabels?.getOrNull(3), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Corner("RL", values[0], colors[0], subLabels?.getOrNull(0), Modifier.weight(1f))
            Corner("RR", values[1], colors[1], subLabels?.getOrNull(1), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Corner(pos: String, value: String, color: Color, sub: String?, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF20202C))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(pos, color = TextDim, fontSize = 10.sp)
        Box(
            Modifier
                .padding(vertical = 4.dp)
                .size(width = 26.dp, height = 34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color),
        )
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        if (sub != null) Text(sub, color = TextDim, fontSize = 9.sp)
    }
}

fun tempColor(tempC: Int): Color = when {
    tempC <= 0 -> Color(0xFF3A3A46)
    tempC < 80 -> Color(0xFF2E7DD1)   // cold - blue
    tempC < 95 -> Color(0xFF39A0E7)
    tempC < 110 -> Color(0xFF39E75F)  // ideal - green
    tempC < 125 -> Color(0xFFFFB020)  // hot - amber
    else -> Color(0xFFE10600)         // overheating - red
}

fun wearColor(pct: Float): Color = when {
    pct < 15 -> Color(0xFF39E75F)
    pct < 35 -> Color(0xFFFFD400)
    pct < 55 -> Color(0xFFFFB020)
    else -> Color(0xFFE10600)
}

fun brakeTempColor(tempC: Int): Color = when {
    tempC <= 0 -> Color(0xFF3A3A46)
    tempC < 250 -> Color(0xFF2E7DD1)
    tempC < 750 -> Color(0xFF39E75F)
    tempC < 1000 -> Color(0xFFFFB020)
    else -> Color(0xFFE10600)
}
