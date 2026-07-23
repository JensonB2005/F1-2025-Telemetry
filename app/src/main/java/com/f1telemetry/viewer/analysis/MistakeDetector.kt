package com.f1telemetry.viewer.analysis

import com.f1telemetry.viewer.data.Insight
import com.f1telemetry.viewer.data.InsightLevel
import com.f1telemetry.viewer.data.LapTrace
import com.f1telemetry.viewer.data.TraceSample
import kotlin.math.abs

/**
 * Heuristic driving-mistake detection over a completed lap trace, plus a
 * lap-to-lap comparison against the driver's best lap to localise time loss.
 *
 * These are intentionally simple, explainable rules — not a physics model — so
 * every insight can point at a distance on track and say why.
 */
object MistakeDetector {

    fun analyseLap(trace: LapTrace, best: LapTrace?): List<Insight> {
        val out = ArrayList<Insight>()
        val s = trace.samples
        if (s.size < 20) return out

        detectLockups(s, trace.lapNumber, out)
        detectThrottleStabs(s, trace.lapNumber, out)
        detectShortShifting(s, trace.lapNumber, out)
        detectCoasting(s, trace.lapNumber, out)
        if (!trace.valid) {
            out.add(
                Insight(
                    InsightLevel.MISTAKE, "Lap invalidated",
                    "Track limits were exceeded or the car left the circuit on this lap.",
                    "Track limits", lap = trace.lapNumber,
                )
            )
        }
        if (best != null && best.samples.size > 20 && best.lapNumber != trace.lapNumber) {
            compareToBest(trace, best, out)
        }
        return out
    }

    /** Sustained hard braking with a sharp speed collapse => probable lock-up. */
    private fun detectLockups(s: List<TraceSample>, lap: Int, out: MutableList<Insight>) {
        var i = 1
        while (i < s.size) {
            if (s[i].brake > 0.85f) {
                val start = i
                var maxDrop = 0
                while (i < s.size && s[i].brake > 0.6f) {
                    val drop = s[i - 1].speed - s[i].speed
                    if (drop > maxDrop) maxDrop = drop
                    i++
                }
                // A very abrupt single-sample speed loss under max brake is the
                // signature of a locked wheel biting then releasing.
                if (maxDrop >= 18) {
                    out.add(
                        Insight(
                            InsightLevel.MISTAKE, "Possible brake lock-up",
                            "Full braking with an abrupt ${maxDrop} km/h speed drop. Ease initial " +
                                "brake pressure or shift brake bias rearward slightly here.",
                            "Braking", atDistance = s[start].distance, lap = lap,
                        )
                    )
                }
            } else i++
        }
    }

    /** Throttle oscillation while cornering => traction loss / nervous rear. */
    private fun detectThrottleStabs(s: List<TraceSample>, lap: Int, out: MutableList<Insight>) {
        var i = 2
        var reported = 0
        while (i < s.size && reported < 4) {
            val cornering = abs(s[i].steer) > 0.25f && s[i].speed > 60
            val stab = s[i].throttle < s[i - 1].throttle - 0.25f &&
                s[i - 1].throttle > s[i - 2].throttle + 0.15f
            if (cornering && stab) {
                out.add(
                    Insight(
                        InsightLevel.WARNING, "Throttle correction mid-corner",
                        "Throttle was picked up then backed off while turning — the rear likely " +
                            "stepped out. Smoother throttle or more rear stability would help.",
                        "Traction", atDistance = s[i].distance, lap = lap,
                    )
                )
                reported++
                i += 10
            } else i++
        }
    }

    /** Upshifting well below the rev limit under load loses acceleration. */
    private fun detectShortShifting(s: List<TraceSample>, lap: Int, out: MutableList<Insight>) {
        var reported = 0
        for (i in 1 until s.size) {
            if (reported >= 2) break
            val upshift = s[i].gear > s[i - 1].gear && s[i - 1].gear in 1..7
            if (upshift && s[i - 1].throttle > 0.9f && s[i - 1].speed < 250 && s[i - 1].rpm > 0) {
                // Without max-RPM here we approximate: flag upshifts at clearly low rpm under full throttle.
                if (s[i - 1].rpm < 9000) {
                    out.add(
                        Insight(
                            InsightLevel.TIP, "Early upshift",
                            "Upshift under full throttle at ~${s[i - 1].rpm} rpm — holding the gear " +
                                "longer keeps the engine in its power band.",
                            "Shifting", atDistance = s[i].distance, lap = lap,
                        )
                    )
                    reported++
                }
            }
        }
    }

    /** Long neither-brake-nor-throttle zones waste lap time. */
    private fun detectCoasting(s: List<TraceSample>, lap: Int, out: MutableList<Insight>) {
        var i = 0
        var reported = 0
        while (i < s.size && reported < 2) {
            if (s[i].throttle < 0.05f && s[i].brake < 0.05f && s[i].speed > 80) {
                val start = i
                while (i < s.size && s[i].throttle < 0.05f && s[i].brake < 0.05f) i++
                val dist = s[minOf(i, s.size - 1)].distance - s[start].distance
                if (dist > 40f) {
                    out.add(
                        Insight(
                            InsightLevel.TIP, "Coasting detected",
                            "~${dist.toInt()} m with no throttle or brake. Trail-brake later or get " +
                                "back to power sooner to carry more speed.",
                            "Efficiency", atDistance = s[start].distance, lap = lap,
                        )
                    )
                    reported++
                }
            } else i++
        }
    }

    /** Bucketed distance comparison to find where the lap lost the most time. */
    private fun compareToBest(trace: LapTrace, best: LapTrace, out: MutableList<Insight>) {
        val bucket = 50f
        val curSpeed = bucketAvgSpeed(trace.samples, bucket)
        val bestSpeed = bucketAvgSpeed(best.samples, bucket)
        var worstKey = -1
        var worstDelta = 0f
        for ((k, v) in bestSpeed) {
            val cur = curSpeed[k] ?: continue
            val delta = v - cur // positive => slower than best here
            if (delta > worstDelta) { worstDelta = delta; worstKey = k }
        }
        if (worstKey >= 0 && worstDelta >= 8f) {
            out.add(
                Insight(
                    InsightLevel.INFO, "Biggest time loss vs best lap",
                    "Around ${(worstKey * bucket).toInt()} m you were ~${worstDelta.toInt()} km/h " +
                        "slower than your best lap — the largest single loss on this lap.",
                    "Delta", atDistance = worstKey * bucket, lap = trace.lapNumber,
                )
            )
        }
    }

    private fun bucketAvgSpeed(samples: List<TraceSample>, bucket: Float): Map<Int, Float> {
        val sum = HashMap<Int, Float>()
        val cnt = HashMap<Int, Int>()
        for (p in samples) {
            val k = (p.distance / bucket).toInt()
            sum[k] = (sum[k] ?: 0f) + p.speed
            cnt[k] = (cnt[k] ?: 0) + 1
        }
        return sum.mapValues { (k, v) -> v / (cnt[k] ?: 1) }
    }
}
