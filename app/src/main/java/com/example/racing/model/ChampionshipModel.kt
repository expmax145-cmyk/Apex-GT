package com.example.racing.model

enum class ChampionshipTier(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val entryFee: Int,
    val requiredLevel: Int,
    val requiredCup: String?,
    val trophyName: String,
    val rounds: List<TrackId>,
    val creditReward: Int,
    val xpReward: Int,
    val eligibleCarIds: List<String>
) {
    BRONZE(
        id = "BRONZE",
        title = "Bronze Cup",
        subtitle = "Clubman Point-to-Point Sprint Series",
        description = "Entry-tier 3-stage regional sprint tournament testing vehicle control across rolling countryside and coastal flats.",
        entryFee = 0,
        requiredLevel = 1,
        requiredCup = null,
        trophyName = "Apex Bronze Cup Trophy",
        rounds = listOf(
            TrackId.COASTAL_SPRINT,
            TrackId.DESERT_RUN,
            TrackId.COUNTRYSIDE_RUN
        ),
        creditReward = 15000,
        xpReward = 1500,
        eligibleCarIds = listOf("apex_valkyrie", "apex_spirit_86", "apex_monza_v10")
    ),
    SILVER(
        id = "SILVER",
        title = "Silver Cup",
        subtitle = "Pro-Am GT Challenge Tour",
        description = "Challenging 4-stage championship traversing technical forest switchbacks, industrial complexes, and coastal circuits.",
        entryFee = 5000,
        requiredLevel = 5,
        requiredCup = "BRONZE",
        trophyName = "Apex Silver Cup Trophy",
        rounds = listOf(
            TrackId.GRAND_PRIX,
            TrackId.FOREST_DRIVE,
            TrackId.INDUSTRIAL_ROUTE,
            TrackId.ALPINE_PASS
        ),
        creditReward = 40000,
        xpReward = 4000,
        eligibleCarIds = listOf("apex_valkyrie", "apex_spirit_86", "apex_monza_v10")
    ),
    GOLD(
        id = "GOLD",
        title = "Gold Cup",
        subtitle = "Apex World Grand Championship",
        description = "The premier 5-stage motorsport gauntlet across severe elevation canyons, nocturnal rain-slicked metropolises, and mountain passes.",
        entryFee = 15000,
        requiredLevel = 10,
        requiredCup = "SILVER",
        trophyName = "Apex Gold Cup Trophy",
        rounds = listOf(
            TrackId.ALPINE_PASS,
            TrackId.CANYON_CHALLENGE,
            TrackId.NEON_METROPOLIS,
            TrackId.COASTAL_SPRINT,
            TrackId.GRAND_PRIX
        ),
        creditReward = 100000,
        xpReward = 10000,
        eligibleCarIds = listOf("apex_valkyrie", "apex_monza_v10")
    );

    companion object {
        fun fromId(id: String): ChampionshipTier? {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
        }

        /**
         * Official FIA-style points system for Apex GT tournaments.
         */
        fun getPointsForPosition(position: Int): Int {
            return when (position) {
                1 -> 25
                2 -> 18
                3 -> 15
                4 -> 12
                else -> 10
            }
        }
    }
}

data class ChampionshipDriverStanding(
    val driverName: String,
    val carName: String,
    val carId: String,
    var points: Int = 0,
    var wins: Int = 0,
    var podiums: Int = 0,
    var bestFinish: Int = 4,
    val isPlayer: Boolean = false
)

data class ActiveChampionshipState(
    val tier: ChampionshipTier,
    var currentRoundIndex: Int = 0,
    val standings: MutableList<ChampionshipDriverStanding>,
    var isCurrentRoundFinished: Boolean = false,
    var isCupCompleted: Boolean = false,
    var finalRewardsClaimed: Boolean = false,
    val completedRoundsCount: Int = 0
) {
    val totalRounds: Int get() = tier.rounds.size
    val currentTrack: TrackId get() = tier.rounds[currentRoundIndex.coerceIn(0, totalRounds - 1)]
    val isLastRound: Boolean get() = currentRoundIndex >= totalRounds - 1

    fun getSortedStandings(): List<ChampionshipDriverStanding> {
        return standings.sortedWith(
            compareByDescending<ChampionshipDriverStanding> { it.points }
                .thenByDescending { it.wins }
                .thenByDescending { it.podiums }
                .thenBy { it.bestFinish }
                .thenBy { if (it.isPlayer) 0 else 1 }
        )
    }

    fun getPlayerPosition(): Int {
        val sorted = getSortedStandings()
        val index = sorted.indexOfFirst { it.isPlayer }
        return if (index >= 0) index + 1 else 1
    }
}
