package io.github.hdlee73.etftracker.data.repo

import io.github.hdlee73.etftracker.domain.model.EtfSummary
import io.github.hdlee73.etftracker.domain.model.HoldingPosition
import io.github.hdlee73.etftracker.domain.model.NewsImpactItem
import io.github.hdlee73.etftracker.domain.model.NewsSentiment
import io.github.hdlee73.etftracker.domain.model.PdfHolding
import io.github.hdlee73.etftracker.domain.model.RocBreakdown
import kotlin.math.sin

object SampleEtfCatalog {

    val defaultHoldings: List<HoldingPosition> = listOf(
        HoldingPosition(
            code = "360750",
            name = "TIGER 미국S&P500",
            avgBuyPrice = 17_850L,
            quantity = 120,
            targetProfitPct = 12.0,
            stopLossPct = -6.0,
            highestPriceSinceBuy = 20_650L,
        ),
        HoldingPosition(
            code = "091180",
            name = "KODEX 자동차",
            avgBuyPrice = 23_900L,
            quantity = 85,
            targetProfitPct = 10.0,
            stopLossPct = -6.0,
            highestPriceSinceBuy = 24_300L,
        ),
        HoldingPosition(
            code = "441640",
            name = "KODEX 미국배당프리미엄액티브",
            avgBuyPrice = 10_620L,
            quantity = 200,
            targetProfitPct = 8.0,
            stopLossPct = -5.0,
            highestPriceSinceBuy = 11_480L,
        ),
    )

    val etfs: List<EtfSummary> = listOf(
        // 1. 강력 매수 후보 (~84점): 미국S&P500
        EtfSummary(
            code = "360750",
            name = "TIGER 미국S&P500",
            manager = "미래에셋자산운용",
            category = "미국 대표지수 (UH)",
            isActive = false,
            marketPrice = 20_480L,
            iNav = 20_525.0, // iNAV 대비 -0.22% 저평가 할인 (매수 찬스)
            dailyChangePct = 1.48,
            aumBillionWon = 6_290L,
            tradingValueBillionWon = 182.3,
            lpAskQty = 9_800L,
            lpBidQty = 11_100L,
            terPct = 0.07,
            otherCostPct = 0.04,
            brokerageFeePct = 0.03,
            annualDividendYieldPct = 1.35,
            sharpeRatio = 1.48,
            mddPct = -10.8,
            trackingErrorPct = 0.18,
            holdings = listOf(
                PdfHolding("AAPL", "Apple", 7.2, 7.0, 640, 620, 1.85),
                PdfHolding("NVDA", "NVIDIA", 7.0, 6.4, 980, 910, 3.40),
                PdfHolding("MSFT", "Microsoft", 6.5, 6.4, 320, 310, 1.15),
                PdfHolding("AMZN", "Amazon", 3.9, 3.8, 420, 410, 1.60),
                PdfHolding("META", "Meta Platforms", 2.6, 2.5, 115, 110, 0.95),
                PdfHolding("BRK.B", "Berkshire Hathaway", 1.8, 1.8, 95, 95, 0.55),
            ),
            rocBreakdown = RocBreakdown(dividendIncomePct = 94.0, optionPremiumPct = 0.0, capitalGainRocPct = 6.0),
            dailyPrices = generateSeries(start = 17_200.0, end = 20_480.0, count = 60, waveAmp = 180.0, phase = 0.4),
            benchmarkPrices = generateSeries(start = 17_200.0, end = 20_520.0, count = 60, waveAmp = 175.0, phase = 0.4),
        ),
        // 2. 강력 매수 후보 (~80점): 코리아밸류업
        EtfSummary(
            code = "496120",
            name = "TIGER 코리아밸류업",
            manager = "미래에셋자산운용",
            category = "국내 밸류업·저PBR",
            isActive = false,
            marketPrice = 11_540L,
            iNav = 11_560.0, // 소폭 할인 (-0.17%)
            dailyChangePct = 1.32,
            aumBillionWon = 840L,
            tradingValueBillionWon = 38.6,
            lpAskQty = 8_500L,
            lpBidQty = 9_200L,
            terPct = 0.008,
            otherCostPct = 0.03,
            brokerageFeePct = 0.03,
            annualDividendYieldPct = 3.60,
            sharpeRatio = 1.26,
            mddPct = -11.4,
            trackingErrorPct = 0.14,
            holdings = listOf(
                PdfHolding("000660", "SK하이닉스", 14.8, 14.1, 1_120, 1_050, 2.85),
                PdfHolding("005930", "삼성전자", 14.2, 14.0, 2_100, 2_100, 1.45),
                PdfHolding("105560", "KB금융", 9.4, 8.9, 1_260, 1_180, 2.40),
                PdfHolding("005380", "현대차", 8.9, 8.6, 760, 740, 1.90),
                PdfHolding("055550", "신한지주", 6.2, 5.8, 1_180, 1_120, 1.75),
                PdfHolding("000270", "기아", 6.1, 6.2, 890, 890, 0.85),
            ),
            rocBreakdown = RocBreakdown(dividendIncomePct = 96.0, optionPremiumPct = 0.0, capitalGainRocPct = 4.0),
            dailyPrices = generateSeries(start = 10_150.0, end = 11_540.0, count = 60, waveAmp = 110.0, phase = 0.8),
            benchmarkPrices = generateSeries(start = 10_150.0, end = 11_560.0, count = 60, waveAmp = 108.0, phase = 0.8),
        ),
        // 3. 분할 매수 후보 (~71점): 미국배당다우존스
        EtfSummary(
            code = "458730",
            name = "TIGER 미국배당다우존스",
            manager = "미래에셋자산운용",
            category = "월배당·배당성장",
            isActive = false,
            marketPrice = 12_680L,
            iNav = 12_675.0,
            dailyChangePct = 0.68,
            aumBillionWon = 2_480L,
            tradingValueBillionWon = 64.8,
            lpAskQty = 8_200L,
            lpBidQty = 7_900L,
            terPct = 0.01,
            otherCostPct = 0.04,
            brokerageFeePct = 0.03,
            annualDividendYieldPct = 3.85,
            sharpeRatio = 1.22,
            mddPct = -9.8,
            trackingErrorPct = 0.17,
            holdings = listOf(
                PdfHolding("AVGO", "Broadcom", 4.6, 4.3, 220, 210, 1.65),
                PdfHolding("HD", "Home Depot", 4.2, 4.1, 190, 185, 0.85),
                PdfHolding("ABBV", "AbbVie", 4.1, 3.9, 330, 320, 1.10),
                PdfHolding("KO", "Coca-Cola", 3.9, 3.9, 980, 980, 0.45),
                PdfHolding("PEP", "PepsiCo", 3.8, 3.9, 340, 340, 0.20),
            ),
            rocBreakdown = RocBreakdown(dividendIncomePct = 91.0, optionPremiumPct = 0.0, capitalGainRocPct = 9.0),
            dailyPrices = generateSeries(start = 11_650.0, end = 12_680.0, count = 60, waveAmp = 95.0, phase = 1.2),
            benchmarkPrices = generateSeries(start = 11_650.0, end = 12_690.0, count = 60, waveAmp = 92.0, phase = 1.2),
        ),
        // 4. 관망·보유 후보 (~58점): KODEX 200
        EtfSummary(
            code = "069500",
            name = "KODEX 200",
            manager = "삼성자산운용",
            category = "국내 대표지수",
            isActive = false,
            marketPrice = 34_820L,
            iNav = 34_830.0,
            dailyChangePct = 0.18,
            aumBillionWon = 6_420L,
            tradingValueBillionWon = 184.5,
            lpAskQty = 12_400L,
            lpBidQty = 11_850L,
            terPct = 0.15,
            otherCostPct = 0.02,
            brokerageFeePct = 0.04,
            annualDividendYieldPct = 1.92,
            sharpeRatio = 0.82,
            mddPct = -16.4,
            trackingErrorPct = 0.14,
            holdings = listOf(
                PdfHolding("005930", "삼성전자", 24.8, 24.9, 4_250, 4_250, -0.35),
                PdfHolding("000660", "SK하이닉스", 9.6, 9.3, 1_120, 1_100, 1.80),
                PdfHolding("005380", "현대차", 3.8, 3.8, 380, 380, 0.40),
                PdfHolding("000270", "기아", 2.9, 3.0, 510, 510, -0.65),
                PdfHolding("105560", "KB금융", 3.4, 3.3, 620, 620, 0.90),
                PdfHolding("035420", "NAVER", 2.7, 2.8, 240, 240, -0.80),
                PdfHolding("068270", "셀트리온", 2.5, 2.5, 210, 210, 0.25),
                PdfHolding("055550", "신한지주", 2.4, 2.4, 590, 590, 0.50),
            ),
            rocBreakdown = RocBreakdown(dividendIncomePct = 96.0, optionPremiumPct = 0.0, capitalGainRocPct = 4.0),
            dailyPrices = generateSeries(start = 34_400.0, end = 34_820.0, count = 60, waveAmp = 620.0, phase = 2.1),
            benchmarkPrices = generateSeries(start = 34_400.0, end = 34_830.0, count = 60, waveAmp = 610.0, phase = 2.1),
        ),
        // 5. 분할 매도 후보 (~41점): 미국배당프리미엄액티브 (커버드콜 ROC 원금잠식 + 고점 대비 꺾임)
        EtfSummary(
            code = "441640",
            name = "KODEX 미국배당프리미엄액티브",
            manager = "삼성자산운용",
            category = "액티브·커버드콜",
            isActive = true,
            marketPrice = 10_890L,
            iNav = 10_820.0, // +0.65% 할증
            dailyChangePct = -0.75,
            aumBillionWon = 950L,
            tradingValueBillionWon = 31.4,
            lpAskQty = 620L,
            lpBidQty = 580L,
            terPct = 0.43,
            otherCostPct = 0.06,
            brokerageFeePct = 0.12,
            annualDividendYieldPct = 8.45,
            sharpeRatio = 0.64,
            mddPct = -17.5,
            trackingErrorPct = 1.85,
            holdings = listOf(
                PdfHolding("MSFT", "Microsoft", 6.5, 6.8, 500, 540, -0.92),
                PdfHolding("AVGO", "Broadcom", 5.2, 5.5, 360, 390, -1.10),
                PdfHolding("JPM", "JPMorgan Chase", 4.8, 4.8, 610, 610, -0.35),
                PdfHolding("XOM", "ExxonMobil", 3.6, 4.9, 440, 620, -1.60),
                PdfHolding("LLY", "Eli Lilly", 3.1, 0.0, 140, 0, 0.85),
                PdfHolding("INTC", "Intel", 0.0, 2.2, 0, 890, -2.90),
            ),
            rocBreakdown = RocBreakdown(dividendIncomePct = 34.0, optionPremiumPct = 31.0, capitalGainRocPct = 35.0),
            dailyPrices = generatePeakAndDropSeries(start = 11_100.0, peak = 11_520.0, end = 10_890.0, count = 60),
            benchmarkPrices = generatePeakAndDropSeries(start = 11_100.0, peak = 11_500.0, end = 10_820.0, count = 60),
        ),
        // 6. 비중 축소/손절 후보 (~27점): KODEX 자동차 (하락 추세 + LP 호가 공백 + iNAV 할증)
        EtfSummary(
            code = "091180",
            name = "KODEX 자동차",
            manager = "삼성자산운용",
            category = "국내 섹터·모빌리티",
            isActive = false,
            marketPrice = 22_390L,
            iNav = 22_160.0, // +1.04% 과열 할증
            dailyChangePct = -1.85,
            aumBillionWon = 460L,
            tradingValueBillionWon = 18.2,
            lpAskQty = 280L,
            lpBidQty = 310L,
            terPct = 0.45,
            otherCostPct = 0.05,
            brokerageFeePct = 0.09,
            annualDividendYieldPct = 2.65,
            sharpeRatio = 0.42,
            mddPct = -24.8,
            trackingErrorPct = 0.51,
            holdings = listOf(
                PdfHolding("005380", "현대차", 23.4, 24.1, 1_410, 1_450, -2.40),
                PdfHolding("000270", "기아", 21.8, 22.3, 2_120, 2_180, -2.15),
                PdfHolding("012330", "현대모비스", 15.2, 15.4, 840, 860, -1.50),
                PdfHolding("000240", "한국앤컴퍼니", 5.8, 5.9, 1_850, 1_900, -1.15),
                PdfHolding("161390", "한국타이어앤테크놀로지", 5.4, 5.4, 1_320, 1_320, -0.85),
                PdfHolding("105560", "KB금융", 1.2, 1.2, 180, 180, 0.40),
            ),
            rocBreakdown = RocBreakdown(dividendIncomePct = 94.0, optionPremiumPct = 0.0, capitalGainRocPct = 6.0),
            dailyPrices = generateSeries(start = 25_200.0, end = 22_390.0, count = 60, waveAmp = 340.0, phase = 0.2),
            benchmarkPrices = generateSeries(start = 25_200.0, end = 22_160.0, count = 60, waveAmp = 330.0, phase = 0.2),
        ),
        // 7. 강력 매수 후보 (~86점): 미국필라델피아반도체나스닥
        EtfSummary(
            code = "381180",
            name = "TIGER 미국필라델피아반도체나스닥",
            manager = "미래에셋자산운용",
            category = "글로벌 AI·반도체",
            isActive = false,
            marketPrice = 19_240L,
            iNav = 19_290.0, // -0.26% 할인
            dailyChangePct = 2.45,
            aumBillionWon = 2_890L,
            tradingValueBillionWon = 115.4,
            lpAskQty = 9_400L,
            lpBidQty = 10_200L,
            terPct = 0.49,
            otherCostPct = 0.05,
            brokerageFeePct = 0.04,
            annualDividendYieldPct = 0.65,
            sharpeRatio = 1.54,
            mddPct = -15.2,
            trackingErrorPct = 0.28,
            holdings = listOf(
                PdfHolding("NVDA", "NVIDIA", 12.4, 11.6, 1_450, 1_380, 3.40),
                PdfHolding("AVGO", "Broadcom", 10.8, 10.2, 820, 790, 2.20),
                PdfHolding("AMD", "AMD", 7.5, 7.3, 610, 600, 1.95),
                PdfHolding("TSM", "TSMC", 7.2, 6.9, 540, 510, 2.60),
                PdfHolding("ASML", "ASML", 4.8, 4.9, 190, 190, 0.80),
            ),
            rocBreakdown = RocBreakdown(dividendIncomePct = 95.0, optionPremiumPct = 0.0, capitalGainRocPct = 5.0),
            dailyPrices = generateSeries(start = 15_400.0, end = 19_240.0, count = 60, waveAmp = 280.0, phase = 0.5),
            benchmarkPrices = generateSeries(start = 15_400.0, end = 19_290.0, count = 60, waveAmp = 275.0, phase = 0.5),
        ),
        // 8. 비중 축소 후보 (~31점): 2차전지산업
        EtfSummary(
            code = "305720",
            name = "KODEX 2차전지산업",
            manager = "삼성자산운용",
            category = "국내 2차전지·배터리",
            isActive = false,
            marketPrice = 14_120L,
            iNav = 14_010.0, // +0.78% 할증
            dailyChangePct = -2.35,
            aumBillionWon = 1_190L,
            tradingValueBillionWon = 68.5,
            lpAskQty = 410L,
            lpBidQty = 390L,
            terPct = 0.45,
            otherCostPct = 0.04,
            brokerageFeePct = 0.08,
            annualDividendYieldPct = 0.40,
            sharpeRatio = 0.28,
            mddPct = -34.2,
            trackingErrorPct = 0.45,
            holdings = listOf(
                PdfHolding("373220", "LG에너지솔루션", 21.5, 22.0, 480, 495, -2.60),
                PdfHolding("005490", "POSCO홀딩스", 16.8, 17.2, 610, 625, -2.90),
                PdfHolding("006400", "삼성SDI", 13.4, 13.8, 420, 435, -1.95),
                PdfHolding("051910", "LG화학", 11.2, 11.5, 380, 390, -1.80),
                PdfHolding("247540", "에코프로비엠", 8.9, 9.2, 520, 540, -3.10),
            ),
            rocBreakdown = RocBreakdown(dividendIncomePct = 95.0, optionPremiumPct = 0.0, capitalGainRocPct = 5.0),
            dailyPrices = generateSeries(start = 17_600.0, end = 14_120.0, count = 60, waveAmp = 390.0, phase = 0.1),
            benchmarkPrices = generateSeries(start = 17_600.0, end = 14_010.0, count = 60, waveAmp = 380.0, phase = 0.1),
        ),
    )

    val newsImpacts: List<NewsImpactItem> = listOf(
        NewsImpactItem(
            id = "n1",
            headline = "엔비디아·TSMC 차세대 AI 서버 랙 출하량 가이던스 25% 상향",
            source = "연합인포맥스",
            timeLabel = "12분 전",
            sentiment = NewsSentiment.POSITIVE,
            relatedStocks = listOf("NVIDIA", "TSMC", "SK하이닉스"),
            impactSummary = "TIGER 미국필라델피아반도체(86점), TIGER 미국S&P500(84점) 강력 매수 시그널 강화",
        ),
        NewsImpactItem(
            id = "n2",
            headline = "국내 금융지주·밸류업 지수 편입 기업 자사주 소각 규모 사상 최대",
            source = "한국경제",
            timeLabel = "35분 전",
            sentiment = NewsSentiment.POSITIVE,
            relatedStocks = listOf("KB금융", "신한지주", "삼성전자"),
            impactSummary = "TIGER 코리아밸류업(비중 45%) 분배금 및 NAV 상승 모멘텀 지속",
        ),
        NewsImpactItem(
            id = "n3",
            headline = "유럽·북미 전기차 보조금 축소 우려에 2차전지·완성차 단기 수급 약세",
            source = "매일경제",
            timeLabel = "1시간 전",
            sentiment = NewsSentiment.NEGATIVE,
            relatedStocks = listOf("LG에너지솔루션", "현대차", "기아"),
            impactSummary = "KODEX 2차전지산업 · KODEX 자동차 추세 이탈 및 손절 시그널 유의",
        ),
    )

    val correlationSeries: Map<String, List<Double>> = mapOf(
        "KODEX 200" to etfs.first { it.code == "069500" }.dailyPrices.takeLast(20),
        "S&P500(UH)" to etfs.first { it.code == "360750" }.dailyPrices.takeLast(20),
        "반도체(UH)" to etfs.first { it.code == "381180" }.dailyPrices.takeLast(20),
        "원/달러 환율" to generateSeries(1335.0, 1396.0, 20, 8.0, 0.3),
        "2차전지" to etfs.first { it.code == "305720" }.dailyPrices.takeLast(20),
    )

    private fun generateSeries(
        start: Double,
        end: Double,
        count: Int,
        waveAmp: Double,
        phase: Double,
    ): List<Double> {
        return (0 until count).map { i ->
            val progress = i.toDouble() / (count - 1).coerceAtLeast(1)
            val linear = start + (end - start) * progress
            val wave = sin(i * 0.45 + phase) * waveAmp * (1.0 - progress * 0.3)
            (linear + wave).coerceAtLeast(100.0)
        }
    }

    private fun generatePeakAndDropSeries(
        start: Double,
        peak: Double,
        end: Double,
        count: Int,
    ): List<Double> {
        val mid = (count * 0.65).toInt()
        return (0 until count).map { i ->
            val base = if (i <= mid) {
                val p = i.toDouble() / mid.coerceAtLeast(1)
                start + (peak - start) * p
            } else {
                val p = (i - mid).toDouble() / (count - 1 - mid).coerceAtLeast(1)
                peak + (end - peak) * p
            }
            base + sin(i * 0.5) * 35.0
        }
    }
}
