package com.f1telemetry.viewer.telemetry

/**
 * Lookup tables and enumerations from the F1 25 UDP appendix. Values that the
 * game may extend year to year fall back to a readable "Unknown (id)" string
 * rather than crashing.
 */
object F1Constants {

    const val PACKET_MOTION = 0
    const val PACKET_SESSION = 1
    const val PACKET_LAP_DATA = 2
    const val PACKET_EVENT = 3
    const val PACKET_PARTICIPANTS = 4
    const val PACKET_CAR_SETUPS = 5
    const val PACKET_CAR_TELEMETRY = 6
    const val PACKET_CAR_STATUS = 7
    const val PACKET_FINAL_CLASSIFICATION = 8
    const val PACKET_LOBBY_INFO = 9
    const val PACKET_CAR_DAMAGE = 10
    const val PACKET_SESSION_HISTORY = 11
    const val PACKET_TYRE_SETS = 12
    const val PACKET_MOTION_EX = 13
    const val PACKET_TIME_TRIAL = 14
    const val PACKET_LAP_POSITIONS = 15

    const val HEADER_SIZE = 29
    const val MAX_CARS = 22

    fun team(id: Int): String = when (id) {
        0 -> "Mercedes"; 1 -> "Ferrari"; 2 -> "Red Bull Racing"; 3 -> "Williams"
        4 -> "Aston Martin"; 5 -> "Alpine"; 6 -> "RB"; 7 -> "Haas"
        8 -> "McLaren"; 9 -> "Sauber"
        41 -> "F1 Generic"; 104 -> "F1 Custom Team"
        255 -> "—"
        else -> "Team $id"
    }

    /** Team colour as 0xAARRGGBB for the timing tower and track-map dots. */
    fun teamColor(id: Int): Long = when (id) {
        0 -> 0xFF00D2BE  // Mercedes
        1 -> 0xFFDC0000  // Ferrari
        2 -> 0xFF3671C6  // Red Bull
        3 -> 0xFF64C4FF  // Williams
        4 -> 0xFF229971  // Aston Martin
        5 -> 0xFF0093CC  // Alpine
        6 -> 0xFF6692FF  // RB
        7 -> 0xFFB6BABD  // Haas
        8 -> 0xFFFF8000  // McLaren
        9 -> 0xFF52E252  // Sauber
        else -> 0xFF9A9AAE
    }

    /** Single-letter tyre code for the tower (S/M/H/I/W). */
    fun tyreLetter(visualId: Int): String = when (visualId) {
        16, 20 -> "S"; 17, 21 -> "M"; 18, 22 -> "H"; 19 -> "S"
        7 -> "I"; 8, 15 -> "W"
        else -> "-"
    }

    fun tyreColor(visualId: Int): Long = when (visualId) {
        16, 19, 20 -> 0xFFE10600  // soft - red
        17, 21 -> 0xFFFFD400      // medium - yellow
        18, 22 -> 0xFFEDEDED      // hard - white
        7 -> 0xFF39E75F           // intermediate - green
        8, 15 -> 0xFF3671C6       // wet - blue
        else -> 0xFF9A9AAE
    }

    fun tyreCompound(id: Int): String = when (id) {
        16 -> "C5"; 17 -> "C4"; 18 -> "C3"; 19 -> "C2"; 20 -> "C1"; 21 -> "C0"; 22 -> "C6"
        7 -> "Inter"; 8 -> "Wet"
        9 -> "Dry (Classic)"; 10 -> "Wet (Classic)"
        11 -> "SuperSoft"; 12 -> "Soft"; 13 -> "Medium"; 14 -> "Hard"; 15 -> "Wet (F2)"
        else -> "?"
    }

    fun visualTyre(id: Int): String = when (id) {
        16 -> "Soft"; 17 -> "Medium"; 18 -> "Hard"; 7 -> "Inter"; 8 -> "Wet"
        15 -> "Wet"; 19 -> "SuperSoft"; 20 -> "Soft"; 21 -> "Medium"; 22 -> "Hard"
        else -> "—"
    }

    fun surface(id: Int): String = when (id) {
        0 -> "Tarmac"; 1 -> "Rumble strip"; 2 -> "Concrete"; 3 -> "Rock"; 4 -> "Gravel"
        5 -> "Mud"; 6 -> "Sand"; 7 -> "Grass"; 8 -> "Water"; 9 -> "Cobblestone"
        10 -> "Metal"; 11 -> "Ridged"
        else -> "?"
    }

    fun track(id: Int): String = when (id) {
        0 -> "Melbourne"; 1 -> "Paul Ricard"; 2 -> "Shanghai"; 3 -> "Sakhir (Bahrain)"
        4 -> "Catalunya"; 5 -> "Monaco"; 6 -> "Montreal"; 7 -> "Silverstone"
        8 -> "Hockenheim"; 9 -> "Hungaroring"; 10 -> "Spa"; 11 -> "Monza"
        12 -> "Singapore"; 13 -> "Suzuka"; 14 -> "Abu Dhabi"; 15 -> "Texas (COTA)"
        16 -> "Interlagos"; 17 -> "Austria"; 18 -> "Sochi"; 19 -> "Mexico"
        20 -> "Baku"; 21 -> "Sakhir Short"; 22 -> "Silverstone Short"
        23 -> "Texas Short"; 24 -> "Suzuka Short"; 25 -> "Hanoi"; 26 -> "Zandvoort"
        27 -> "Imola"; 28 -> "Portimão"; 29 -> "Jeddah"; 30 -> "Miami"
        31 -> "Las Vegas"; 32 -> "Losail (Qatar)"
        else -> "Track $id"
    }

    fun sessionType(id: Int): String = when (id) {
        0 -> "Unknown"; 1 -> "Practice 1"; 2 -> "Practice 2"; 3 -> "Practice 3"
        4 -> "Short Practice"; 5 -> "Qualifying 1"; 6 -> "Qualifying 2"; 7 -> "Qualifying 3"
        8 -> "Short Qualifying"; 9 -> "One-Shot Qualifying"
        10 -> "Sprint Shootout 1"; 11 -> "Sprint Shootout 2"; 12 -> "Sprint Shootout 3"
        13 -> "Short Sprint Shootout"; 14 -> "One-Shot Sprint Shootout"
        15 -> "Race"; 16 -> "Race 2"; 17 -> "Race 3"; 18 -> "Time Trial"
        else -> "Session $id"
    }

    fun weather(id: Int): String = when (id) {
        0 -> "Clear"; 1 -> "Light Cloud"; 2 -> "Overcast"; 3 -> "Light Rain"
        4 -> "Heavy Rain"; 5 -> "Storm"
        else -> "?"
    }

    fun ersMode(id: Int): String = when (id) {
        0 -> "None"; 1 -> "Medium"; 2 -> "Hotlap"; 3 -> "Overtake"
        else -> "?"
    }

    fun fuelMix(id: Int): String = when (id) {
        0 -> "Lean"; 1 -> "Standard"; 2 -> "Rich"; 3 -> "Max"
        else -> "?"
    }

    fun drsFault(allowed: Int): String = when (allowed) {
        0 -> "Not allowed"; 1 -> "Allowed"; else -> "Unknown"
    }

    fun pitStatus(id: Int): String = when (id) {
        0 -> "On track"; 1 -> "Pitting"; 2 -> "In pit area"; else -> "?"
    }

    fun driverStatus(id: Int): String = when (id) {
        0 -> "In garage"; 1 -> "Flying lap"; 2 -> "In lap"; 3 -> "Out lap"; 4 -> "On track"
        else -> "?"
    }

    fun sector(id: Int): Int = id + 1

    /** Two-letter event string codes carried by the Event packet. */
    fun eventName(code: String): String = when (code) {
        "SSTA" -> "Session started"; "SEND" -> "Session ended"
        "FTLP" -> "Fastest lap"; "RTMT" -> "Retirement"
        "DRSE" -> "DRS enabled"; "DRSD" -> "DRS disabled"
        "TMPT" -> "Teammate in pits"; "CHQF" -> "Chequered flag"
        "RCWN" -> "Race winner"; "PENA" -> "Penalty issued"
        "SPTP" -> "Speed trap"; "STLG" -> "Start lights"
        "LGOT" -> "Lights out"; "DTSV" -> "Drive-through served"
        "SGSV" -> "Stop-go served"; "FLBK" -> "Flashback"
        "BUTN" -> "Button status"; "RDFL" -> "Red flag"
        "OVTK" -> "Overtake"; "SCAR" -> "Safety car"
        "COLL" -> "Collision"
        else -> code
    }

    fun penaltyType(id: Int): String = when (id) {
        0 -> "Drive-through"; 1 -> "Stop-Go"; 2 -> "Grid penalty"; 3 -> "Penalty reminder"
        4 -> "Time penalty"; 5 -> "Warning"; 6 -> "Disqualified"; 7 -> "Removed from formation"
        8 -> "Parked too long"; 9 -> "Tyre regulations"; 10 -> "This lap invalidated"
        11 -> "This & next lap invalidated"; 12 -> "This lap invalidated (no reason)"
        13 -> "This & next invalidated (no reason)"; 14 -> "This & previous invalidated"
        15 -> "This & previous invalidated (no reason)"; 16 -> "Retired"; 17 -> "Black flag timer"
        else -> "Penalty $id"
    }

    fun infringement(id: Int): String = when (id) {
        0 -> "Blocking by slow driving"; 1 -> "Blocking by wrong way"
        2 -> "Reversing off the start line"; 3 -> "Big collision"; 4 -> "Small collision"
        5 -> "Collision failed to hand back position single"; 6 -> "Collision failed to hand back position multiple"
        7 -> "Corner cutting gained time"; 8 -> "Corner cutting overtake single"
        9 -> "Corner cutting overtake multiple"; 10 -> "Crossed pit exit lane"
        11 -> "Ignoring blue flags"; 12 -> "Ignoring yellow flags"; 13 -> "Ignoring drive through"
        14 -> "Too many drive throughs"; 15 -> "Drive through reminder serve within N laps"
        16 -> "Drive through reminder serve this lap"; 17 -> "Pit lane speeding"
        18 -> "Parked for too long"; 19 -> "Ignoring tyre regulations"
        20 -> "Too many penalties"; 21 -> "Multiple warnings"; 22 -> "Approaching disqualification"
        23 -> "Tyre regulations select single"; 24 -> "Tyre regulations select multiple"
        25 -> "Lap invalidated corner cutting"; 26 -> "Lap invalidated running wide"
        27 -> "Corner cutting ran wide gained time minor"; 28 -> "Corner cutting ran wide gained time significant"
        29 -> "Corner cutting ran wide gained time extreme"; 30 -> "Lap invalidated wall riding"
        31 -> "Lap invalidated flashback used"; 32 -> "Lap invalidated reset to track"
        33 -> "Blocking the pit lane"; 34 -> "Jump start"; 35 -> "Safety car to car collision"
        36 -> "Safety car illegal overtake"; 37 -> "Safety car exceeding allowed pace"
        38 -> "Virtual safety car exceeding allowed pace"; 39 -> "Formation lap below allowed speed"
        40 -> "Formation lap parking"; 41 -> "Retired mechanical failure"; 42 -> "Retired terminally damaged"
        43 -> "Safety car falling too far back"; 44 -> "Black flag timer"; 45 -> "Unserved stop go penalty"
        46 -> "Illegal time gain"; 47 -> "Excessive lateness"
        else -> "Infringement $id"
    }
}
