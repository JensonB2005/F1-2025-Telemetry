package com.f1telemetry.viewer.data

import com.f1telemetry.viewer.telemetry.CarDamage
import com.f1telemetry.viewer.telemetry.CarSetup
import com.f1telemetry.viewer.telemetry.CarStatus
import com.f1telemetry.viewer.telemetry.CarTelemetry
import com.f1telemetry.viewer.telemetry.Header
import com.f1telemetry.viewer.telemetry.LapData
import com.f1telemetry.viewer.telemetry.MotionData
import com.f1telemetry.viewer.telemetry.ParticipantInfo
import com.f1telemetry.viewer.telemetry.SessionInfo
import com.f1telemetry.viewer.telemetry.TelemetryEvent

/** One sampled point along a lap, keyed by distance for lap-to-lap comparison. */
data class TraceSample(
    val distance: Float,
    val timeMs: Long,
    val speed: Int,
    val throttle: Float,
    val brake: Float,
    val steer: Float,
    val gear: Int,
    val gLat: Float,
    val gLon: Float,
    val rpm: Int,
)

/** A completed (or in-progress) lap trace. */
data class LapTrace(
    val lapNumber: Int,
    val samples: List<TraceSample>,
    val lapTimeMs: Long,
    val valid: Boolean,
)

/** Immutable snapshot of everything the app knows, exposed to the UI. */
data class TelemetryState(
    val connected: Boolean = false,
    val lastPacketAtMs: Long = 0L,
    val packetsReceived: Long = 0L,
    val datagramsReceived: Long = 0L,
    val lastSenderIp: String = "",
    val packetFormat: Int = 0,
    val gameYear: Int = 0,
    val source: String = "Live UDP",

    val header: Header? = null,
    val telemetry: CarTelemetry = CarTelemetry(),
    val lap: LapData = LapData(),
    val status: CarStatus = CarStatus(),
    val damage: CarDamage = CarDamage(),
    val setup: CarSetup = CarSetup(),
    val motion: MotionData = MotionData(),
    val session: SessionInfo = SessionInfo(),
    val participants: List<ParticipantInfo> = emptyList(),

    val bestLapTimeMs: Long = 0L,
    val currentTrace: LapTrace? = null,
    val bestTrace: LapTrace? = null,
    val lastTrace: LapTrace? = null,

    val events: List<TelemetryEvent> = emptyList(),
    val insights: List<Insight> = emptyList(),

    val cars: List<LiveCar> = emptyList(),
    val trackPath: List<Pair<Float, Float>> = emptyList(),
    val fastestLapCarIndex: Int = -1,
    val fastestLapMs: Long = 0L,
) {
    val playerName: String
        get() = header?.let { participants.getOrNull(it.playerCarIndex)?.name }?.takeIf { it.isNotBlank() } ?: "Player"
}

/** Aggregated per-car row for the timing tower and track map. */
data class LiveCar(
    val index: Int,
    val position: Int,
    val name: String,
    val abbrev: String,
    val teamId: Int,
    val lastLapMs: Long,
    val bestLapMs: Long,
    val visualTyre: Int,
    val tyreAge: Int,
    val ersPct: Int,
    val drsOpen: Boolean,
    val drsAllowed: Boolean,
    val pitting: Boolean,
    val resultStatus: Int,
    val penaltiesSec: Int,
    val deltaAheadMs: Int,
    val deltaLeaderMs: Int,
    val lapDistance: Float,
    val worldX: Float,
    val worldZ: Float,
    val isPlayer: Boolean,
)

enum class InsightLevel { INFO, TIP, WARNING, MISTAKE }

data class Insight(
    val level: InsightLevel,
    val title: String,
    val detail: String,
    val category: String,
    val atDistance: Float? = null,
    val lap: Int? = null,
)
