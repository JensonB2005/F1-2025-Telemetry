package com.f1telemetry.viewer.telemetry

/**
 * Maps a driver's name (as sent in the Participants packet) to the official
 * three-letter TV code. Falls back to null so the caller can derive one.
 */
object DriverCodes {
    private val bySurname = mapOf(
        "verstappen" to "VER", "hamilton" to "HAM", "leclerc" to "LEC", "norris" to "NOR",
        "piastri" to "PIA", "russell" to "RUS", "sainz" to "SAI", "alonso" to "ALO",
        "stroll" to "STR", "gasly" to "GAS", "ocon" to "OCO", "albon" to "ALB",
        "tsunoda" to "TSU", "hulkenberg" to "HUL", "hülkenberg" to "HUL", "bottas" to "BOT",
        "zhou" to "ZHO", "guanyu" to "ZHO", "magnussen" to "MAG", "ricciardo" to "RIC",
        "perez" to "PER", "pérez" to "PER", "bearman" to "BEA", "colapinto" to "COL",
        "lawson" to "LAW", "doohan" to "DOO", "antonelli" to "ANT", "bortoleto" to "BOR",
        "hadjar" to "HAD", "vettel" to "VET", "raikkonen" to "RAI", "räikkönen" to "RAI",
        "schumacher" to "MSC", "latifi" to "LAT", "mazepin" to "MAZ", "giovinazzi" to "GIO",
        "kubica" to "KUB", "aitken" to "AIT", "fittipaldi" to "FIT",
    )

    fun forName(name: String): String? {
        val tokens = name.trim().lowercase().split(" ", "_", "-").filter { it.isNotBlank() }
        for (t in tokens.asReversed()) {
            bySurname[t]?.let { return it }
        }
        return null
    }
}
