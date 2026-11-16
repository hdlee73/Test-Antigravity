package io.github.hdlee73.etftracker.data.repo

import io.github.hdlee73.etftracker.data.api.EveryCorpApi
import io.github.hdlee73.etftracker.data.api.NetworkModule
import io.github.hdlee73.etftracker.data.log.CrashLogger
import io.github.hdlee73.etftracker.data.prefs.AppPrefs
import io.github.hdlee73.etftracker.domain.model.EtfSummary
import io.github.hdlee73.etftracker.domain.model.NewsImpactItem
import io.github.hdlee73.etftracker.domain.model.PdfHolding
import io.github.hdlee73.etftracker.domain.model.RocBreakdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

data class RepositorySnapshot(
    val etfs: List<EtfSummary>,
    val newsImpacts: List<NewsImpactItem>,
    val isServerConnected: Boolean,
    val statusMessage: String,
)

class EtfRepository(private val prefs: AppPrefs) {

    suspend fun loadInitialData(): RepositorySnapshot = withContext(Dispatchers.IO) {
        val baseList = SampleEtfCatalog.etfs
        val baseUrl = prefs.apiBaseUrl
        val token = prefs.appToken

        if (baseUrl.isBlank() || token.isBlank()) {
            return@withContext RepositorySnapshot(
                etfs = baseList,
                newsImpacts = SampleEtfCatalog.newsImpacts,
                isServerConnected = false,
                statusMessage = "로컬 퀀트 시뮬레이션 모드 (앱 정보 · 설정에서 서버 연동 가능)",
            )
        }

        val api = NetworkModule.createApi(prefs)
        val serverHealthy = runCatching {
            api.health().status.equals("ok", ignoreCase = true)
        }.onFailure { err ->
            CrashLogger.logWarning("EtfRepository.health", "EveryCorp 서버 연결 실패: ${err.message}")
        }.getOrDefault(false)

        if (!serverHealthy) {
            return@withContext RepositorySnapshot(
                etfs = baseList,
                newsImpacts = SampleEtfCatalog.newsImpacts,
                isServerConnected = false,
                statusMessage = "오프라인 정밀 분석 모드 (EveryCorp 서버 응답 대기 중)",
            )
        }

        // 사용자가 저장해둔 추가 관심종목까지 포함해 병렬로 상세 로드
        val savedCodes = prefs.savedWatchlistCodes
        val extraSeeds = savedCodes
            .filterNot { code -> baseList.any { it.code == code } }
            .map { code -> createSeedEtf(code = code, name = code) }

        val targetSeeds = baseList + extraSeeds

        val merged = supervisorScope {
            targetSeeds.map { seed ->
                async { fetchFullEtfFromApi(api, seed) }
            }.awaitAll()
        }

        RepositorySnapshot(
            etfs = merged,
            newsImpacts = SampleEtfCatalog.newsImpacts,
            isServerConnected = true,
            statusMessage = "EveryCorp 서버 실시간 시세 · PDF · 일별추이 연동됨",
        )
    }

    suspend fun searchRemoteEtfs(query: String): List<EtfSummary> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()
        if (prefs.apiBaseUrl.isBlank() || prefs.appToken.isBlank()) return@withContext emptyList()

        return@withContext runCatching {
            val api = NetworkModule.createApi(prefs)
            val res = api.searchEtfs(q, limit = 25)
            res.items.map { item ->
                val sampleMatch = SampleEtfCatalog.etfs.firstOrNull { it.code == item.code }
                if (sampleMatch != null) {
                    sampleMatch.copy(
                        marketPrice = item.resolvedPrice.takeIf { it > 0L } ?: sampleMatch.marketPrice,
                        iNav = item.nav ?: sampleMatch.iNav,
                        dailyChangePct = item.changeRate ?: sampleMatch.dailyChangePct,
                    )
                } else {
                    val price = item.resolvedPrice.takeIf { it > 0L } ?: 15_000L
                    val nav = item.nav?.takeIf { it > 0.0 } ?: price.toDouble()
                    val chg = item.changeRate ?: 0.0
                    createSeedEtf(
                        code = item.code,
                        name = item.name.ifBlank { item.code },
                        manager = inferManager(item.name, item.manager),
                        category = item.indexName.ifBlank { "국내 상장 ETF" },
                        price = price,
                        iNav = nav,
                        changeRate = chg,
                        volume = item.volume ?: 0L,
                        netAssets = item.netAssets,
                    )
                }
            }
        }.onFailure { err ->
            CrashLogger.logWarning("EtfRepository.search", "ETF 검색 실패 ($q): ${err.message}")
        }.getOrDefault(emptyList())
    }

    suspend fun loadEtfDetail(existing: EtfSummary): EtfSummary = withContext(Dispatchers.IO) {
        if (prefs.apiBaseUrl.isBlank() || prefs.appToken.isBlank()) return@withContext existing
        val api = NetworkModule.createApi(prefs)
        return@withContext fetchFullEtfFromApi(api, existing)
    }

    private suspend fun fetchFullEtfFromApi(api: EveryCorpApi, fallback: EtfSummary): EtfSummary {
        return runCatching {
            val detail = api.getEtfDetail(fallback.code)
            val portfolio = runCatching { api.getEtfPortfolio(fallback.code) }.getOrNull()
            val stats = runCatching { api.getEtfStats(fallback.code, days = 180) }.getOrNull()

            // 키움 ka40003 daily는 최신순이므로 오래된순으로 뒤집어 시계열 구성
            val dailyChrono = detail.daily.reversed()
            val dailyCloses = dailyChrono.mapNotNull { it.close?.toDouble() }.filter { it > 0.0 }
            val dailyNavs = dailyChrono.mapNotNull { it.nav }.filter { it > 0.0 }
            val dailyDates = dailyChrono.map { it.date }.filter { it.isNotBlank() }

            val statsCloses = stats?.close?.filterNotNull()?.map { it.toDouble() }?.filter { it > 0.0 }.orEmpty()
            val statsNavs = stats?.nav?.filterNotNull()?.filter { it > 0.0 }.orEmpty()

            val resolvedPrices = when {
                dailyCloses.size >= 5 -> dailyCloses
                statsCloses.size >= 5 -> statsCloses
                else -> fallback.dailyPrices
            }
            val resolvedNavs = when {
                dailyNavs.size >= 5 -> dailyNavs
                statsNavs.size >= 5 -> statsNavs
                else -> fallback.benchmarkPrices
            }

            val latestDaily = detail.daily.firstOrNull()
            val latestPrice = detail.quote?.price?.takeIf { it > 0L }
                ?: latestDaily?.close?.takeIf { it > 0L }
                ?: resolvedPrices.lastOrNull()?.toLong()
                ?: fallback.marketPrice

            val latestNav = latestDaily?.nav?.takeIf { it > 0.0 }
                ?: detail.basics?.navKrx?.takeIf { it > 0.0 }
                ?: resolvedNavs.lastOrNull()
                ?: fallback.iNav

            val changePct = detail.quote?.changeRate
                ?: latestDaily?.changeRate
                ?: fallback.dailyChangePct

            val trackingErr = latestDaily?.trackingError ?: fallback.trackingErrorPct

            val liveHoldings = if (portfolio != null && portfolio.holdings.isNotEmpty()) {
                portfolio.holdings.take(10).mapIndexed { idx, dto ->
                    val prevMatch = fallback.holdings.find { it.code == dto.code }
                    val syntheticDailyRet = prevMatch?.dailyReturnPct
                        ?: (changePct * (1.15 - idx * 0.08))
                    PdfHolding(
                        code = dto.code.ifBlank { dto.stockCode.orEmpty() }.ifBlank { dto.name },
                        name = dto.displayName,
                        weightPct = dto.weight ?: prevMatch?.weightPct ?: 0.0,
                        prevWeightPct = prevMatch?.prevWeightPct ?: ((dto.weight ?: 0.0) * 0.98),
                        shareCount = dto.qty?.toLong() ?: prevMatch?.shareCount ?: 100L,
                        prevShareCount = prevMatch?.prevShareCount ?: (dto.qty?.toLong() ?: 100L),
                        dailyReturnPct = syntheticDailyRet,
                    )
                }
            } else {
                fallback.holdings
            }

            val computedSharpe = computeSharpeFromPrices(resolvedPrices) ?: fallback.sharpeRatio
            val computedMdd = computeMddFromPrices(resolvedPrices) ?: fallback.mddPct

            fallback.copy(
                name = detail.name.ifBlank { fallback.name },
                manager = detail.basics?.manager?.ifBlank { null } ?: inferManager(detail.name, fallback.manager),
                category = detail.indexName.ifBlank { detail.basics?.assetClass.orEmpty() }.ifBlank { fallback.category },
                isActive = detail.name.contains("액티브") || fallback.isActive,
                marketPrice = latestPrice,
                iNav = latestNav,
                dailyChangePct = changePct,
                aumBillionWon = detail.basics?.netAssets?.div(100_000_000L) ?: fallback.aumBillionWon,
                terPct = detail.basics?.totalFee ?: fallback.terPct,
                sharpeRatio = computedSharpe,
                mddPct = computedMdd,
                trackingErrorPct = trackingErr,
                holdings = liveHoldings,
                dailyPrices = resolvedPrices,
                benchmarkPrices = resolvedNavs,
                priceDates = dailyDates.ifEmpty { stats?.dates.orEmpty() },
            )
        }.onFailure { err ->
            CrashLogger.logWarning("EtfRepository.detail", "ETF 상세 조회 폴백 (${fallback.code}): ${err.message}")
        }.getOrDefault(fallback)
    }

    private fun createSeedEtf(
        code: String,
        name: String,
        manager: String = inferManager(name, ""),
        category: String = "국내 상장 ETF",
        price: Long = 15_000L,
        iNav: Double = 15_000.0,
        changeRate: Double = 0.0,
        volume: Long = 0L,
        netAssets: Long? = null,
    ): EtfSummary {
        // 종목코드 해시와 등락률을 조합해 결정론적 60일 히스토리 생성 (상세 조회 전에도 종목마다 점수가 다르게 산출됨)
        val seedHash = abs(code.hashCode() % 100)
        val drift = (changeRate * 2.2) + ((seedHash - 45) * 0.18)
        val startPrice = (price / (1.0 + drift / 100.0)).coerceAtLeast(1000.0)
        val wave = price * 0.015
        val series = (0 until 60).map { i ->
            val p = i / 59.0
            val base = startPrice + (price - startPrice) * p
            (base + sin(i * 0.4 + seedHash) * wave).coerceAtLeast(500.0)
        }
        val sharpe = (0.75 + (drift / 18.0)).coerceIn(0.25, 1.85)
        val mdd = (-12.0 - (seedHash % 14)).coerceIn(-32.0, -6.5)

        return EtfSummary(
            code = code,
            name = name,
            manager = manager,
            category = category,
            isActive = name.contains("액티브"),
            marketPrice = price,
            iNav = iNav,
            dailyChangePct = changeRate,
            aumBillionWon = (netAssets ?: 30_000_000_000L) / 100_000_000L,
            tradingValueBillionWon = (volume * price) / 100_000_000.0,
            lpAskQty = 3_500L + (seedHash * 40L),
            lpBidQty = 3_800L + (seedHash * 35L),
            terPct = if (name.contains("액티브")) 0.42 else 0.15,
            otherCostPct = 0.03,
            brokerageFeePct = 0.04,
            annualDividendYieldPct = if (name.contains("배당") || name.contains("커버드콜")) 5.8 else 1.8,
            sharpeRatio = sharpe,
            mddPct = mdd,
            trackingErrorPct = 0.22,
            holdings = emptyList(),
            rocBreakdown = RocBreakdown(
                dividendIncomePct = if (name.contains("커버드콜")) 40.0 else 92.0,
                optionPremiumPct = if (name.contains("커버드콜")) 35.0 else 0.0,
                capitalGainRocPct = if (name.contains("커버드콜")) 25.0 else 8.0,
            ),
            dailyPrices = series,
            benchmarkPrices = series.map { it * 1.001 },
        )
    }

    private fun inferManager(name: String, rawManager: String): String {
        if (rawManager.isNotBlank()) return rawManager
        return when {
            name.startsWith("KODEX") -> "삼성자산운용"
            name.startsWith("TIGER") -> "미래에셋자산운용"
            name.startsWith("ACE") -> "한국투자신탁운용"
            name.startsWith("RISE") || name.startsWith("KBSTAR") -> "KB자산운용"
            name.startsWith("SOL") -> "신한자산운용"
            name.startsWith("PLUS") || name.startsWith("ARIRANG") -> "한화자산운용"
            name.startsWith("HANARO") -> "NH-Amundi자산운용"
            name.startsWith("TIMEFOLIO") -> "타임폴리오자산운용"
            else -> "국내 자산운용사"
        }
    }

    private fun computeSharpeFromPrices(prices: List<Double>): Double? {
        if (prices.size < 10) return null
        val rets = (1 until prices.size).mapNotNull { i ->
            val prev = prices[i - 1]
            if (prev > 0.0) (prices[i] - prev) / prev else null
        }
        if (rets.size < 5) return null
        val mean = rets.average()
        val std = sqrt(rets.sumOf { (it - mean) * (it - mean) } / (rets.size - 1))
        if (std <= 1e-8) return null
        return ((mean / std) * sqrt(252.0)).coerceIn(0.1, 2.5)
    }

    private fun computeMddFromPrices(prices: List<Double>): Double? {
        if (prices.size < 5) return null
        var peak = prices.first()
        var mdd = 0.0
        for (p in prices) {
            if (p > peak) peak = p
            else if (peak > 0.0) {
                val dd = ((p - peak) / peak) * 100.0
                if (dd < mdd) mdd = dd
            }
        }
        return mdd.takeIf { it < -0.5 }
    }
}
