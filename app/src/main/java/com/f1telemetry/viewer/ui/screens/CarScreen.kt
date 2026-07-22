package com.f1telemetry.viewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.f1telemetry.viewer.data.TelemetryState
import com.f1telemetry.viewer.telemetry.F1Constants
import com.f1telemetry.viewer.ui.components.CornerGrid
import com.f1telemetry.viewer.ui.components.LabeledValue
import com.f1telemetry.viewer.ui.components.SectionCard
import com.f1telemetry.viewer.ui.components.StatTile
import com.f1telemetry.viewer.ui.components.brakeTempColor
import com.f1telemetry.viewer.ui.components.tempColor
import com.f1telemetry.viewer.ui.components.wearColor
import com.f1telemetry.viewer.ui.theme.AccentGreen
import com.f1telemetry.viewer.ui.theme.AccentYellow
import com.f1telemetry.viewer.ui.theme.F1Red
import com.f1telemetry.viewer.ui.theme.TextDim

@Composable
fun CarScreen(state: TelemetryState) {
    val t = state.telemetry
    val d = state.damage
    val status = state.status
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard("Tyre surface temperature (°C)") {
            CornerGrid(
                title = "Compound: ${F1Constants.tyreCompound(status.actualTyreCompound)} (${F1Constants.visualTyre(status.visualTyreCompound)})  •  Age ${status.tyresAgeLaps} laps",
                values = t.tyreSurfaceTempC.map { "$it" },
                colors = t.tyreSurfaceTempC.map { tempColor(it) },
                subLabels = t.tyreInnerTempC.map { "core $it°" },
            )
        }

        SectionCard("Tyre pressure (psi)") {
            CornerGrid(
                title = "Working window ≈ 21–24 psi",
                values = t.tyrePressurePsi.map { "%.1f".format(it) },
                colors = t.tyrePressurePsi.map { pressureColor(it) },
            )
        }

        SectionCard("Tyre wear (%)") {
            CornerGrid(
                title = "Grip loss & pit strategy",
                values = d.tyreWearPct.map { "%.0f".format(it) },
                colors = d.tyreWearPct.map { wearColor(it) },
                subLabels = d.tyreDamagePct.map { "dmg ${it}%" },
            )
        }

        SectionCard("Brake temperature (°C)") {
            CornerGrid(
                title = "Working window ≈ 250–750 °C",
                values = t.brakesTempC.map { "$it" },
                colors = t.brakesTempC.map { brakeTempColor(it) },
                subLabels = d.brakeDamagePct.map { "dmg ${it}%" },
            )
        }

        SectionCard("Power unit") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("ERS store", "${(status.ersStoreEnergy / 40000f).coerceIn(0f, 100f).toInt()}", unit = "%", accent = AccentGreen, modifier = Modifier.weight(1f))
                StatTile("Deployed", "%.0f".format(status.ersDeployedThisLap / 1000f), unit = "kJ", modifier = Modifier.weight(1f))
                StatTile("Harvested", "%.0f".format((status.ersHarvestedThisLapMGUK + status.ersHarvestedThisLapMGUH) / 1000f), unit = "kJ", accent = AccentYellow, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            LabeledValue("Engine temp", "${t.engineTempC}°C")
            LabeledValue("Fuel in tank", "%.2f kg / %.0f kg".format(status.fuelInTank, status.fuelCapacity))
            LabeledValue("Fuel remaining", "%.2f laps".format(status.fuelRemainingLaps), if (status.fuelRemainingLaps < 0) F1Red else AccentGreen)
            LabeledValue("Fuel mix", F1Constants.fuelMix(status.fuelMix))
        }

        SectionCard("Damage") {
            LabeledValue("Front wing L / R", "${d.frontLeftWingDamage}% / ${d.frontRightWingDamage}%", dmgColor(maxOf(d.frontLeftWingDamage, d.frontRightWingDamage)))
            LabeledValue("Rear wing", "${d.rearWingDamage}%", dmgColor(d.rearWingDamage))
            LabeledValue("Floor", "${d.floorDamage}%", dmgColor(d.floorDamage))
            LabeledValue("Diffuser", "${d.diffuserDamage}%", dmgColor(d.diffuserDamage))
            LabeledValue("Sidepod", "${d.sidepodDamage}%", dmgColor(d.sidepodDamage))
            LabeledValue("Gearbox", "${d.gearboxDamage}%", dmgColor(d.gearboxDamage))
            LabeledValue("Engine", "${d.engineDamage}%", dmgColor(d.engineDamage))
            LabeledValue("DRS / ERS fault", "${if (d.drsFault) "DRS FAULT" else "ok"} / ${if (d.ersFault) "ERS FAULT" else "ok"}", if (d.drsFault || d.ersFault) F1Red else AccentGreen)
        }

        Text(
            "Tyre & brake positions: FL FR (front) / RL RR (rear).",
            color = TextDim, fontSize = 11.sp,
        )
    }
}

private fun pressureColor(psi: Float) = when {
    psi <= 0f -> androidx.compose.ui.graphics.Color(0xFF3A3A46)
    psi < 20f -> androidx.compose.ui.graphics.Color(0xFF2E7DD1)
    psi <= 24f -> AccentGreen
    psi <= 26f -> AccentYellow
    else -> F1Red
}

private fun dmgColor(pct: Int) = when {
    pct <= 0 -> AccentGreen
    pct < 20 -> AccentYellow
    pct < 40 -> androidx.compose.ui.graphics.Color(0xFFFFB020)
    else -> F1Red
}
