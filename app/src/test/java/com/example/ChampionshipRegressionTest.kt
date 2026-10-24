package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.racing.data.GameDataManager
import com.example.racing.model.ActiveChampionshipState
import com.example.racing.model.CarDefinition
import com.example.racing.model.CarUpgradeState
import com.example.racing.model.ChampionshipDriverStanding
import com.example.racing.model.ChampionshipTier
import com.example.racing.model.LeaderboardRacer
import com.example.racing.model.RaceMode
import com.example.racing.model.TrackId
import com.example.racing.ui.GameScreen
import com.example.racing.ui.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChampionshipRegressionTest {

    private lateinit var app: Application
    private lateinit var dataManager: GameDataManager
    private lateinit var viewModel: GameViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext<Application>()
        dataManager = GameDataManager(app)
        // Reset championship persistent fields for clean test runs
        dataManager.clearActiveChampionship()
        dataManager.unlockedCups = setOf("BRONZE")
        dataManager.completedCups = emptySet()
        dataManager.trophiesEarned = emptySet()
        dataManager.careerChampionshipPoints = 0
        dataManager.championshipEntries = 0
        dataManager.championshipWins = 0
        dataManager.credits = 50000
        dataManager.playerLevel = 14
        dataManager.selectedCarId = "apex_valkyrie"

        viewModel = GameViewModel(app)
    }

    // 1. Bronze, Silver, and Gold Cup eligibility
    @Test
    fun test01_cupEligibilityCriteria() {
        assertEquals("Bronze Cup entry fee is 0", 0, ChampionshipTier.BRONZE.entryFee)
        assertEquals("Bronze requires Level 1", 1, ChampionshipTier.BRONZE.requiredLevel)
        assertNull("Bronze has no previous cup prerequisite", ChampionshipTier.BRONZE.requiredCup)

        assertEquals("Silver Cup entry fee is 5,000", 5000, ChampionshipTier.SILVER.entryFee)
        assertEquals("Silver requires Level 5", 5, ChampionshipTier.SILVER.requiredLevel)
        assertEquals("Silver requires BRONZE cup completion", "BRONZE", ChampionshipTier.SILVER.requiredCup)

        assertEquals("Gold Cup entry fee is 15,000", 15000, ChampionshipTier.GOLD.entryFee)
        assertEquals("Gold requires Level 10", 10, ChampionshipTier.GOLD.requiredLevel)
        assertEquals("Gold requires SILVER cup completion", "SILVER", ChampionshipTier.GOLD.requiredCup)
    }

    // 2. Locked-cup requirements and unlocking
    @Test
    fun test02_lockedCupRequirementsAndUnlocking() {
        // Initially Bronze is unlocked
        val (bronzeUnlocked, _) = viewModel.checkCupUnlockStatus(ChampionshipTier.BRONZE)
        assertTrue("Bronze Cup must be unlocked by default", bronzeUnlocked)

        // Silver is initially locked because Bronze has not been completed
        val (silverUnlockedBefore, silverReason) = viewModel.checkCupUnlockStatus(ChampionshipTier.SILVER)
        assertFalse("Silver Cup must be locked before Bronze completion", silverUnlockedBefore)
        assertTrue("Silver lock message must mention BRONZE", silverReason.contains("BRONZE", ignoreCase = true))

        // Complete Bronze Cup
        dataManager.recordCupCompletion(ChampionshipTier.BRONZE, playerWon = true)

        val (silverUnlockedAfter, _) = viewModel.checkCupUnlockStatus(ChampionshipTier.SILVER)
        assertTrue("Silver Cup must be unlocked after winning Bronze Cup", silverUnlockedAfter)

        // Gold is still locked
        val (goldUnlockedBefore, _) = viewModel.checkCupUnlockStatus(ChampionshipTier.GOLD)
        assertFalse("Gold Cup must remain locked until Silver Cup completion", goldUnlockedBefore)

        // Complete Silver Cup
        dataManager.recordCupCompletion(ChampionshipTier.SILVER, playerWon = true)
        val (goldUnlockedAfter, _) = viewModel.checkCupUnlockStatus(ChampionshipTier.GOLD)
        assertTrue("Gold Cup must unlock after winning Silver Cup", goldUnlockedAfter)
    }

    // 3. Vehicle eligibility validation
    @Test
    fun test03_vehicleEligibilityValidation() {
        val valkyrie = CarDefinition.ALL_CARS.first { it.id == "apex_valkyrie" }
        val spirit = CarDefinition.ALL_CARS.first { it.id == "apex_spirit_86" }
        val monza = CarDefinition.ALL_CARS.first { it.id == "apex_monza_v10" }

        // Bronze allows all cars
        assertTrue(viewModel.checkVehicleEligibility(ChampionshipTier.BRONZE, valkyrie).first)
        assertTrue(viewModel.checkVehicleEligibility(ChampionshipTier.BRONZE, spirit).first)
        assertTrue(viewModel.checkVehicleEligibility(ChampionshipTier.BRONZE, monza).first)

        // Gold allows only Hypercars/Supercars (Valkyrie, Monza)
        assertTrue(viewModel.checkVehicleEligibility(ChampionshipTier.GOLD, valkyrie).first)
        assertTrue(viewModel.checkVehicleEligibility(ChampionshipTier.GOLD, monza).first)
        assertFalse("Gold Cup must not allow low-class tuner coupe without upgrades", viewModel.checkVehicleEligibility(ChampionshipTier.GOLD, spirit).first)
    }

    // 4. Advancing between rounds
    @Test
    fun test04_advancingBetweenRounds() {
        val started = viewModel.startChampionship(ChampionshipTier.BRONZE)
        assertTrue("Championship must start successfully", started)

        val active = viewModel.activeChampionship.value
        assertNotNull(active)
        assertEquals(0, active!!.currentRoundIndex)
        assertEquals(ChampionshipTier.BRONZE.rounds[0], active.currentTrack)
        assertFalse(active.isLastRound)

        // Advance to round 2
        val advanced = viewModel.advanceToNextChampionshipRound()
        assertTrue("Must advance to round 2", advanced)
        assertEquals(1, active.currentRoundIndex)
        assertEquals(ChampionshipTier.BRONZE.rounds[1], active.currentTrack)
        assertFalse(active.isLastRound)

        // Advance to round 3 (last round)
        val advancedLast = viewModel.advanceToNextChampionshipRound()
        assertTrue("Must advance to round 3", advancedLast)
        assertEquals(2, active.currentRoundIndex)
        assertEquals(ChampionshipTier.BRONZE.rounds[2], active.currentTrack)
        assertTrue("Round 3 is the final round of Bronze Cup", active.isLastRound)
    }

    // 5. Championship point calculations
    @Test
    fun test05_championshipPointCalculations() {
        assertEquals("1st place must be awarded 25 pts", 25, ChampionshipTier.getPointsForPosition(1))
        assertEquals("2nd place must be awarded 18 pts", 18, ChampionshipTier.getPointsForPosition(2))
        assertEquals("3rd place must be awarded 15 pts", 15, ChampionshipTier.getPointsForPosition(3))
        assertEquals("4th place must be awarded 12 pts", 12, ChampionshipTier.getPointsForPosition(4))
    }

    // 6. Standings and tie resolution
    @Test
    fun test06_standingsAndTieResolution() {
        val d1 = ChampionshipDriverStanding("Racer A", "Apex RS-1", "apex_valkyrie", points = 43, wins = 1, podiums = 2, bestFinish = 1, isPlayer = false)
        val d2 = ChampionshipDriverStanding("Racer B", "Apex Spirit", "apex_spirit_86", points = 43, wins = 1, podiums = 2, bestFinish = 1, isPlayer = true)
        val d3 = ChampionshipDriverStanding("Racer C", "Apex Monza", "apex_monza_v10", points = 50, wins = 2, podiums = 2, bestFinish = 1, isPlayer = false)

        val state = ActiveChampionshipState(
            tier = ChampionshipTier.BRONZE,
            currentRoundIndex = 1,
            standings = mutableListOf(d1, d2, d3)
        )

        val sorted = state.getSortedStandings()
        assertEquals("Leader must be Racer C with 50 pts", "Racer C", sorted[0].driverName)
        // Tie between A and B (same points, wins, podiums, finish) resolved deterministically in player favor
        assertEquals("Player wins tie resolution", "Racer B", sorted[1].driverName)
        assertEquals("Racer A is 3rd", "Racer A", sorted[2].driverName)
    }

    // 7. Final championship completion
    @Test
    fun test07_finalChampionshipCompletion() {
        viewModel.startChampionship(ChampionshipTier.BRONZE)
        val champ = viewModel.activeChampionship.value!!

        // Advance to round 3 (last round)
        champ.currentRoundIndex = 2
        champ.standings.find { it.isPlayer }!!.points = 50
        champ.standings.filter { !it.isPlayer }.forEach { it.points = 30 }

        val dummyStandings = listOf(
            LeaderboardRacer(1, dataManager.playerName, "Apex Valkyrie", floatArrayOf(1f, 0f, 0f), 1, 65f, "LEADER", isPlayer = true),
            LeaderboardRacer(2, "Viper", "Apex Spirit", floatArrayOf(0f, 1f, 0f), 1, 67f, "+2.00s"),
            LeaderboardRacer(3, "Shadow", "Apex Monza", floatArrayOf(0f, 0f, 1f), 1, 68f, "+3.00s"),
            LeaderboardRacer(4, "Ghost", "Apex Valkyrie", floatArrayOf(1f, 1f, 1f), 1, 69f, "+4.00s")
        )

        viewModel.handleRaceCompletion(1, dummyStandings)

        assertTrue("Cup must be marked as completed", champ.isCupCompleted)
        assertTrue("Bronze must be added to completed cups", dataManager.completedCups.contains("BRONZE"))
        assertTrue("Silver Cup must be unlocked", dataManager.unlockedCups.contains("SILVER"))
    }

    // 8. Trophy awards and unlocks
    @Test
    fun test08_trophyAwardsAndUnlocks() {
        dataManager.recordCupCompletion(ChampionshipTier.BRONZE, playerWon = true)
        assertTrue("Bronze trophy earned", dataManager.trophiesEarned.contains("BRONZE"))

        dataManager.recordCupCompletion(ChampionshipTier.SILVER, playerWon = true)
        assertTrue("Silver trophy earned", dataManager.trophiesEarned.contains("SILVER"))

        dataManager.recordCupCompletion(ChampionshipTier.GOLD, playerWon = true)
        assertTrue("Gold trophy earned", dataManager.trophiesEarned.contains("GOLD"))
        assertEquals("Total 3 trophies earned", 3, dataManager.trophiesEarned.size)
    }

    // 9. Credit and XP rewards
    @Test
    fun test09_creditAndXpRewards() {
        val initialCredits = dataManager.credits
        val initialXp = dataManager.driverXp

        viewModel.startChampionship(ChampionshipTier.BRONZE)
        val dummyStandings = listOf(
            LeaderboardRacer(1, dataManager.playerName, "Apex Valkyrie", floatArrayOf(1f, 0f, 0f), 1, 65f, "LEADER", isPlayer = true)
        )

        viewModel.handleRaceCompletion(1, dummyStandings)

        // 1st place round award: 7,500 CR + 500 XP
        assertEquals(initialCredits + 7500, dataManager.credits)
        assertEquals(initialXp + 500, dataManager.driverXp)
    }

    // 10. Duplicate result submission and duplicate-reward prevention (Idempotency)
    @Test
    fun test10_duplicateResultSubmissionPrevention() {
        viewModel.startChampionship(ChampionshipTier.BRONZE)
        val dummyStandings = listOf(
            LeaderboardRacer(1, dataManager.playerName, "Apex Valkyrie", floatArrayOf(1f, 0f, 0f), 1, 65f, "LEADER", isPlayer = true)
        )

        val creditsBefore = dataManager.credits
        val ptsBefore = dataManager.careerChampionshipPoints

        // First completion
        viewModel.handleRaceCompletion(1, dummyStandings)
        val creditsAfterFirst = dataManager.credits
        val ptsAfterFirst = dataManager.careerChampionshipPoints
        assertEquals(creditsBefore + 7500, creditsAfterFirst)
        assertEquals(ptsBefore + 25, ptsAfterFirst)

        // Second duplicate submission for same round
        viewModel.handleRaceCompletion(1, dummyStandings)
        assertEquals("Duplicate call must not add further credits", creditsAfterFirst, dataManager.credits)
        assertEquals("Duplicate call must not add further championship points", ptsAfterFirst, dataManager.careerChampionshipPoints)
    }

    // 11. Interrupted races and recovery after application restart
    @Test
    fun test11_interruptedRacesAndRecovery() {
        viewModel.startChampionship(ChampionshipTier.BRONZE)
        val state = viewModel.activeChampionship.value!!
        state.standings.find { it.isPlayer }!!.points = 25
        dataManager.saveActiveChampionship(state)

        // Simulate app kill and recreation
        val newVm = GameViewModel(app)
        val loaded = newVm.activeChampionship.value
        assertNotNull("Active championship must be recovered from persistent store", loaded)
        assertEquals(ChampionshipTier.BRONZE, loaded!!.tier)
        assertEquals(0, loaded.currentRoundIndex)
        assertEquals(25, loaded.standings.find { it.isPlayer }!!.points)

        val resumed = newVm.resumeActiveChampionship()
        assertTrue("Championship must resume successfully", resumed)
        assertEquals(RaceMode.CHAMPIONSHIP, newVm.activeRaceMode.value)
    }

    // 12. Persistence across process/application restarts
    @Test
    fun test12_persistenceAcrossRestarts() {
        dataManager.unlockedCups = setOf("BRONZE", "SILVER")
        dataManager.completedCups = setOf("BRONZE")
        dataManager.trophiesEarned = setOf("BRONZE")
        dataManager.careerChampionshipPoints = 148
        dataManager.championshipWins = 1

        val freshDm = GameDataManager(app)
        assertTrue(freshDm.unlockedCups.contains("SILVER"))
        assertTrue(freshDm.completedCups.contains("BRONZE"))
        assertTrue(freshDm.trophiesEarned.contains("BRONZE"))
        assertEquals(148, freshDm.careerChampionshipPoints)
        assertEquals(1, freshDm.championshipWins)
    }

    // 13. Existing race rewards and career statistics remaining correct
    @Test
    fun test13_existingRaceRewardsAndCareerStatsIntegrity() {
        val initialRacesCompleted = dataManager.racesCompleted
        val initialRacesWon = dataManager.racesWon

        viewModel.handleRaceCompletion(1, listOf(LeaderboardRacer(1, dataManager.playerName, "Apex Valkyrie", floatArrayOf(1f, 0f, 0f), 1, 65f, "LEADER", isPlayer = true)))

        assertEquals(initialRacesCompleted + 1, dataManager.racesCompleted)
        assertEquals(initialRacesWon + 1, dataManager.racesWon)
    }

    // 14. Compatibility with existing profile, garage, and customization data
    @Test
    fun test14_compatibilityWithProfileAndGarage() {
        val upgrades = CarUpgradeState(engineStage = 3, tireStage = 2, suspensionStage = 1, weightReduction = 0)
        dataManager.saveCarUpgrades("apex_monza_v10", upgrades)
        dataManager.setCarColorIndex("apex_monza_v10", 4)

        // Starting championship does not reset garage data
        viewModel.startChampionship(ChampionshipTier.BRONZE)

        val retrievedUpgrades = dataManager.getCarUpgrades("apex_monza_v10")
        assertEquals(3, retrievedUpgrades.engineStage)
        assertEquals(2, retrievedUpgrades.tireStage)
        assertEquals(4, dataManager.getCarColorIndex("apex_monza_v10"))
    }

    // 15. Responsive championship navigation and safe UI state transitions
    @Test
    fun test15_championshipNavigationAndScreenTransitions() {
        assertEquals(GameScreen.MAIN_MENU, viewModel.currentScreen.value)

        viewModel.navigateTo(GameScreen.CHAMPIONSHIP)
        assertEquals(GameScreen.CHAMPIONSHIP, viewModel.currentScreen.value)

        viewModel.navigateTo(GameScreen.GARAGE)
        assertEquals(GameScreen.GARAGE, viewModel.currentScreen.value)

        viewModel.navigateTo(GameScreen.MAIN_MENU)
        assertEquals(GameScreen.MAIN_MENU, viewModel.currentScreen.value)

        // Abandon championship clears state cleanly
        viewModel.startChampionship(ChampionshipTier.BRONZE)
        assertNotNull(viewModel.activeChampionship.value)
        viewModel.abandonActiveChampionship()
        assertNull(viewModel.activeChampionship.value)
        assertNull(dataManager.loadActiveChampionship())
    }
}
