package com.f1telemetry.viewer.telemetry

/** Parsed 29-byte packet header shared by every packet. */
data class Header(
    val packetFormat: Int,
    val gameYear: Int,
    val gameMajorVersion: Int,
    val gameMinorVersion: Int,
    val packetVersion: Int,
    val packetId: Int,
    val sessionUID: Long,
    val sessionTime: Float,
    val frameIdentifier: Long,
    val overallFrameIdentifier: Long,
    val playerCarIndex: Int,
    val secondaryPlayerCarIndex: Int,
)

/** Player-car slice of the Car Telemetry packet (id 6). */
data class CarTelemetry(
    val speedKmh: Int = 0,
    val throttle: Float = 0f,
    val steer: Float = 0f,
    val brake: Float = 0f,
    val clutch: Int = 0,
    val gear: Int = 0,
    val engineRpm: Int = 0,
    val drs: Boolean = false,
    val revLightsPercent: Int = 0,
    val brakesTempC: IntArray = IntArray(4),
    val tyreSurfaceTempC: IntArray = IntArray(4),
    val tyreInnerTempC: IntArray = IntArray(4),
    val engineTempC: Int = 0,
    val tyrePressurePsi: FloatArray = FloatArray(4),
    val surfaceType: IntArray = IntArray(4),
    val suggestedGear: Int = 0,
)

/** Player-car slice of the Lap Data packet (id 2). */
data class LapData(
    val lastLapTimeMs: Long = 0,
    val currentLapTimeMs: Long = 0,
    val sector1Ms: Int = 0,
    val sector2Ms: Int = 0,
    val lapDistance: Float = 0f,
    val totalDistance: Float = 0f,
    val carPosition: Int = 0,
    val currentLapNum: Int = 0,
    val pitStatus: Int = 0,
    val numPitStops: Int = 0,
    val sector: Int = 0,
    val currentLapInvalid: Boolean = false,
    val penaltiesSec: Int = 0,
    val totalWarnings: Int = 0,
    val cornerCuttingWarnings: Int = 0,
    val gridPosition: Int = 0,
    val driverStatus: Int = 0,
    val deltaToLeaderMs: Int = 0,
    val deltaToCarAheadMs: Int = 0,
)

/** Player-car slice of the Car Status packet (id 7). */
data class CarStatus(
    val fuelInTank: Float = 0f,
    val fuelCapacity: Float = 0f,
    val fuelRemainingLaps: Float = 0f,
    val maxRpm: Int = 0,
    val idleRpm: Int = 0,
    val maxGears: Int = 0,
    val drsAllowed: Int = 0,
    val drsActivationDistance: Int = 0,
    val actualTyreCompound: Int = 0,
    val visualTyreCompound: Int = 0,
    val tyresAgeLaps: Int = 0,
    val fuelMix: Int = 0,
    val frontBrakeBias: Int = 0,
    val ersStoreEnergy: Float = 0f,
    val ersDeployMode: Int = 0,
    val ersHarvestedThisLapMGUK: Float = 0f,
    val ersHarvestedThisLapMGUH: Float = 0f,
    val ersDeployedThisLap: Float = 0f,
    val vehicleFiaFlags: Int = 0,
)

/** Player-car slice of the Car Damage packet (id 10). */
data class CarDamage(
    val tyreWearPct: FloatArray = FloatArray(4),
    val tyreDamagePct: IntArray = IntArray(4),
    val brakeDamagePct: IntArray = IntArray(4),
    val frontLeftWingDamage: Int = 0,
    val frontRightWingDamage: Int = 0,
    val rearWingDamage: Int = 0,
    val floorDamage: Int = 0,
    val diffuserDamage: Int = 0,
    val sidepodDamage: Int = 0,
    val drsFault: Boolean = false,
    val ersFault: Boolean = false,
    val gearboxDamage: Int = 0,
    val engineDamage: Int = 0,
)

/** Player-car slice of the Car Setups packet (id 5). */
data class CarSetup(
    val frontWing: Int = 0,
    val rearWing: Int = 0,
    val onThrottleDiff: Int = 0,
    val offThrottleDiff: Int = 0,
    val frontCamber: Float = 0f,
    val rearCamber: Float = 0f,
    val frontToe: Float = 0f,
    val rearToe: Float = 0f,
    val frontSuspension: Int = 0,
    val rearSuspension: Int = 0,
    val frontAntiRollBar: Int = 0,
    val rearAntiRollBar: Int = 0,
    val frontRideHeight: Int = 0,
    val rearRideHeight: Int = 0,
    val brakePressure: Int = 0,
    val brakeBias: Int = 0,
    val frontLeftPressure: Float = 0f,
    val frontRightPressure: Float = 0f,
    val rearLeftPressure: Float = 0f,
    val rearRightPressure: Float = 0f,
    val ballast: Int = 0,
    val fuelLoad: Float = 0f,
)

data class SessionInfo(
    val weather: Int = 0,
    val trackTempC: Int = 0,
    val airTempC: Int = 0,
    val totalLaps: Int = 0,
    val trackLengthM: Int = 0,
    val sessionType: Int = 0,
    val trackId: Int = -1,
    val sessionTimeLeft: Int = 0,
    val sessionDuration: Int = 0,
    val pitSpeedLimit: Int = 0,
    val safetyCarStatus: Int = 0,
    val networkGame: Boolean = false,
    val rainNowPct: Int = 0,
    val weather5: Int = 0,
    val rain5Pct: Int = 0,
    val weather10: Int = 0,
    val rain10Pct: Int = 0,
)

/** Player-car slice of the Motion packet (id 0), for g-force / attitude. */
data class MotionData(
    val gForceLat: Float = 0f,
    val gForceLon: Float = 0f,
    val gForceVert: Float = 0f,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val worldPosX: Float = 0f,
    val worldPosZ: Float = 0f,
)

data class ParticipantInfo(
    val teamId: Int = 255,
    val raceNumber: Int = 0,
    val name: String = "",
    val nationality: Int = 0,
)

data class TelemetryEvent(
    val code: String,
    val detail: String,
    val sessionTime: Float,
)

/** Lightweight per-car lap slice used to build the timing tower / track map. */
data class CarLapLite(
    val index: Int,
    val position: Int,
    val lastLapMs: Long,
    val currentLapMs: Long,
    val lapDistance: Float,
    val pitStatus: Int,
    val resultStatus: Int,
    val penaltiesSec: Int,
    val deltaAheadMs: Int,
    val deltaLeaderMs: Int,
    val currentLapNum: Int,
)

/** Lightweight per-car status slice (tyre / ERS / DRS-allowed). */
data class CarStatusLite(
    val index: Int,
    val visualTyre: Int,
    val actualTyre: Int,
    val tyreAge: Int,
    val ersPct: Int,
    val drsAllowed: Int,
)
