package io.github.hdlee73.etftracker

import io.github.hdlee73.etftracker.data.repo.SampleEtfCatalog
import io.github.hdlee73.etftracker.data.update.UpdateChecker
import io.github.hdlee73.etftracker.domain.analytics.EtfAnalyticsEngine
import io.github.hdlee73.etftracker.domain.model.DiscrepancyAlertLevel
import io.github.hdlee73.etftracker.domain.model.RebalanceAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EtfAnalyticsEngineTest {

    @Test
    fun detectRebalancing_findsAddedAndRemovedHoldings() {
        val activeEtf = SampleEtfCatalog.etfs.first { it.code == "441640" }
        val events = EtfAnalyticsEngine.detectRebalancing(activeEtf.holdings)
        assertTrue(events.any { it.stockName == "Eli Lilly" && it.action == RebalanceAction.ADDED })
        assertTrue(events.any { it.stockName == "Intel" && it.action == RebalanceAction.REMOVED })
    }

    @Test
    fun analyzeOverlap_computesCommonWeightAndCount() {
        val auto = SampleEtfCatalog.etfs.first { it.code == "091180" }
        val valueUp = SampleEtfCatalog.etfs.first { it.code == "496120" }
        val overlap = EtfAnalyticsEngine.analyzeOverlap(auto, valueUp)
        assertTrue(overlap.commonCount >= 3)
        assertTrue(overlap.totalOverlapPct > 10.0)
    }

    @Test
    fun compareRealCosts_sortsByLowestRealCostAndComputesWon() {
        val rows = EtfAnalyticsEngine.compareRealCosts(SampleEtfCatalog.etfs, 10_000_000L)
        assertTrue(rows.first().realCostPct <= rows.last().realCostPct)
        assertTrue(rows.all { it.annualCostWon >= 0L })
    }

    @Test
    fun evaluateDiscrepancy_detectsOverheatOrLpGap() {
        val auto = SampleEtfCatalog.etfs.first { it.code == "091180" }
        val level = EtfAnalyticsEngine.evaluateDiscrepancy(auto)
        assertEquals(DiscrepancyAlertLevel.OVERHEAT_ALERT, level)
    }

    @Test
    fun simulateDividendCompound_trOutperformsCashOut() {
        val sim = EtfAnalyticsEngine.simulateDividendCompound(
            principalWon = 10_000_000L,
            annualPriceGrowthPct = 6.0,
            annualDividendYieldPct = 4.0,
            years = 10,
        )
        assertTrue(sim.trFinalWon > sim.cashOutFinalWon)
        assertEquals(11, sim.points.size)
    }

    @Test
    fun compareAccountTaxes_isaAndPensionSaveTaxOverGeneral() {
        val rows = EtfAnalyticsEngine.compareAccountTaxes(5_000_000L, isComprehensiveIncomeTaxSubject = false)
        val general = rows.first { it.accountName.contains("일반") }
        val isa = rows.first { it.accountName.contains("ISA") }
        assertTrue(isa.netDividendWon > general.netDividendWon)
        assertTrue(isa.taxSavedWon > 0L)
    }

    @Test
    fun updateChecker_comparesSemVerCorrectly() {
        assertTrue(UpdateChecker.isNewerVersion("1.0.1", "1.0.0"))
        assertTrue(UpdateChecker.isNewerVersion("1.1.0", "1.0.9"))
        assertFalse(UpdateChecker.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(UpdateChecker.isNewerVersion("0.9.9", "1.0.0"))
    }

    @Test
    fun generateTradingSignal_returnsValidScoreAndFactors() {
        val sp500 = SampleEtfCatalog.etfs.first { it.code == "360750" }
        val signal = EtfAnalyticsEngine.generateTradingSignal(sp500)
        assertTrue(signal.totalScore in 0..100)
        assertEquals(4, signal.factors.size)
    }
}
