package com.f1telemetry.viewer.analysis

import com.f1telemetry.viewer.data.Insight
import com.f1telemetry.viewer.data.InsightLevel
import com.f1telemetry.viewer.data.TelemetryState
import kotlin.math.abs

/**
 * Turns live car state (tyre temps & pressures, wear balance, brake temps,
 * damage, and a running understeer/oversteer estimate) into concrete,
 * F1-style setup change suggestions.
 *
 * Ideal windows are broad, road-legal approximations for the F1 games rather
 * than exact per-track figures — they exist to point the driver in a direction.
 */
object SetupAdvisor {

    // Approx ideal operating windows for the F1 games (°C / psi).
    private const val TYRE_TEMP_LOW = 85
    private const val TYRE_TEMP_HIGH = 110
    private const val BRAKE_TEMP_LOW = 250
    private const val BRAKE_TEMP_HIGH = 750
    private const val FRONT_PSI_TARGET = 23.0f
    private const val REAR_PSI_TARGET = 21.0f

    fun advise(state: TelemetryState, balance: HandlingBalance): List<Insight> {
        val out = ArrayList<Insight>()
        val t = state.telemetry
        val d = state.damage
        val s = state.setup

        // --- Tyre temperatures ---
        // Tyre arrays are ordered [RL, RR, FL, FR]; fronts are indices 2 and 3.
        val surf = t.tyreSurfaceTempC
        if (surf.any { it > 0 }) {
            val avg = surf.average()
            if (avg in 1.0..TYRE_TEMP_LOW.toDouble()) {
                out.add(
                    Insight(
                        InsightLevel.TIP, "Tyres running cold",
                        "Surface temps average ${avg.toInt()}°C (target >$TYRE_TEMP_LOW°C). Lower tyre " +
                            "pressures slightly, or push harder to build temperature.",
                        "Tyres",
                    )
                )
            } else if (avg > TYRE_TEMP_HIGH) {
                out.add(
                    Insight(
                        InsightLevel.WARNING, "Tyres overheating",
                        "Surface temps average ${avg.toInt()}°C (target <$TYRE_TEMP_HIGH°C). Raise " +
                            "pressures, add cooling, or manage throttle/steering aggression to save the tyres.",
                        "Tyres",
                    )
                )
            }
            // Front/rear temp imbalance -> handling bias
            val front = (surf[2] + surf[3]) / 2.0
            val rear = (surf[0] + surf[1]) / 2.0
            if (front - rear > 12) {
                out.add(
                    Insight(
                        InsightLevel.TIP, "Fronts hotter than rears",
                        "Front tyres are ${(front - rear).toInt()}°C hotter — a sign of understeer/front " +
                            "overwork. Try less front wing relative to rear, or soften the front anti-roll bar.",
                        "Balance",
                    )
                )
            } else if (rear - front > 12) {
                out.add(
                    Insight(
                        InsightLevel.TIP, "Rears hotter than fronts",
                        "Rear tyres are ${(rear - front).toInt()}°C hotter — often oversteer/rear overwork. " +
                            "Add rear wing, soften the rear anti-roll bar, or ease on-throttle differential.",
                        "Balance",
                    )
                )
            }
        }

        // --- Tyre pressures ---
        val p = t.tyrePressurePsi
        if (p.any { it > 0f }) {
            val fAvg = (p[2] + p[3]) / 2f
            val rAvg = (p[0] + p[1]) / 2f
            if (abs(fAvg - FRONT_PSI_TARGET) > 1.2f) {
                out.add(
                    Insight(
                        InsightLevel.INFO, "Front pressure off target",
                        "Front ~%.1f psi vs ~%.1f target. %s".format(
                            fAvg, FRONT_PSI_TARGET,
                            if (fAvg > FRONT_PSI_TARGET) "Drop front pressure for more grip and even wear." else "Raise front pressure to reach the working window.",
                        ),
                        "Tyres",
                    )
                )
            }
            if (abs(rAvg - REAR_PSI_TARGET) > 1.2f) {
                out.add(
                    Insight(
                        InsightLevel.INFO, "Rear pressure off target",
                        "Rear ~%.1f psi vs ~%.1f target. %s".format(
                            rAvg, REAR_PSI_TARGET,
                            if (rAvg > REAR_PSI_TARGET) "Drop rear pressure for traction and even wear." else "Raise rear pressure toward the window.",
                        ),
                        "Tyres",
                    )
                )
            }
        }

        // --- Brake temperatures ---
        val bt = t.brakesTempC
        if (bt.any { it > 0 }) {
            val bAvg = bt.average()
            if (bAvg > BRAKE_TEMP_HIGH) {
                out.add(
                    Insight(
                        InsightLevel.WARNING, "Brakes overheating",
                        "Brake temps ~${bAvg.toInt()}°C. Open brake ducts (more cooling), brake earlier/" +
                            "lighter, or lift-and-coast to protect them.",
                        "Brakes",
                    )
                )
            } else if (bAvg in 1.0..BRAKE_TEMP_LOW.toDouble()) {
                out.add(
                    Insight(
                        InsightLevel.TIP, "Brakes cold",
                        "Brake temps ~${bAvg.toInt()}°C — below the ideal window. Close brake ducts for " +
                            "more consistent bite.",
                        "Brakes",
                    )
                )
            }
        }

        // --- Wear balance -> longevity / pressure & camber ---
        val wear = d.tyreWearPct
        if (wear.any { it > 0f }) {
            val fW = (wear[2] + wear[3]) / 2f
            val rW = (wear[0] + wear[1]) / 2f
            if (fW - rW > 5f) {
                out.add(
                    Insight(
                        InsightLevel.TIP, "Fronts wearing faster",
                        "Front wear leads rear by ${(fW - rW).toInt()}%. Reduce front camber, soften front, " +
                            "or ease steering aggression to extend a stint.",
                        "Tyres",
                    )
                )
            } else if (rW - fW > 5f) {
                out.add(
                    Insight(
                        InsightLevel.TIP, "Rears wearing faster",
                        "Rear wear leads front by ${(rW - fW).toInt()}%. Smoother throttle, less on-throttle " +
                            "diff, or a touch more rear wing helps rear life.",
                        "Tyres",
                    )
                )
            }
        }

        // --- Handling balance from motion + steering ---
        when (balance.tendency) {
            Tendency.UNDERSTEER -> out.add(
                Insight(
                    InsightLevel.TIP, "Understeer tendency",
                    "The car is pushing wide in corners (${balance.confidencePct}% of sampled turns). " +
                        "Add front wing, soften the front anti-roll bar, or raise rear ride height slightly.",
                    "Balance",
                )
            )
            Tendency.OVERSTEER -> out.add(
                Insight(
                    InsightLevel.TIP, "Oversteer tendency",
                    "The rear is loose on entry/exit (${balance.confidencePct}% of sampled turns). " +
                        "Add rear wing, soften the rear anti-roll bar, or reduce on-throttle differential.",
                    "Balance",
                )
            )
            Tendency.NEUTRAL -> {}
        }

        // --- Damage ---
        addDamage(d.frontLeftWingDamage.coerceAtLeast(d.frontRightWingDamage), "Front wing", out)
        addDamage(d.rearWingDamage, "Rear wing", out)
        addDamage(d.floorDamage, "Floor", out)
        addDamage(d.diffuserDamage, "Diffuser", out)
        if (d.drsFault) out.add(Insight(InsightLevel.WARNING, "DRS fault", "DRS is faulty — expect reduced straight-line speed.", "Damage"))
        if (d.ersFault) out.add(Insight(InsightLevel.WARNING, "ERS fault", "ERS is faulty — deployment is compromised.", "Damage"))

        // --- Fuel & ERS management ---
        if (state.status.fuelRemainingLaps < -0.3f) {
            out.add(
                Insight(
                    InsightLevel.WARNING, "Fuel short",
                    "Projected ${"%.1f".format(state.status.fuelRemainingLaps)} laps of fuel margin. " +
                        "Lean the mix or lift-and-coast to make the finish.",
                    "Fuel",
                )
            )
        }

        return out
    }

    private fun addDamage(pct: Int, part: String, out: MutableList<Insight>) {
        if (pct >= 20) {
            out.add(
                Insight(
                    if (pct >= 40) InsightLevel.WARNING else InsightLevel.TIP,
                    "$part damage ${pct}%",
                    "$part is at ${pct}% damage, hurting downforce/handling. Consider a pit repair.",
                    "Damage",
                )
            )
        }
    }
}

enum class Tendency { UNDERSTEER, OVERSTEER, NEUTRAL }

data class HandlingBalance(
    val tendency: Tendency,
    val confidencePct: Int,
)
