package io.github.hdlee73.etftracker.domain.analytics

import io.github.hdlee73.etftracker.domain.model.AccountTaxRow
import io.github.hdlee73.etftracker.domain.model.CorrelationMatrix
import io.github.hdlee73.etftracker.domain.model.CostComparisonRow
import io.github.hdlee73.etftracker.domain.model.DiscrepancyAlertLevel
import io.github.hdlee73.etftracker.domain.model.DividendSimulationResult
import io.github.hdlee73.etftracker.domain.model.DividendYearPoint
import io.github.hdlee73.etftracker.domain.model.EtfSummary
import io.github.hdlee73.etftracker.domain.model.OverlapAnalysis
import io.github.hdlee73.etftracker.domain.model.OverlapHoldingItem
import io.github.hdlee73.etftracker.domain.model.PdfHolding
import io.github.hdlee73.etftracker.domain.model.RebalanceAction
import io.github.hdlee73.etftracker.domain.model.RebalanceEvent
import io.github.hdlee73.etftracker.domain.model.StressTestRow
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * 순수 Kotlin 기반 ETF 정밀 분석 엔진 (Compose Multiplatform 공통 모듈 이식 가능).
 */
object EtfAnalyticsEngine {

    fun detectRebalancing(holdings: List<PdfHolding>): List<RebalanceEvent> {
        return holdings.mapNotNull { h ->
            val action = when {
                h.prevShareCount == 0L && h.shareCount > 0L -> RebalanceAction.ADDED
                h.prevShareCount > 0L && h.shareCount == 0L -> RebalanceAction.REMOVED
                h.shareDelta > 0L -> RebalanceAction.INCREASED
                h.shareDelta < 0L -> RebalanceAction.DECREASED
                else -> null
            } ?: return@mapNotNull null

            RebalanceEvent(
                stockCode = h.code,
                stockName = h.name,
                action = action,
                shareDelta = h.shareDelta,
                weightDeltaPct = h.weightDeltaPct,
            )
        }.sortedByDescending { abs(it.weightDeltaPct) }
    }

    fun analyzeOverlap(
        etfA: EtfSummary,
        etfB: EtfSummary,
        weightShareA: Double = 0.5,
    ): OverlapAnalysis {
        val clampedA = weightShareA.coerceIn(0.0, 1.0)
        val clampedB = 1.0 - clampedA
        val mapA = etfA.holdings.associateBy { it.code }
        val mapB = etfB.holdings.associateBy { it.code }
        val allCodes = (mapA.keys + mapB.keys).distinct()

        val commonItems = allCodes.mapNotNull { code ->
            val a = mapA[code]
            val b = mapB[code]
            if (a != null && b != null) {
                val minOverlap = minOf(a.weightPct, b.weightPct)
                val exposure = a.weightPct * clampedA + b.weightPct * clampedB
                OverlapHoldingItem(
                    code = code,
                    name = a.name,
                    weightA = a.weightPct,
                    weightB = b.weightPct,
                    minOverlapPct = minOverlap,
                    combinedExposurePct = exposure,
                )
            } else {
                null
            }
        }.sortedByDescending { it.minOverlapPct }

        return OverlapAnalysis(
            etfAName = etfA.name,
            etfBName = etfB.name,
            totalOverlapPct = commonItems.sumOf { it.minOverlapPct }.coerceAtMost(100.0),
            commonCount = commonItems.size,
            items = commonItems,
        )
    }

    fun compareRealCosts(etfs: List<EtfSummary>, investmentWon: Long): List<CostComparisonRow> {
        val safePrincipal = investmentWon.coerceAtLeast(0L)
        return etfs.map { etf ->
            val annualCost = (safePrincipal * (etf.realCostPct / 100.0)).roundToLong()
            CostComparisonRow(
                code = etf.code,
                name = etf.name,
                manager = etf.manager,
                terPct = etf.terPct,
                otherCostPct = etf.otherCostPct,
                brokerageFeePct = etf.brokerageFeePct,
                realCostPct = etf.realCostPct,
                annualCostWon = annualCost,
            )
        }.sortedBy { it.realCostPct }
    }

    fun evaluateDiscrepancy(etf: EtfSummary): DiscrepancyAlertLevel {
        val absDiff = abs(etf.discrepancyPct)
        val lpWeak = minOf(etf.lpAskQty, etf.lpBidQty) < 500L
        return when {
            absDiff >= 0.8 || (absDiff >= 0.4 && lpWeak) -> DiscrepancyAlertLevel.OVERHEAT_ALERT
            absDiff >= 0.3 || lpWeak -> DiscrepancyAlertLevel.CAUTION
            else -> DiscrepancyAlertLevel.NORMAL
        }
    }

    fun calculateTrackingErrorPct(etfPrices: List<Double>, benchmarkPrices: List<Double>): Double {
        val count = minOf(etfPrices.size, benchmarkPrices.size)
        if (count < 3) return 0.0
        val diffs = buildList {
            for (i in 1 until count) {
                val prevE = etfPrices[i - 1]
                val prevB = benchmarkPrices[i - 1]
                if (prevE > 0.0 && prevB > 0.0) {
                    val retE = (etfPrices[i] - prevE) / prevE
                    val retB = (benchmarkPrices[i] - prevB) / prevB
                    add(retE - retB)
                }
            }
        }
        if (diffs.size < 2) return 0.0
        val mean = diffs.average()
        val variance = diffs.sumOf { (it - mean).pow(2) } / (diffs.size - 1)
        return sqrt(variance) * sqrt(252.0) * 100.0
    }

    fun simulateDividendCompound(
        principalWon: Long,
        annualPriceGrowthPct: Double,
        annualDividendYieldPct: Double,
        years: Int,
        taxRatePct: Double = 15.4,
    ): DividendSimulationResult {
        val safePrincipal = principalWon.coerceAtLeast(0L)
        val safeYears = years.coerceIn(1, 40)
        val priceGrowth = annualPriceGrowthPct / 100.0
        val divYield = annualDividendYieldPct / 100.0
        val afterTaxFactor = 1.0 - (taxRatePct.coerceIn(0.0, 50.0) / 100.0)

        var trAsset = safePrincipal.toDouble()
        var cashAsset = safePrincipal.toDouble()
        var accumulatedCash = 0.0

        val points = mutableListOf(
            DividendYearPoint(year = 0, trValueWon = safePrincipal, cashOutValueWon = safePrincipal)
        )

        for (yr in 1..safeYears) {
            val trDiv = trAsset * divYield * afterTaxFactor
            trAsset = trAsset * (1.0 + priceGrowth) + trDiv

            val cashDiv = cashAsset * divYield * afterTaxFactor
            cashAsset *= (1.0 + priceGrowth)
            accumulatedCash += cashDiv

            points += DividendYearPoint(
                year = yr,
                trValueWon = trAsset.roundToLong(),
                cashOutValueWon = (cashAsset + accumulatedCash).roundToLong(),
            )
        }

        val finalTr = points.last().trValueWon
        val finalCash = points.last().cashOutValueWon
        return DividendSimulationResult(
            principalWon = safePrincipal,
            years = safeYears,
            trFinalWon = finalTr,
            cashOutFinalWon = finalCash,
            compoundGapWon = finalTr - finalCash,
            points = points,
        )
    }

    fun compareAccountTaxes(
        annualGrossDividendWon: Long,
        isComprehensiveIncomeTaxSubject: Boolean,
    ): List<AccountTaxRow> {
        val gross = annualGrossDividendWon.coerceAtLeast(0L)
        val generalRate = if (isComprehensiveIncomeTaxSubject) 0.264 else 0.154
        val generalTax = (gross * generalRate).roundToLong()

        // ISA: 비과세 한도(서민형/일반형 평균 200만원 가정) 초과분 9.9% 분리과세
        val isaTaxable = (gross - 2_000_000L).coerceAtLeast(0L)
        val isaTax = (isaTaxable * 0.099).roundToLong()

        // 연금저축 / IRP: 인출 시 연금소득세 3.3%~5.5% (과세이연 효과 반영 4.4% 기준)
        val pensionTax = (gross * 0.044).roundToLong()
        val irpTax = (gross * 0.033).roundToLong()

        return listOf(
            AccountTaxRow(
                accountName = "일반 위탁계좌",
                taxRateLabel = if (isComprehensiveIncomeTaxSubject) "26.4% (종합과세)" else "15.4% (배당소득세)",
                grossDividendWon = gross,
                taxWon = generalTax,
                netDividendWon = gross - generalTax,
                taxSavedWon = 0L,
            ),
            AccountTaxRow(
                accountName = "ISA (중개형)",
                taxRateLabel = "200만원 비과세 + 9.9%",
                grossDividendWon = gross,
                taxWon = isaTax,
                netDividendWon = gross - isaTax,
                taxSavedWon = (generalTax - isaTax).coerceAtLeast(0L),
            ),
            AccountTaxRow(
                accountName = "연금저축펀드",
                taxRateLabel = "과세이연 후 4.4%",
                grossDividendWon = gross,
                taxWon = pensionTax,
                netDividendWon = gross - pensionTax,
                taxSavedWon = (generalTax - pensionTax).coerceAtLeast(0L),
            ),
            AccountTaxRow(
                accountName = "개인형 IRP",
                taxRateLabel = "과세이연 후 3.3%",
                grossDividendWon = gross,
                taxWon = irpTax,
                netDividendWon = gross - irpTax,
                taxSavedWon = (generalTax - irpTax).coerceAtLeast(0L),
            ),
        )
    }

    fun calculateStressTest(etf: EtfSummary): StressTestRow {
        val prices = etf.dailyPrices
        if (prices.isEmpty()) {
            return StressTestRow(etf.code, etf.name, 0.0, 0, true, emptyList())
        }
        var peak = prices.first()
        var maxDrawdown = 0.0
        var currentUnderwater = 0
        var maxUnderwater = 0
        val drawdowns = mutableListOf<Double>()

        for (p in prices) {
            if (p >= peak) {
                peak = p
                currentUnderwater = 0
                drawdowns += 0.0
            } else {
                val dd = ((p - peak) / peak) * 100.0
                drawdowns += dd
                if (dd < maxDrawdown) maxDrawdown = dd
                currentUnderwater++
                if (currentUnderwater > maxUnderwater) maxUnderwater = currentUnderwater
            }
        }

        return StressTestRow(
            code = etf.code,
            name = etf.name,
            mddPct = maxDrawdown,
            underwaterDays = maxUnderwater * 15, // 영업일/구간 환산
            recovered = prices.last() >= peak * 0.995,
            drawdownSeries = drawdowns,
        )
    }

    fun buildCorrelationMatrix(seriesMap: Map<String, List<Double>>): CorrelationMatrix {
        val labels = seriesMap.keys.toList()
        val returnsMap = labels.associateWith { key ->
            val prices = seriesMap[key].orEmpty()
            if (prices.size < 2) emptyList()
            else (1 until prices.size).mapNotNull { i ->
                val prev = prices[i - 1]
                if (prev > 0.0) (prices[i] - prev) / prev else null
            }
        }

        val matrix = labels.map { rowKey ->
            val r1 = returnsMap[rowKey].orEmpty()
            labels.map { colKey ->
                if (rowKey == colKey) 1.0 else pearson(r1, returnsMap[colKey].orEmpty())
            }
        }
        return CorrelationMatrix(labels = labels, values = matrix)
    }

    private fun pearson(x: List<Double>, y: List<Double>): Double {
        val n = minOf(x.size, y.size)
        if (n < 2) return 0.0
        val xSub = x.take(n)
        val ySub = y.take(n)
        val meanX = xSub.average()
        val meanY = ySub.average()
        var num = 0.0
        var denX = 0.0
        var denY = 0.0
        for (i in 0 until n) {
            val dx = xSub[i] - meanX
            val dy = ySub[i] - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }
        val denom = sqrt(denX * denY)
        return if (denom == 0.0) 0.0 else (num / denom).coerceIn(-1.0, 1.0)
    }
}
