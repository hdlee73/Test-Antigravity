package io.github.hdlee73.etftracker.domain.analytics

import io.github.hdlee73.etftracker.domain.model.AccountTaxRow
import io.github.hdlee73.etftracker.domain.model.CorrelationMatrix
import io.github.hdlee73.etftracker.domain.model.CostComparisonRow
import io.github.hdlee73.etftracker.domain.model.DiscrepancyAlertLevel
import io.github.hdlee73.etftracker.domain.model.DividendSimulationResult
import io.github.hdlee73.etftracker.domain.model.DividendYearPoint
import io.github.hdlee73.etftracker.domain.model.EtfSummary
import io.github.hdlee73.etftracker.domain.model.ExitActionType
import io.github.hdlee73.etftracker.domain.model.HoldingPosition
import io.github.hdlee73.etftracker.domain.model.HoldingSellSignal
import io.github.hdlee73.etftracker.domain.model.OverlapAnalysis
import io.github.hdlee73.etftracker.domain.model.OverlapHoldingItem
import io.github.hdlee73.etftracker.domain.model.PdfHolding
import io.github.hdlee73.etftracker.domain.model.RebalanceAction
import io.github.hdlee73.etftracker.domain.model.RebalanceEvent
import io.github.hdlee73.etftracker.domain.model.SignalFactor
import io.github.hdlee73.etftracker.domain.model.StressTestRow
import io.github.hdlee73.etftracker.domain.model.TechnicalHistoryAnalysis
import io.github.hdlee73.etftracker.domain.model.TradingSignalGrade
import io.github.hdlee73.etftracker.domain.model.TradingSignalResult
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * 순수 Kotlin 기반 ETF 퀀트 시그널·매도 코치·백테스팅 엔진.
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
        val lpWeak = (etf.lpAskQty > 0L || etf.lpBidQty > 0L) && minOf(etf.lpAskQty, etf.lpBidQty) < 500L
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

        val isaTaxable = (gross - 2_000_000L).coerceAtLeast(0L)
        val isaTax = (isaTaxable * 0.099).roundToLong()

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

        val dayScale = if (prices.size >= 30) 1 else 5
        return StressTestRow(
            code = etf.code,
            name = etf.name,
            mddPct = maxDrawdown,
            underwaterDays = maxUnderwater * dayScale,
            recovered = prices.last() >= peak * 0.99,
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

    /**
     * 과거 시계열(일봉/이평선/RSI/변동성/수익률/시그널 승률)을 정밀 계산한다.
     */
    fun analyzeHistory(etf: EtfSummary): TechnicalHistoryAnalysis {
        val prices = etf.dailyPrices.ifEmpty { listOf(etf.marketPrice.toDouble()) }
        val last = prices.last().coerceAtLeast(1.0)
        val ma5 = prices.takeLast(5).average()
        val ma20 = prices.takeLast(minOf(20, prices.size)).average()
        val ma60 = prices.takeLast(minOf(60, prices.size)).average()

        val rsi14 = computeRsi(prices, 14)

        fun retAt(lookback: Int): Double {
            if (prices.size < 2) return etf.dailyChangePct
            val idx = (prices.size - 1 - lookback).coerceAtLeast(0)
            val base = prices[idx].coerceAtLeast(1.0)
            return ((last - base) / base) * 100.0
        }

        val ret1M = retAt(minOf(20, maxOf(1, prices.size / 3)))
        val ret3M = retAt(minOf(60, maxOf(2, (prices.size * 2) / 3)))
        val ret6M = retAt(prices.size - 1)

        val dailyReturns = if (prices.size >= 2) {
            (1 until prices.size).map { i ->
                val prev = prices[i - 1].coerceAtLeast(1.0)
                (prices[i] - prev) / prev
            }
        } else {
            emptyList()
        }

        val volPct = if (dailyReturns.size >= 2) {
            val mean = dailyReturns.average()
            val variance = dailyReturns.sumOf { (it - mean).pow(2) } / (dailyReturns.size - 1)
            sqrt(variance) * sqrt(252.0) * 100.0
        } else {
            14.5
        }

        val stress = calculateStressTest(etf)
        val mdd = if (stress.mddPct < -0.5) stress.mddPct else etf.mddPct

        val trendStatus = when {
            last >= ma5 && ma5 >= ma20 -> "정배열 상승 추세 (MA5 > MA20)"
            last >= ma20 -> "단기 눌림목 · 중기 상승 유지"
            last < ma5 && ma5 < ma20 -> "역배열 하락 구간 (보수적 접근)"
            else -> "바닥 다지기 · 추세 전환 시도"
        }

        // 과거 골든크로스/RSI 반등 구간 승률 시뮬레이션
        var wins = 0
        var trades = 0
        var cumReturn = 0.0
        if (prices.size >= 6) {
            for (i in 2 until prices.size - 2) {
                val prevSlope = prices[i] - prices[i - 2]
                val nextRet = ((prices[i + 2] - prices[i]) / prices[i].coerceAtLeast(1.0)) * 100.0
                if (prevSlope >= 0.0) {
                    trades++
                    cumReturn += nextRet
                    if (nextRet > 0.0) wins++
                }
            }
        }
        val winRate = if (trades > 0) (wins.toDouble() / trades) * 100.0 else 58.0

        return TechnicalHistoryAnalysis(
            rsi14 = rsi14,
            ma5 = ma5,
            ma20 = ma20,
            ma60 = ma60,
            return1M = ret1M,
            return3M = ret3M,
            return6M = ret6M,
            volatilityPct = volPct,
            mddPct = mdd,
            trendStatus = trendStatus,
            winRatePct = winRate.coerceIn(25.0, 92.0),
            signalBacktestReturnPct = cumReturn.coerceIn(-35.0, 85.0),
        )
    }

    /**
     * 종목별 고유 시계열·RSI·이평선·괴리율·PDF 기여도·비용을 정밀 반영해 0~100점 차등 점수를 산출한다.
     */
    fun generateTradingSignal(etf: EtfSummary): TradingSignalResult {
        val hist = analyzeHistory(etf)
        val lastPrice = etf.marketPrice.coerceAtLeast(1000L)

        // 1. 추세 · 모멘텀 · RSI (0~35점, 종목별 연속 수식으로 확실한 점수 차등)
        val maAlignScore = when {
            hist.ma5 > hist.ma20 * 1.01 -> 12.0
            hist.ma5 >= hist.ma20 -> 8.5
            hist.ma5 >= hist.ma20 * 0.985 -> 5.0
            else -> 1.5
        }
        val retScore = (hist.return1M * 0.75 + hist.return3M * 0.45 + etf.dailyChangePct * 1.6).coerceIn(-8.0, 15.0)
        // RSI가 32~58 구간이면 상승 여력이 높고, 75 이상이면 단기 과열 감점
        val rsiScore = when {
            hist.rsi14 in 32.0..58.0 -> 8.0 - abs(45.0 - hist.rsi14) * 0.12
            hist.rsi14 < 32.0 -> 6.5 // 과매도 기술적 반등 기대
            hist.rsi14 <= 70.0 -> 4.5
            else -> 1.0 // 과매수 추격 주의
        }
        val trendScore = (maAlignScore + retScore + rsiScore).roundToInt().coerceIn(2, 35)
        val trendSummary = "${hist.trendStatus} · RSI ${String.format(Locale.US, "%.1f", hist.rsi14)} · 1개월 ${formatPct(hist.return1M)}"

        // 2. iNAV 괴리율 · LP 호가 유동성 (0~25점)
        val disc = etf.discrepancyPct
        val lpMin = minOf(etf.lpAskQty, etf.lpBidQty)
        val lpBonus = when {
            lpMin >= 5_000L -> 5.0
            lpMin >= 1_000L -> 3.0
            lpMin in 1L..499L -> -5.0
            else -> 1.5
        }
        val discRaw = 17.0 - (disc * 14.0) + lpBonus
        val discScore = discRaw.roundToInt().coerceIn(1, 25)
        val discSummary = when {
            disc <= -0.12 -> "iNAV 대비 ${String.format(Locale.US, "%.2f", disc)}% 저평가 할인 (매수 유리)"
            disc >= 0.45 -> "iNAV 대비 +${String.format(Locale.US, "%.2f", disc)}% 할증 과열 (고점 주의)"
            lpMin in 1L..499L -> "LP 호가 얇음 (${lpMin}주) · 시장가 주문 주의"
            else -> "괴리율 ${formatPct(disc)} 안정권 · 유동성 양호"
        }

        // 3. PDF 구성종목 기여도 · 액티브 수급 (0~20점)
        val netContribution = etf.holdings.sumOf { it.contributionPctPoint }
        val posHoldingsRatio = if (etf.holdings.isNotEmpty()) {
            etf.holdings.count { it.dailyReturnPct > 0 }.toDouble() / etf.holdings.size
        } else {
            0.5 + (etf.dailyChangePct / 10.0).coerceIn(-0.3, 0.3)
        }
        val rebalBoost = etf.holdings.count { it.shareDelta > 0 } * 0.8 - etf.holdings.count { it.shareDelta < 0 } * 0.6
        val pdfScore = (8.0 + netContribution * 9.5 + posHoldingsRatio * 7.0 + rebalBoost).roundToInt().coerceIn(1, 20)
        val topMover = etf.holdings.maxByOrNull { abs(it.contributionPctPoint) }
        val pdfSummary = if (topMover != null) {
            "핵심 기여: ${topMover.name}(${formatPct(topMover.contributionPctPoint, "%p")}) · 상승비중 ${(posHoldingsRatio * 100).roundToInt()}%"
        } else {
            "기초지수 동조 수급 (${formatPct(etf.dailyChangePct)})"
        }

        // 4. 위험 대비 수익(샤프·MDD) · 실부담비용 · 분배금 건전성 (0~20점)
        val sharpePart = (etf.sharpeRatio * 7.2).coerceIn(1.0, 10.0)
        val costPart = (5.5 - etf.realCostPct * 8.0).coerceIn(-2.0, 5.5)
        val mddPart = ((25.0 + hist.mddPct) * 0.22).coerceIn(-2.0, 4.5)
        val rocPenalty = if (etf.rocBreakdown.isPrincipalErosionRisk) -3.0 else 1.0
        val riskScore = (sharpePart + costPart + mddPart + rocPenalty).roundToInt().coerceIn(1, 20)
        val riskSummary = "샤프 ${String.format(Locale.US, "%.2f", etf.sharpeRatio)} · 실부담 ${String.format(Locale.US, "%.2f", etf.realCostPct)}% · MDD ${String.format(Locale.US, "%.1f", hist.mddPct)}%"

        val total = (trendScore + discScore + pdfScore + riskScore).coerceIn(5, 99)
        val grade = when {
            total >= 78 -> TradingSignalGrade.STRONG_BUY
            total >= 64 -> TradingSignalGrade.BUY
            total >= 48 -> TradingSignalGrade.HOLD
            total >= 35 -> TradingSignalGrade.TRIM
            else -> TradingSignalGrade.SELL
        }

        // 권장 진입가 · 목표가 · 손절가 계산
        val volFactor = (hist.volatilityPct / 100.0).coerceIn(0.08, 0.35)
        val entryPrice = if (disc > 0.25) {
            etf.iNav.roundToLong().coerceAtLeast(100L)
        } else {
            (lastPrice * 0.997).roundToLong()
        }
        val upsideRatio = when (grade) {
            TradingSignalGrade.STRONG_BUY -> 0.08 + volFactor * 0.35
            TradingSignalGrade.BUY -> 0.05 + volFactor * 0.25
            TradingSignalGrade.HOLD -> 0.03 + volFactor * 0.15
            TradingSignalGrade.TRIM -> 0.01
            TradingSignalGrade.SELL -> -0.03
        }
        val targetPrice = (lastPrice * (1.0 + upsideRatio)).roundToLong()
        val stopLossPrice = (lastPrice * (1.0 - (0.035 + volFactor * 0.15))).roundToLong()
        val expectedUpsidePct = ((targetPrice - lastPrice).toDouble() / lastPrice) * 100.0

        val actionText = when (grade) {
            TradingSignalGrade.STRONG_BUY ->
                "모멘텀·PDF 수급·괴리율이 모두 최상위권입니다. 권장 진입가 ${formatWonPlain(entryPrice)} 이하 분할 매수 후 목표가 ${formatWonPlain(targetPrice)}(+${String.format(Locale.US, "%.1f", expectedUpsidePct)}%) 노리세요."
            TradingSignalGrade.BUY ->
                "상승 추세와 구성종목 기여도가 우호적입니다. ${formatWonPlain(entryPrice)} 부근 분할 진입, 손절선 ${formatWonPlain(stopLossPrice)} 설정을 권장합니다."
            TradingSignalGrade.HOLD ->
                "단기 방향성이 팽팽합니다. 추격 매수보다 ${formatWonPlain(( hist.ma20 ).roundToLong())} 지지 여부를 확인하며 관망·보유하세요."
            TradingSignalGrade.TRIM ->
                "단기 과열(RSI ${String.format(Locale.US, "%.0f", hist.rsi14)}) 또는 괴리율 할증 구간입니다. 보유 물량의 30~50% 차익 실현을 권장합니다."
            TradingSignalGrade.SELL ->
                "추세 이탈 및 수급 약화가 뚜렷합니다. 반등 시 비중을 축소하고 현금 비중을 확보하세요."
        }

        return TradingSignalResult(
            code = etf.code,
            name = etf.name,
            manager = etf.manager,
            marketPrice = lastPrice,
            dailyChangePct = etf.dailyChangePct,
            grade = grade,
            totalScore = total,
            entryPriceWon = entryPrice,
            targetPriceWon = targetPrice,
            stopLossPriceWon = stopLossPrice,
            expectedUpsidePct = expectedUpsidePct,
            targetActionText = actionText,
            factors = listOf(
                SignalFactor("추세·모멘텀 (35)", trendScore, 35, trendSummary),
                SignalFactor("iNAV·유동성 (25)", discScore, 25, discSummary),
                SignalFactor("PDF 수급·기여 (20)", pdfScore, 20, pdfSummary),
                SignalFactor("위험·비용 효율 (20)", riskScore, 20, riskSummary),
            ),
            history = hist,
        )
    }

    /**
     * 이미 보유 중인 ETF 포지션에 대해 평단가·수량·고점 대비 하락률·기술적 꺾임을 종합해 구체적인 매도/익절/손절 시그널을 생성한다.
     */
    fun evaluateHoldingSellSignal(
        position: HoldingPosition,
        etf: EtfSummary?,
    ): HoldingSellSignal {
        val currentPrice = (etf?.marketPrice ?: position.avgBuyPrice).coerceAtLeast(1L)
        val avgBuy = position.avgBuyPrice.coerceAtLeast(1L)
        val qty = position.quantity.coerceAtLeast(1)
        val highest = maxOf(position.highestPriceSinceBuy, currentPrice, avgBuy)

        val pnlPct = ((currentPrice - avgBuy).toDouble() / avgBuy) * 100.0
        val pnlAmountWon = (currentPrice - avgBuy) * qty.toLong()
        val drawdownFromHighPct = ((currentPrice - highest).toDouble() / highest) * 100.0

        val marketSignal = etf?.let { generateTradingSignal(it) }
        val hist = marketSignal?.history ?: TechnicalHistoryAnalysis(
            rsi14 = 50.0,
            ma5 = currentPrice.toDouble(),
            ma20 = currentPrice.toDouble(),
            ma60 = currentPrice.toDouble(),
            return1M = pnlPct,
            return3M = pnlPct,
            return6M = pnlPct,
            volatilityPct = 16.0,
            mddPct = -8.0,
            trendStatus = "중립 구간",
            winRatePct = 55.0,
            signalBacktestReturnPct = pnlPct,
        )
        val totalScore = marketSignal?.totalScore ?: 55
        val discrepancyPct = etf?.discrepancyPct ?: 0.0

        val nextTargetPrice = (avgBuy * (1.0 + position.targetProfitPct / 100.0)).roundToLong()
        val stopLossPrice = (avgBuy * (1.0 + position.stopLossPct / 100.0)).roundToLong()
        val trailingStopPrice = (highest * 0.955).roundToLong() // 고점 대비 -4.5% 트레일링 스탑

        return when {
            // 1. 손절선 도달
            pnlPct <= position.stopLossPct -> HoldingSellSignal(
                position = position,
                currentPrice = currentPrice,
                pnlPct = pnlPct,
                pnlAmountWon = pnlAmountWon,
                drawdownFromHighPct = drawdownFromHighPct,
                action = ExitActionType.STOP_LOSS_NOW,
                urgencyScore = 95,
                recommendedSellRatioPct = 100,
                nextTrailingStopPrice = stopLossPrice,
                nextTargetPrice = nextTargetPrice,
                headline = "손절 기준(${String.format(Locale.US, "%.1f", position.stopLossPct)}%) 도달 · 전량 매도 권장",
                reasonDetail = "현재 수익률 ${formatPct(pnlPct)}로 설정 손절선을 하회했습니다. 추가 원금 손실을 막기 위해 기계적 손절 후 재진입 기회를 노리세요.",
            )

            // 2. 목표 수익률 달성 + 모멘텀 둔화/과열 → 전량 또는 대폭 익절
            pnlPct >= position.targetProfitPct && (hist.rsi14 >= 68.0 || totalScore < 58) -> HoldingSellSignal(
                position = position,
                currentPrice = currentPrice,
                pnlPct = pnlPct,
                pnlAmountWon = pnlAmountWon,
                drawdownFromHighPct = drawdownFromHighPct,
                action = ExitActionType.TAKE_PROFIT_NOW,
                urgencyScore = 90,
                recommendedSellRatioPct = 70,
                nextTrailingStopPrice = trailingStopPrice,
                nextTargetPrice = (currentPrice * 1.05).roundToLong(),
                headline = "목표 수익 달성(${formatPct(pnlPct)}) + 단기 고점 신호 · 70% 익절!",
                reasonDetail = "목표 수익률(${String.format(Locale.US, "%.1f", position.targetProfitPct)}%) 달성 동시에 RSI(${String.format(Locale.US, "%.1f", hist.rsi14)}) 과열이 감지되었습니다. 수익을 확정하세요.",
            )

            // 3. 목표 수익률 달성 (추세는 아직 강함) → 50% 분할 익절 + 트레일링 스탑
            pnlPct >= position.targetProfitPct -> HoldingSellSignal(
                position = position,
                currentPrice = currentPrice,
                pnlPct = pnlPct,
                pnlAmountWon = pnlAmountWon,
                drawdownFromHighPct = drawdownFromHighPct,
                action = ExitActionType.PARTIAL_TAKE_PROFIT,
                urgencyScore = 78,
                recommendedSellRatioPct = 50,
                nextTrailingStopPrice = trailingStopPrice,
                nextTargetPrice = (currentPrice * 1.06).roundToLong(),
                headline = "목표 수익 돌파(${formatPct(pnlPct)}) · 50% 분할 익절 권장",
                reasonDetail = "목표가(${formatWonPlain(nextTargetPrice)})를 돌파했습니다. 절반을 매도해 수익을 챙기고, 나머지는 트레일링 스탑(${formatWonPlain(trailingStopPrice)})으로 끝까지 가져가세요.",
            )

            // 4. 수익 구간에서 고점 대비 -4.5% 이상 꺾임 OR 역배열 전환 → 추세이탈 매도
            (pnlPct > 2.0 && drawdownFromHighPct <= -4.5) || marketSignal?.grade == TradingSignalGrade.SELL -> HoldingSellSignal(
                position = position,
                currentPrice = currentPrice,
                pnlPct = pnlPct,
                pnlAmountWon = pnlAmountWon,
                drawdownFromHighPct = drawdownFromHighPct,
                action = ExitActionType.TRAILING_STOP_SELL,
                urgencyScore = 84,
                recommendedSellRatioPct = 80,
                nextTrailingStopPrice = trailingStopPrice,
                nextTargetPrice = nextTargetPrice,
                headline = "고점 대비 ${formatPct(drawdownFromHighPct)} 하락 · 추세 이탈 매도 시그널",
                reasonDetail = "수익 반납을 막기 위한 트레일링 스탑 구간(시그널 ${totalScore}점)입니다. 80% 이상 매도해 이익을 보전하세요.",
            )

            // 5. iNAV 할증 과열
            discrepancyPct >= 0.50 -> HoldingSellSignal(
                position = position,
                currentPrice = currentPrice,
                pnlPct = pnlPct,
                pnlAmountWon = pnlAmountWon,
                drawdownFromHighPct = drawdownFromHighPct,
                action = ExitActionType.OVERHEAT_TRIM,
                urgencyScore = 75,
                recommendedSellRatioPct = 40,
                nextTrailingStopPrice = trailingStopPrice,
                nextTargetPrice = nextTargetPrice,
                headline = "실시간 iNAV 대비 +${String.format(Locale.US, "%.2f", discrepancyPct)}% 비싸게 거래 중 · 단기 매도 찬스",
                reasonDetail = "본질가치(iNAV)보다 시장가가 과도하게 할증된 상태입니다. 고가에 40% 분할 매도 후 괴리율 정상화 시 재매수하면 유리합니다.",
            )

            // 6. 소폭 조정 중이나 시그널 점수 우수 → 눌림목 추가 매수
            pnlPct in -4.5..-1.0 && totalScore >= 68 -> HoldingSellSignal(
                position = position,
                currentPrice = currentPrice,
                pnlPct = pnlPct,
                pnlAmountWon = pnlAmountWon,
                drawdownFromHighPct = drawdownFromHighPct,
                action = ExitActionType.ADD_ON_DIP,
                urgencyScore = 60,
                recommendedSellRatioPct = 0,
                nextTrailingStopPrice = stopLossPrice,
                nextTargetPrice = nextTargetPrice,
                headline = "강세 추세 내 단기 눌림목 · 평단가 낮추기(추가 매수) 적합",
                reasonDetail = "시그널 점수(${totalScore}점)와 PDF 수급이 견조합니다. 매도하지 말고 분할 추가 매수로 목표가 도달을 노리세요.",
            )

            // 7. 기본 보유 유지
            else -> HoldingSellSignal(
                position = position,
                currentPrice = currentPrice,
                pnlPct = pnlPct,
                pnlAmountWon = pnlAmountWon,
                drawdownFromHighPct = drawdownFromHighPct,
                action = ExitActionType.HOLD_POSITION,
                urgencyScore = 30,
                recommendedSellRatioPct = 0,
                nextTrailingStopPrice = maxOf(stopLossPrice, trailingStopPrice),
                nextTargetPrice = nextTargetPrice,
                headline = "추세 순항 중 · 목표가(${formatWonPlain(nextTargetPrice)})까지 보유 유지",
                reasonDetail = "현재 수익률 ${formatPct(pnlPct)}, 시그널 ${totalScore}점입니다. 손절/익절 기준선이 지켜지고 있으니 흔들리지 말고 보유하세요.",
            )
        }
    }

    private fun computeRsi(prices: List<Double>, period: Int = 14): Double {
        if (prices.size < 3) return 50.0
        val diffs = (1 until prices.size).map { prices[it] - prices[it - 1] }.takeLast(period)
        val gains = diffs.filter { it > 0.0 }.sum() / diffs.size
        val losses = diffs.filter { it < 0.0 }.sumOf { abs(it) } / diffs.size
        if (losses == 0.0 && gains == 0.0) return 50.0
        if (losses == 0.0) return 82.0
        val rs = gains / losses
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(10.0, 90.0)
    }

    private fun formatPct(v: Double, suffix: String = "%"): String {
        val sign = if (v > 0) "+" else ""
        return "$sign${String.format(Locale.US, "%.2f", v)}$suffix"
    }

    private fun formatWonPlain(v: Long): String {
        return "${java.text.NumberFormat.getNumberInstance(Locale.KOREA).format(v)}원"
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
