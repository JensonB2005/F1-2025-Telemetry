package com.f1telemetry.viewer.data

import com.f1telemetry.viewer.analysis.HandlingEstimator
import com.f1telemetry.viewer.analysis.MistakeDetector
import com.f1telemetry.viewer.analysis.SetupAdvisor
import com.f1telemetry.viewer.net.SessionRecorder
import com.f1telemetry.viewer.telemetry.DriverCodes
import com.f1telemetry.viewer.telemetry.F1Constants
import com.f1telemetry.viewer.telemetry.MotionData
import com.f1telemetry.viewer.telemetry.Parsed
import com.f1telemetry.viewer.telemetry.PacketParser
import com.f1telemetry.viewer.telemetry.TelemetryEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central store: consumes raw UDP payloads (live or replayed), parses them,
 * maintains the aggregated [TelemetryState], builds per-lap traces, and runs
 * the mistake / setup analysis. Packets are processed on a single stream at a
 * time (live receiver OR replayer), so the working buffers need no locking.
 */
object TelemetryRepository {

    private val _state = MutableStateFlow(TelemetryState())
    val state: StateFlow<TelemetryState> = _state.asStateFlow()

    private val handling = HandlingEstimator()
    private val working = ArrayList<TraceSample>(1400)
    private var curLapNum = -1
    private var lapInvalidFlag = false
    private var lastSampleDistance = -1000f
    private var latestMotion = MotionData()
    private var lastAdviceMs = 0L
    private var lastTracePublishMs = 0L
    private val lapInsights = ArrayList<Insight>()
    private var liveInsights: List<Insight> = emptyList()

    // All-car state for the timing tower and track map.
    private var lapAll: List<com.f1telemetry.viewer.telemetry.CarLapLite> = emptyList()
    private var statusAll: List<com.f1telemetry.viewer.telemetry.CarStatusLite> = emptyList()
    private var drsAll: List<Boolean> = emptyList()
    private var posAll: List<Pair<Float, Float>> = emptyList()
    private val bestLapByCar = LongArray(F1Constants.MAX_CARS)
    private val trackBuckets = HashMap<Int, Pair<Float, Float>>()
    private var trackPathCache: List<Pair<Float, Float>> = emptyList()

    @Volatile private var recorder: SessionRecorder? = null

    fun setRecorder(r: SessionRecorder?) { recorder = r }
    fun isRecording(): Boolean = recorder != null

    /** Reset transient analysis state (e.g. when switching source or session). */
    fun resetSession(source: String) {
        handling.reset()
        working.clear()
        curLapNum = -1
        lapInvalidFlag = false
        lastSampleDistance = -1000f
        lapInsights.clear()
        liveInsights = emptyList()
        lapAll = emptyList(); statusAll = emptyList(); drsAll = emptyList(); posAll = emptyList()
        bestLapByCar.fill(0L)
        trackBuckets.clear()
        trackPathCache = emptyList()
        rawCount = 0L
        lastSenderIp = ""
        _state.value = TelemetryState(source = source)
    }

    private var rawCount = 0L
    private var lastSenderIp = ""

    fun onRawPacket(buf: ByteArray, length: Int, senderIp: String? = null, nowMs: Long = System.currentTimeMillis()) {
        recorder?.write(buf, length)
        rawCount++
        if (senderIp != null) lastSenderIp = senderIp
        val parsed = PacketParser.parse(buf, length)
        if (parsed == null) {
            // Bytes are arriving even if we couldn't parse them — reflect that so
            // the user can tell a network problem from a format/parse problem.
            _state.value = _state.value.copy(
                connected = true, lastPacketAtMs = nowMs,
                datagramsReceived = rawCount, lastSenderIp = lastSenderIp,
            )
            return
        }
        apply(parsed, nowMs)
    }

    private fun apply(parsed: Parsed, nowMs: Long) {
        val prev = _state.value
        var next = prev.copy(
            connected = true,
            lastPacketAtMs = nowMs,
            packetsReceived = prev.packetsReceived + 1,
            datagramsReceived = rawCount,
            lastSenderIp = lastSenderIp,
            header = parsed.header,
            packetFormat = parsed.header.packetFormat,
            gameYear = parsed.header.gameYear,
        )

        when (parsed) {
            is Parsed.Telemetry -> {
                next = next.copy(telemetry = parsed.data)
                if (parsed.allDrs.isNotEmpty()) drsAll = parsed.allDrs
                handling.sample(parsed.data.steer, latestMotion.gForceLat, parsed.data.speedKmh)
            }
            is Parsed.Motion -> {
                latestMotion = parsed.data
                next = next.copy(motion = parsed.data)
                if (parsed.positions.isNotEmpty()) posAll = parsed.positions
                recordTrackPoint(parsed.data, next.lap.lapDistance)
                if (trackPathCache.isNotEmpty()) next = next.copy(trackPath = trackPathCache)
            }
            is Parsed.Status -> {
                next = next.copy(status = parsed.data)
                if (parsed.all.isNotEmpty()) statusAll = parsed.all
            }
            is Parsed.Damage -> next = next.copy(damage = parsed.data)
            is Parsed.Setup -> next = next.copy(setup = parsed.data)
            is Parsed.Session -> next = next.copy(session = parsed.info)
            is Parsed.Participants -> next = next.copy(participants = parsed.list)
            is Parsed.History -> {
                if (parsed.bestLapTimeMs > 0 && parsed.carIdx in 0 until F1Constants.MAX_CARS) {
                    bestLapByCar[parsed.carIdx] = parsed.bestLapTimeMs
                }
                if (parsed.carIdx == parsed.header.playerCarIndex && parsed.bestLapTimeMs > 0) {
                    next = next.copy(bestLapTimeMs = parsed.bestLapTimeMs)
                }
            }
            is Parsed.Event -> next = next.copy(events = pushEvent(prev.events, parsed.event))
            is Parsed.Lap -> {
                next = handleLap(next, parsed)
                if (parsed.all.isNotEmpty()) {
                    lapAll = parsed.all
                    next = buildStandings(next)
                }
            }
            is Parsed.Other -> {}
        }

        // Periodic live setup advice (once per second).
        if (nowMs - lastAdviceMs > 1000) {
            lastAdviceMs = nowMs
            liveInsights = SetupAdvisor.advise(next, handling.balance())
        }
        next = next.copy(insights = (liveInsights + lapInsights.asReversed()).take(40))
        _state.value = next
    }

    private fun handleLap(state: TelemetryState, parsed: Parsed.Lap): TelemetryState {
        val lap = parsed.data
        var next = state.copy(lap = lap)
        if (lap.currentLapInvalid) lapInvalidFlag = true

        // Lap boundary: finalise the completed lap.
        if (curLapNum >= 1 && lap.currentLapNum != curLapNum && working.size > 10) {
            val finished = LapTrace(
                lapNumber = curLapNum,
                samples = ArrayList(working),
                lapTimeMs = lap.lastLapTimeMs,
                valid = !lapInvalidFlag,
            )
            next = next.copy(lastTrace = finished)
            val newBest = state.bestTrace == null ||
                (finished.valid && finished.lapTimeMs in 1 until (state.bestTrace?.lapTimeMs ?: Long.MAX_VALUE))
            if (newBest && finished.valid) next = next.copy(bestTrace = finished)

            val insights = MistakeDetector.analyseLap(finished, next.bestTrace)
            lapInsights.addAll(insights)
            while (lapInsights.size > 30) lapInsights.removeAt(0)

            working.clear()
            lastSampleDistance = -1000f
            lapInvalidFlag = lap.currentLapInvalid
        }
        curLapNum = lap.currentLapNum

        // Distance-based sampling of the current lap.
        val t = state.telemetry
        if (lap.lapDistance >= 0 && lap.lapDistance - lastSampleDistance >= 5f) {
            lastSampleDistance = lap.lapDistance
            working.add(
                TraceSample(
                    distance = lap.lapDistance,
                    timeMs = lap.currentLapTimeMs,
                    speed = t.speedKmh,
                    throttle = t.throttle,
                    brake = t.brake,
                    steer = t.steer,
                    gear = t.gear,
                    gLat = latestMotion.gForceLat,
                    gLon = latestMotion.gForceLon,
                    rpm = t.engineRpm,
                )
            )
        }

        // Publish a live snapshot of the current lap trace (throttled).
        val nowMs = state.lastPacketAtMs
        if (nowMs - lastTracePublishMs > 120 && working.isNotEmpty()) {
            lastTracePublishMs = nowMs
            next = next.copy(
                currentTrace = LapTrace(lap.currentLapNum, ArrayList(working), lap.currentLapTimeMs, !lapInvalidFlag)
            )
        }
        return next
    }

    private fun pushEvent(list: List<TelemetryEvent>, e: TelemetryEvent): List<TelemetryEvent> {
        val out = ArrayList<TelemetryEvent>(list.size + 1)
        out.add(e)
        out.addAll(list)
        while (out.size > 30) out.removeAt(out.size - 1)
        return out
    }

    /** Accumulate the player's world path, keyed by lap distance, into a circuit outline. */
    private fun recordTrackPoint(motion: MotionData, lapDistance: Float) {
        if (lapDistance < 0f || (motion.worldPosX == 0f && motion.worldPosZ == 0f)) return
        val bucket = (lapDistance / 8f).toInt()
        val isNew = !trackBuckets.containsKey(bucket)
        trackBuckets[bucket] = motion.worldPosX to motion.worldPosZ
        if (isNew && trackBuckets.size > 8) {
            trackPathCache = trackBuckets.entries.sortedBy { it.key }.map { it.value }
        }
    }

    /** Merge the latest per-car lap/status/DRS/position data into the standings list. */
    private fun buildStandings(state: TelemetryState): TelemetryState {
        if (lapAll.isEmpty()) return state
        val playerIdx = state.header?.playerCarIndex ?: -1
        val list = ArrayList<LiveCar>(lapAll.size)
        var fastestMs = Long.MAX_VALUE
        var fastestIdx = -1
        for (lp in lapAll) {
            if (lp.position <= 0 && lp.resultStatus < 2) continue
            val st = statusAll.getOrNull(lp.index)
            val part = state.participants.getOrNull(lp.index)
            val name = part?.name?.takeIf { it.isNotBlank() } ?: "CAR ${lp.index + 1}"
            val best = bestLapByCar.getOrElse(lp.index) { 0L }
            if (best in 1 until fastestMs) { fastestMs = best; fastestIdx = lp.index }
            list.add(
                LiveCar(
                    index = lp.index,
                    position = lp.position,
                    name = name,
                    abbrev = abbrev(name, part?.teamId ?: 255, lp.index),
                    teamId = part?.teamId ?: 255,
                    lastLapMs = lp.lastLapMs,
                    bestLapMs = best,
                    visualTyre = st?.visualTyre ?: 0,
                    tyreAge = st?.tyreAge ?: 0,
                    ersPct = st?.ersPct ?: 0,
                    drsOpen = drsAll.getOrElse(lp.index) { false },
                    drsAllowed = (st?.drsAllowed ?: 0) == 1,
                    pitting = lp.pitStatus != 0,
                    resultStatus = lp.resultStatus,
                    penaltiesSec = lp.penaltiesSec,
                    deltaAheadMs = lp.deltaAheadMs,
                    deltaLeaderMs = lp.deltaLeaderMs,
                    lapDistance = lp.lapDistance,
                    worldX = posAll.getOrNull(lp.index)?.first ?: 0f,
                    worldZ = posAll.getOrNull(lp.index)?.second ?: 0f,
                    isPlayer = lp.index == playerIdx,
                )
            )
        }
        list.sortBy { it.position }
        return state.copy(
            cars = list,
            fastestLapCarIndex = fastestIdx,
            fastestLapMs = if (fastestMs == Long.MAX_VALUE) 0L else fastestMs,
        )
    }

    /** Three-letter driver code, from the real code table when known, else derived from the name. */
    private fun abbrev(name: String, teamId: Int, index: Int): String {
        val known = DriverCodes.forName(name)
        if (known != null) return known
        val token = name.trim().split(" ", "_", "-").lastOrNull { it.isNotBlank() } ?: name
        val letters = token.filter { it.isLetter() }
        return if (letters.length >= 3) letters.substring(0, 3).uppercase()
        else if (letters.isNotEmpty()) letters.uppercase().padEnd(3, 'X')
        else "C${index + 1}"
    }

    fun markDisconnectedIfStale(nowMs: Long) {
        val s = _state.value
        if (s.connected && nowMs - s.lastPacketAtMs > 3000) {
            _state.value = s.copy(connected = false)
        }
    }
}
