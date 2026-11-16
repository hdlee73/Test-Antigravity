package io.github.hdlee73.etftracker

import io.github.hdlee73.etftracker.data.repo.SampleEtfCatalog
import io.github.hdlee73.etftracker.data.update.UpdateChecker
import io.github.hdlee73.etftracker.domain.analytics.EtfAnalyticsEngine
import io.github.hdlee73.etftracker.domain.model.DiscrepancyAlertLevel
import io.github.hdlee73.etftracker.domain.model.ExitActionType
import io.github.hdlee73.etftracker.domain.model.HoldingPosition
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
    fun generateTradingSignal_differentiatesScoresAcrossCatalog() {
        val signals = SampleEtfCatalog.etfs.map { EtfAnalyticsEngine.generateTradingSignal(it) }
        val maxScore = signals.maxOf { it.totalScore }
        val minScore = signals.minOf { it.totalScore }
        val distinctScores = signals.map { it.totalScore }.distinct().size
        assertTrue("Expected wide score spread (>=30), got ${maxScore - minScore}", (maxScore - minScore) >= 30)
        assertTrue("Expected at least 5 distinct scores, got $distinctScores", distinctScores >= 5)
        assertTrue(signals.all { it.entryPriceWon > 0 && it.targetPriceWon > 0 && it.stopLossPriceWon > 0 })
    }

    @Test
    fun evaluateHoldingSellSignal_detectsTakeProfitAndStopLoss() {
        val sp500 = SampleEtfCatalog.etfs.first { it.code == "360750" }
        val profitPos = HoldingPosition(
            code = sp500.code,
            name = sp500.name,
            avgBuyPrice = (sp500.marketPrice * 0.84).toLong(),
            quantity = 50,
            targetProfitPct = 12.0,
            stopLossPct = -6.0,
        )
        val profitSignal = EtfAnalyticsEngine.evaluateHoldingSellSignal(profitPos, sp500)
        assertTrue(
            profitSignal.action == ExitActionType.PARTIAL_TAKE_PROFIT ||
                profitSignal.action == ExitActionType.TAKE_PROFIT_NOW,
        )
        assertTrue(profitSignal.recommendedSellRatioPct >= 50)

        val lossPos = HoldingPosition(
            code = sp500.code,
            name = sp500.name,
            avgBuyPrice = (sp500.marketPrice * 1.12).toLong(),
            quantity = 30,
            targetProfitPct = 12.0,
            stopLossPct = -6.0,
        )
        val lossSignal = EtfAnalyticsEngine.evaluateHoldingSellSignal(lossPos, sp500)
        assertEquals(ExitActionType.STOP_LOSS_NOW, lossSignal.action)
        assertEquals(100, lossSignal.recommendedSellRatioPct)
    }

    @Test
    fun analyzeHistory_computesValidRsiAndMultiPeriodReturns() {
        val semi = SampleEtfCatalog.etfs.first { it.code == "381180" }
        val history = EtfAnalyticsEngine.analyzeHistory(semi)
        assertTrue(history.rsi14 in 1.0..99.0)
        assertTrue(history.ma5 > 0.0 && history.ma20 > 0.0 && history.ma60 > 0.0)
        assertTrue(history.return3M > 0.0)
        assertTrue(history.winRatePct in 20.0..95.0)
    }
}
