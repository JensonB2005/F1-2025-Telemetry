package com.f1telemetry.viewer.telemetry

/**
 * Parses F1 25 (packet format 2025, also compatible with 2022–2024) UDP packets.
 *
 * Design note: rather than hard-coding every struct size, per-car block sizes
 * are derived from the actual received packet length
 * ("(length - header - trailer) / 22"). Because EA extends these structs with
 * new trailing fields most years, deriving the stride keeps the leading fields
 * we care about (speed, throttle, brake, temps, ...) correctly aligned across
 * game versions. Fields near the tail of a block are addressed relative to the
 * end of the block for the same reason.
 */
sealed interface Parsed {
    val header: Header
    data class Motion(
        override val header: Header,
        val data: MotionData,
        val positions: List<Pair<Float, Float>> = emptyList(),
    ) : Parsed
    data class Session(override val header: Header, val info: SessionInfo) : Parsed
    data class Lap(
        override val header: Header,
        val data: LapData,
        val all: List<CarLapLite> = emptyList(),
    ) : Parsed
    data class Telemetry(
        override val header: Header,
        val data: CarTelemetry,
        val allDrs: List<Boolean> = emptyList(),
    ) : Parsed
    data class Status(
        override val header: Header,
        val data: CarStatus,
        val all: List<CarStatusLite> = emptyList(),
    ) : Parsed
    data class Damage(override val header: Header, val data: CarDamage) : Parsed
    data class Setup(override val header: Header, val data: CarSetup) : Parsed
    data class Participants(override val header: Header, val list: List<ParticipantInfo>) : Parsed
    data class Event(override val header: Header, val event: TelemetryEvent) : Parsed
    data class History(
        override val header: Header,
        val carIdx: Int,
        val bestLapTimeMs: Long,
        val bestLapNum: Int,
    ) : Parsed
    data class Other(override val header: Header) : Parsed
}

object PacketParser {

    fun parseHeader(r: ByteReader): Header = Header(
        packetFormat = r.u16(),
        gameYear = r.u8(),
        gameMajorVersion = r.u8(),
        gameMinorVersion = r.u8(),
        packetVersion = r.u8(),
        packetId = r.u8(),
        sessionUID = r.u64(),
        sessionTime = r.f32(),
        frameIdentifier = r.u32(),
        overallFrameIdentifier = r.u32(),
        playerCarIndex = r.u8(),
        secondaryPlayerCarIndex = r.u8(),
    )

    fun parse(buf: ByteArray, length: Int): Parsed? {
        if (length < F1Constants.HEADER_SIZE) return null
        val r = ByteReader(buf, length)
        val h = parseHeader(r)
        val p = h.playerCarIndex.coerceIn(0, F1Constants.MAX_CARS - 1)
        return when (h.packetId) {
            F1Constants.PACKET_MOTION -> Parsed.Motion(h, parseMotion(buf, length, p), parseMotionAll(buf, length))
            F1Constants.PACKET_SESSION -> Parsed.Session(h, parseSession(buf, length))
            F1Constants.PACKET_LAP_DATA -> Parsed.Lap(h, parseLap(buf, length, p), parseLapAll(buf, length))
            F1Constants.PACKET_EVENT -> Parsed.Event(h, parseEvent(buf, length, h))
            F1Constants.PACKET_PARTICIPANTS -> Parsed.Participants(h, parseParticipants(buf, length))
            F1Constants.PACKET_CAR_SETUPS -> Parsed.Setup(h, parseSetup(buf, length, p))
            F1Constants.PACKET_CAR_TELEMETRY -> Parsed.Telemetry(h, parseTelemetry(buf, length, p), parseDrsAll(buf, length))
            F1Constants.PACKET_CAR_STATUS -> Parsed.Status(h, parseStatus(buf, length, p), parseStatusAll(buf, length))
            F1Constants.PACKET_CAR_DAMAGE -> Parsed.Damage(h, parseDamage(buf, length, p))
            F1Constants.PACKET_SESSION_HISTORY -> parseHistory(buf, length, h)
            else -> Parsed.Other(h)
        }
    }

    private fun stride(length: Int, trailer: Int, prefix: Int = 0): Int {
        val body = length - F1Constants.HEADER_SIZE - trailer - prefix
        return if (body > 0) body / F1Constants.MAX_CARS else 0
    }

    private fun parseTelemetry(buf: ByteArray, len: Int, player: Int): CarTelemetry {
        val s = stride(len, trailer = 3)
        if (s < 60) return CarTelemetry()
        val base = F1Constants.HEADER_SIZE + player * s
        val r = ByteReader(buf, len)
        r.seek(base)
        val speed = r.u16()
        val throttle = r.f32()
        val steer = r.f32()
        val brake = r.f32()
        val clutch = r.u8()
        val gear = r.i8()
        val rpm = r.u16()
        val drs = r.u8()
        val revPct = r.u8()
        r.u16() // revLightsBitValue
        val brakesTemp = r.u16Array(4)
        val tyreSurf = r.u8Array(4)
        val tyreInner = r.u8Array(4)
        val engineTemp = r.u16()
        val pressures = r.f32Array(4)
        val surfaces = r.u8Array(4)
        // suggested gear is the final byte of the packet
        val suggested = if (len >= 1) buf[len - 1].toInt() else 0
        return CarTelemetry(
            speedKmh = speed, throttle = throttle, steer = steer, brake = brake,
            clutch = clutch, gear = gear, engineRpm = rpm, drs = drs == 1,
            revLightsPercent = revPct, brakesTempC = brakesTemp, tyreSurfaceTempC = tyreSurf,
            tyreInnerTempC = tyreInner, engineTempC = engineTemp, tyrePressurePsi = pressures,
            surfaceType = surfaces, suggestedGear = suggested,
        )
    }

    private fun parseLap(buf: ByteArray, len: Int, player: Int): LapData {
        val s = stride(len, trailer = 2)
        if (s < 43) return LapData()
        val base = F1Constants.HEADER_SIZE + player * s
        val r = ByteReader(buf, len)
        r.seek(base)
        val lastLap = r.u32()
        val curLap = r.u32()
        val s1ms = r.u16()
        val s1min = r.u8()
        val s2ms = r.u16()
        val s2min = r.u8()
        val deltaFrontMs = r.u16()
        val deltaFrontMin = r.u8()
        val deltaLeaderMs = r.u16()
        val deltaLeaderMin = r.u8()
        val lapDistance = r.f32()
        val totalDistance = r.f32()
        r.f32() // safety car delta
        val carPos = r.u8()
        val curLapNum = r.u8()
        val pitStatus = r.u8()
        val numPitStops = r.u8()
        val sector = r.u8()
        val lapInvalid = r.u8()
        val penalties = r.u8()
        val totalWarnings = r.u8()
        val cornerCutting = r.u8()
        r.u8() // unserved drive-through
        r.u8() // unserved stop-go
        val gridPos = r.u8()
        val driverStatus = r.u8()
        return LapData(
            lastLapTimeMs = lastLap,
            currentLapTimeMs = curLap,
            sector1Ms = s1min * 60000 + s1ms,
            sector2Ms = s2min * 60000 + s2ms,
            lapDistance = lapDistance,
            totalDistance = totalDistance,
            carPosition = carPos,
            currentLapNum = curLapNum,
            pitStatus = pitStatus,
            numPitStops = numPitStops,
            sector = sector,
            currentLapInvalid = lapInvalid == 1,
            penaltiesSec = penalties,
            totalWarnings = totalWarnings,
            cornerCuttingWarnings = cornerCutting,
            gridPosition = gridPos,
            driverStatus = driverStatus,
            deltaToLeaderMs = deltaLeaderMin * 60000 + deltaLeaderMs,
            deltaToCarAheadMs = deltaFrontMin * 60000 + deltaFrontMs,
        )
    }

    private fun parseStatus(buf: ByteArray, len: Int, player: Int): CarStatus {
        val s = stride(len, trailer = 0)
        if (s < 55) return CarStatus()
        val base = F1Constants.HEADER_SIZE + player * s
        val r = ByteReader(buf, len)
        r.seek(base)
        r.u8(); r.u8() // traction control, ABS
        val fuelMix = r.u8()
        val frontBrakeBias = r.u8()
        r.u8() // pit limiter
        val fuelInTank = r.f32()
        val fuelCapacity = r.f32()
        val fuelRemainingLaps = r.f32()
        val maxRpm = r.u16()
        val idleRpm = r.u16()
        val maxGears = r.u8()
        val drsAllowed = r.u8()
        val drsActivation = r.u16()
        val actualTyre = r.u8()
        val visualTyre = r.u8()
        val tyreAge = r.u8()
        val fiaFlags = r.i8()
        r.f32() // engine power ICE
        r.f32() // engine power MGU-K
        val ersStore = r.f32()
        val ersMode = r.u8()
        val ersHarvestK = r.f32()
        val ersHarvestH = r.f32()
        val ersDeployed = r.f32()
        return CarStatus(
            fuelInTank = fuelInTank, fuelCapacity = fuelCapacity, fuelRemainingLaps = fuelRemainingLaps,
            maxRpm = maxRpm, idleRpm = idleRpm, maxGears = maxGears, drsAllowed = drsAllowed,
            drsActivationDistance = drsActivation, actualTyreCompound = actualTyre,
            visualTyreCompound = visualTyre, tyresAgeLaps = tyreAge, fuelMix = fuelMix,
            frontBrakeBias = frontBrakeBias, ersStoreEnergy = ersStore, ersDeployMode = ersMode,
            ersHarvestedThisLapMGUK = ersHarvestK, ersHarvestedThisLapMGUH = ersHarvestH,
            ersDeployedThisLap = ersDeployed, vehicleFiaFlags = fiaFlags,
        )
    }

    private fun parseDamage(buf: ByteArray, len: Int, player: Int): CarDamage {
        val s = stride(len, trailer = 0)
        if (s < 42) return CarDamage()
        val base = F1Constants.HEADER_SIZE + player * s
        val r = ByteReader(buf, len)
        r.seek(base)
        val wear = r.f32Array(4)
        val tyreDmg = r.u8Array(4)
        val brakeDmg = r.u8Array(4)
        val flWing = r.u8()
        val frWing = r.u8()
        val rearWing = r.u8()
        val floor = r.u8()
        val diffuser = r.u8()
        val sidepod = r.u8()
        val drsFault = r.u8()
        val ersFault = r.u8()
        val gearbox = r.u8()
        val engine = r.u8()
        return CarDamage(
            tyreWearPct = wear, tyreDamagePct = tyreDmg, brakeDamagePct = brakeDmg,
            frontLeftWingDamage = flWing, frontRightWingDamage = frWing, rearWingDamage = rearWing,
            floorDamage = floor, diffuserDamage = diffuser, sidepodDamage = sidepod,
            drsFault = drsFault == 1, ersFault = ersFault == 1,
            gearboxDamage = gearbox, engineDamage = engine,
        )
    }

    private fun parseSetup(buf: ByteArray, len: Int, player: Int): CarSetup {
        val s = stride(len, trailer = 4) // trailer: nextFrontWingValue (f32)
        if (s < 49) return CarSetup()
        val base = F1Constants.HEADER_SIZE + player * s
        val r = ByteReader(buf, len)
        r.seek(base)
        val frontWing = r.u8()
        val rearWing = r.u8()
        val onThrottle = r.u8()
        val offThrottle = r.u8()
        val frontCamber = r.f32()
        val rearCamber = r.f32()
        val frontToe = r.f32()
        val rearToe = r.f32()
        val frontSusp = r.u8()
        val rearSusp = r.u8()
        val frontArb = r.u8()
        val rearArb = r.u8()
        val frontRide = r.u8()
        val rearRide = r.u8()
        val brakePressure = r.u8()
        val brakeBias = r.u8()
        // Tyre pressures, ballast and fuel sit at the tail of the block. Address
        // them relative to the block end so a mid-block field added in a newer
        // game version does not shift them.
        val tail = base + s
        val rl = ByteReader(buf, len).apply { seek(tail - 21) }.f32()
        val rr = ByteReader(buf, len).apply { seek(tail - 17) }.f32()
        val fl = ByteReader(buf, len).apply { seek(tail - 13) }.f32()
        val fr = ByteReader(buf, len).apply { seek(tail - 9) }.f32()
        val ballast = buf[tail - 5].toInt() and 0xFF
        val fuel = ByteReader(buf, len).apply { seek(tail - 4) }.f32()
        return CarSetup(
            frontWing = frontWing, rearWing = rearWing, onThrottleDiff = onThrottle,
            offThrottleDiff = offThrottle, frontCamber = frontCamber, rearCamber = rearCamber,
            frontToe = frontToe, rearToe = rearToe, frontSuspension = frontSusp,
            rearSuspension = rearSusp, frontAntiRollBar = frontArb, rearAntiRollBar = rearArb,
            frontRideHeight = frontRide, rearRideHeight = rearRide, brakePressure = brakePressure,
            brakeBias = brakeBias, frontLeftPressure = fl, frontRightPressure = fr,
            rearLeftPressure = rl, rearRightPressure = rr, ballast = ballast, fuelLoad = fuel,
        )
    }

    private fun parseMotion(buf: ByteArray, len: Int, player: Int): MotionData {
        val s = stride(len, trailer = 0)
        if (s < 60) return MotionData()
        val base = F1Constants.HEADER_SIZE + player * s
        val r = ByteReader(buf, len)
        r.seek(base)
        val wx = r.f32(); r.f32(); val wz = r.f32()
        r.f32(); r.f32(); r.f32() // velocities
        r.skip(12) // 6 x int16 direction vectors
        val gLat = r.f32()
        val gLon = r.f32()
        val gVert = r.f32()
        val yaw = r.f32()
        val pitch = r.f32()
        val roll = r.f32()
        return MotionData(
            gForceLat = gLat, gForceLon = gLon, gForceVert = gVert,
            yaw = yaw, pitch = pitch, roll = roll, worldPosX = wx, worldPosZ = wz,
        )
    }

    /** World (x, z) position of every car, for the live track map. */
    private fun parseMotionAll(buf: ByteArray, len: Int): List<Pair<Float, Float>> {
        val s = stride(len, trailer = 0)
        if (s < 60) return emptyList()
        val out = ArrayList<Pair<Float, Float>>(F1Constants.MAX_CARS)
        for (i in 0 until F1Constants.MAX_CARS) {
            val r = ByteReader(buf, len)
            r.seek(F1Constants.HEADER_SIZE + i * s)
            val x = r.f32()
            r.f32() // worldPosY
            val z = r.f32()
            out.add(x to z)
        }
        return out
    }

    /** Per-car lap slice for every car (timing tower). */
    private fun parseLapAll(buf: ByteArray, len: Int): List<CarLapLite> {
        val s = stride(len, trailer = 2)
        if (s < 46) return emptyList()
        val out = ArrayList<CarLapLite>(F1Constants.MAX_CARS)
        for (i in 0 until F1Constants.MAX_CARS) {
            val r = ByteReader(buf, len)
            r.seek(F1Constants.HEADER_SIZE + i * s)
            val lastLap = r.u32()
            val curLap = r.u32()
            r.u16(); r.u8(); r.u16(); r.u8() // sector 1/2 parts
            val deltaFrontMs = r.u16(); val deltaFrontMin = r.u8()
            val deltaLeaderMs = r.u16(); val deltaLeaderMin = r.u8()
            val lapDistance = r.f32()
            r.f32(); r.f32() // total distance, safety car delta
            val carPos = r.u8()
            val curLapNum = r.u8()
            val pitStatus = r.u8()
            r.u8() // num pit stops
            r.u8() // sector
            r.u8() // current lap invalid
            val penalties = r.u8()
            r.u8(); r.u8() // total warnings, corner-cutting warnings
            r.u8(); r.u8() // unserved drive-through, stop-go
            r.u8() // grid position
            r.u8() // driver status
            val resultStatus = r.u8()
            out.add(
                CarLapLite(
                    index = i, position = carPos, lastLapMs = lastLap, currentLapMs = curLap,
                    lapDistance = lapDistance, pitStatus = pitStatus, resultStatus = resultStatus,
                    penaltiesSec = penalties,
                    deltaAheadMs = deltaFrontMin * 60000 + deltaFrontMs,
                    deltaLeaderMs = deltaLeaderMin * 60000 + deltaLeaderMs,
                    currentLapNum = curLapNum,
                )
            )
        }
        return out
    }

    /** Per-car tyre / ERS / DRS-allowed slice for every car. */
    private fun parseStatusAll(buf: ByteArray, len: Int): List<CarStatusLite> {
        val s = stride(len, trailer = 0)
        if (s < 55) return emptyList()
        val out = ArrayList<CarStatusLite>(F1Constants.MAX_CARS)
        for (i in 0 until F1Constants.MAX_CARS) {
            val r = ByteReader(buf, len)
            r.seek(F1Constants.HEADER_SIZE + i * s)
            r.u8(); r.u8() // traction control, ABS
            r.u8() // fuel mix
            r.u8() // front brake bias
            r.u8() // pit limiter
            r.f32(); r.f32(); r.f32() // fuel in tank / capacity / remaining laps
            r.u16(); r.u16() // max rpm, idle rpm
            r.u8() // max gears
            val drsAllowed = r.u8()
            r.u16() // drs activation distance
            val actualTyre = r.u8()
            val visualTyre = r.u8()
            val tyreAge = r.u8()
            r.i8() // fia flags
            r.f32(); r.f32() // engine power ICE / MGU-K
            val ersStore = r.f32()
            val ersPct = (ersStore / 40000f).coerceIn(0f, 100f).toInt()
            out.add(CarStatusLite(i, visualTyre, actualTyre, tyreAge, ersPct, drsAllowed))
        }
        return out
    }

    /** DRS open/closed flag for every car. */
    private fun parseDrsAll(buf: ByteArray, len: Int): List<Boolean> {
        val s = stride(len, trailer = 3)
        if (s < 60) return emptyList()
        val out = ArrayList<Boolean>(F1Constants.MAX_CARS)
        for (i in 0 until F1Constants.MAX_CARS) {
            val drs = buf[F1Constants.HEADER_SIZE + i * s + 18].toInt() and 0xFF
            out.add(drs == 1)
        }
        return out
    }

    private fun parseSession(buf: ByteArray, len: Int): SessionInfo {
        val r = ByteReader(buf, len)
        r.seek(F1Constants.HEADER_SIZE)
        if (r.remaining() < 15) return SessionInfo()
        val weather = r.u8()
        val trackTemp = r.i8()
        val airTemp = r.i8()
        val totalLaps = r.u8()
        val trackLength = r.u16()
        val sessionType = r.u8()
        val trackId = r.i8()
        r.u8() // formula
        val timeLeft = r.u16()
        val duration = r.u16()
        val pitLimit = r.u8()
        return SessionInfo(
            weather = weather, trackTempC = trackTemp, airTempC = airTemp,
            totalLaps = totalLaps, trackLengthM = trackLength, sessionType = sessionType,
            trackId = trackId, sessionTimeLeft = timeLeft, sessionDuration = duration,
            pitSpeedLimit = pitLimit,
        )
    }

    private fun parseParticipants(buf: ByteArray, len: Int): List<ParticipantInfo> {
        val prefix = 1 // numActiveCars
        val s = stride(len, trailer = 0, prefix = prefix)
        if (s < 24) return emptyList()
        val r = ByteReader(buf, len)
        r.seek(F1Constants.HEADER_SIZE)
        val numActive = r.u8().coerceIn(0, F1Constants.MAX_CARS)
        val out = ArrayList<ParticipantInfo>(numActive)
        val arrayBase = F1Constants.HEADER_SIZE + prefix
        for (i in 0 until numActive) {
            val cr = ByteReader(buf, len)
            cr.seek(arrayBase + i * s)
            cr.u8() // aiControlled
            cr.u8() // driverId
            cr.u8() // networkId
            val teamId = cr.u8()
            cr.u8() // myTeam
            val raceNumber = cr.u8()
            val nationality = cr.u8()
            val nameLen = minOf(48, s - 7)
            val name = cr.str(nameLen)
            out.add(ParticipantInfo(teamId, raceNumber, name, nationality))
        }
        return out
    }

    private fun parseEvent(buf: ByteArray, len: Int, h: Header): TelemetryEvent {
        val r = ByteReader(buf, len)
        r.seek(F1Constants.HEADER_SIZE)
        val code = r.str(4)
        val detail = when (code) {
            "PENA" -> {
                val penType = r.u8(); val infr = r.u8()
                "${F1Constants.penaltyType(penType)} — ${F1Constants.infringement(infr)}"
            }
            "SPTP" -> {
                r.u8() // vehicleIdx
                val speed = r.f32()
                "%.1f km/h".format(speed)
            }
            "FTLP" -> {
                r.u8() // vehicleIdx
                val t = r.f32()
                "Lap %.3fs".format(t)
            }
            "OVTK" -> "Overtake"
            "RTMT" -> "Retirement"
            "SCAR" -> {
                val scType = r.u8()
                when (scType) { 0 -> "No safety car"; 1 -> "Full safety car"; 2 -> "Virtual safety car"; 3 -> "Formation lap"; else -> "Safety car" }
            }
            else -> ""
        }
        return TelemetryEvent(code, detail, h.sessionTime)
    }

    private fun parseHistory(buf: ByteArray, len: Int, h: Header): Parsed {
        val r = ByteReader(buf, len)
        r.seek(F1Constants.HEADER_SIZE)
        if (r.remaining() < 7) return Parsed.Other(h)
        val carIdx = r.u8()
        r.u8() // numLaps
        r.u8() // numTyreStints
        val bestLapNum = r.u8()
        r.u8(); r.u8(); r.u8() // best sector lap nums
        var bestLapMs = 0L
        if (bestLapNum in 1..100) {
            val lapBase = F1Constants.HEADER_SIZE + 7 + (bestLapNum - 1) * 14
            if (lapBase + 4 <= len) {
                val lr = ByteReader(buf, len)
                lr.seek(lapBase)
                bestLapMs = lr.u32()
            }
        }
        return Parsed.History(h, carIdx, bestLapMs, bestLapNum)
    }
}
