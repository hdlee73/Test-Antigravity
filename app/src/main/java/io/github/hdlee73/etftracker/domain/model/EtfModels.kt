package io.github.hdlee73.etftracker.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class EtfSummary(
    val code: String,
    val name: String,
    val manager: String,
    val category: String,
    val isActive: Boolean,
    val marketPrice: Long,
    val iNav: Double,
    val dailyChangePct: Double,
    val aumBillionWon: Long,
    val tradingValueBillionWon: Double,
    val lpAskQty: Long,
    val lpBidQty: Long,
    val terPct: Double,
    val otherCostPct: Double,
    val brokerageFeePct: Double,
    val annualDividendYieldPct: Double,
    val sharpeRatio: Double,
    val mddPct: Double,
    val trackingErrorPct: Double,
    val holdings: List<PdfHolding>,
    val rocBreakdown: RocBreakdown,
    val dailyPrices: List<Double>,
    val benchmarkPrices: List<Double>,
) {
    val realCostPct: Double get() = terPct + otherCostPct + brokerageFeePct
    val discrepancyPct: Double
        get() = if (iNav > 0.0) ((marketPrice - iNav) / iNav) * 100.0 else 0.0
}

@Immutable
data class PdfHolding(
    val code: String,
    val name: String,
    val weightPct: Double,
    val prevWeightPct: Double,
    val shareCount: Long,
    val prevShareCount: Long,
    val dailyReturnPct: Double,
) {
    val contributionPctPoint: Double get() = (weightPct * dailyReturnPct) / 100.0
    val weightDeltaPct: Double get() = weightPct - prevWeightPct
    val shareDelta: Long get() = shareCount - prevShareCount
}

enum class RebalanceAction(val label: String) {
    ADDED("신규 편입"),
    REMOVED("전량 편출"),
    INCREASED("비중 확대"),
    DECREASED("비중 축소"),
}

@Immutable
data class RebalanceEvent(
    val stockCode: String,
    val stockName: String,
    val action: RebalanceAction,
    val shareDelta: Long,
    val weightDeltaPct: Double,
)

@Immutable
data class OverlapHoldingItem(
    val code: String,
    val name: String,
    val weightA: Double,
    val weightB: Double,
    val minOverlapPct: Double,
    val combinedExposurePct: Double,
)

@Immutable
data class OverlapAnalysis(
    val etfAName: String,
    val etfBName: String,
    val totalOverlapPct: Double,
    val commonCount: Int,
    val items: List<OverlapHoldingItem>,
)

enum class DiscrepancyAlertLevel(val label: String) {
    NORMAL("정상 범위"),
    CAUTION("괴리 확대 주의"),
    OVERHEAT_ALERT("할증·LP 공백 경보"),
}

@Immutable
data class CostComparisonRow(
    val code: String,
    val name: String,
    val manager: String,
    val terPct: Double,
    val otherCostPct: Double,
    val brokerageFeePct: Double,
    val realCostPct: Double,
    val annualCostWon: Long,
)

@Immutable
data class DividendYearPoint(
    val year: Int,
    val trValueWon: Long,
    val cashOutValueWon: Long,
)

@Immutable
data class DividendSimulationResult(
    val principalWon: Long,
    val years: Int,
    val trFinalWon: Long,
    val cashOutFinalWon: Long,
    val compoundGapWon: Long,
    val points: List<DividendYearPoint>,
)

@Immutable
data class RocBreakdown(
    val dividendIncomePct: Double,
    val optionPremiumPct: Double,
    val capitalGainRocPct: Double,
) {
    val isPrincipalErosionRisk: Boolean get() = capitalGainRocPct >= 30.0
}

@Immutable
data class AccountTaxRow(
    val accountName: String,
    val taxRateLabel: String,
    val grossDividendWon: Long,
    val taxWon: Long,
    val netDividendWon: Long,
    val taxSavedWon: Long,
)

enum class NewsSentiment(val label: String) {
    POSITIVE("호재"),
    NEGATIVE("악재"),
    NEUTRAL("중립"),
}

@Immutable
data class NewsImpactItem(
    val id: String,
    val headline: String,
    val source: String,
    val timeLabel: String,
    val sentiment: NewsSentiment,
    val relatedStocks: List<String>,
    val impactSummary: String,
)

@Immutable
data class StressTestRow(
    val code: String,
    val name: String,
    val mddPct: Double,
    val underwaterDays: Int,
    val recovered: Boolean,
    val drawdownSeries: List<Double>,
)

@Immutable
data class CorrelationMatrix(
    val labels: List<String>,
    val values: List<List<Double>>,
)

enum class TradingSignalGrade(val label: String) {
    STRONG_BUY("강력 매수"),
    BUY("분할 매수"),
    HOLD("관망 · 보유"),
    TRIM("분할 매도"),
    SELL("비중 축소"),
}

@Immutable
data class SignalFactor(
    val name: String,
    val score: Int,
    val summary: String,
)

@Immutable
data class TradingSignalResult(
    val code: String,
    val name: String,
    val grade: TradingSignalGrade,
    val totalScore: Int,
    val targetActionText: String,
    val factors: List<SignalFactor>,
)
