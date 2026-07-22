package com.f1telemetry.viewer.analysis

import kotlin.math.abs

/**
 * Running understeer/oversteer estimate from steering input vs. actual lateral
 * grip (lateral g). In a corner:
 *  - lots of steering but little lateral g  => the fronts aren't biting (understeer)
 *  - little steering but high lateral g / opposite-lock corrections => loose rear (oversteer)
 *
 * We only sample genuine cornering (meaningful steer + speed) and report the
 * dominant tendency over a rolling window.
 */
class HandlingEstimator(private val window: Int = 600) {
    private var understeer = 0
    private var oversteer = 0
    private var total = 0
    private var prevSteer = 0f

    fun sample(steer: Float, gLat: Float, speedKmh: Int) {
        if (speedKmh < 55) { prevSteer = steer; return }
        val absSteer = abs(steer)
        if (absSteer < 0.15f) { prevSteer = steer; return }

        val absG = abs(gLat)
        // Normalised "grip per unit steering": low => car won't rotate (understeer).
        val gripRatio = absG / absSteer
        val opposite = steer * prevSteer < 0f && abs(steer - prevSteer) > 0.3f

        if (gripRatio < 1.6f) understeer++
        else if (opposite || gripRatio > 4.5f) oversteer++
        total++

        if (total > window) {
            understeer = (understeer * 3) / 4
            oversteer = (oversteer * 3) / 4
            total = (total * 3) / 4
        }
        prevSteer = steer
    }

    fun balance(): HandlingBalance {
        if (total < 30) return HandlingBalance(Tendency.NEUTRAL, 0)
        val uPct = understeer * 100 / total
        val oPct = oversteer * 100 / total
        return when {
            uPct >= 35 && uPct >= oPct -> HandlingBalance(Tendency.UNDERSTEER, uPct)
            oPct >= 30 -> HandlingBalance(Tendency.OVERSTEER, oPct)
            else -> HandlingBalance(Tendency.NEUTRAL, maxOf(uPct, oPct))
        }
    }

    fun reset() { understeer = 0; oversteer = 0; total = 0; prevSteer = 0f }
}
