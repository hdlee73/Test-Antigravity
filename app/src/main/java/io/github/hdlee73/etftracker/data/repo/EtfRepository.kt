package io.github.hdlee73.etftracker.data.repo

import io.github.hdlee73.etftracker.data.api.NetworkModule
import io.github.hdlee73.etftracker.data.log.CrashLogger
import io.github.hdlee73.etftracker.data.prefs.AppPrefs
import io.github.hdlee73.etftracker.domain.model.EtfSummary
import io.github.hdlee73.etftracker.domain.model.NewsImpactItem
import io.github.hdlee73.etftracker.domain.model.PdfHolding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                statusMessage = "로컬 정밀 분석 모드 (앱 정보 · 설정에서 서버 URL 및 토큰 입력 시 실시간 연동)",
            )
        }

        // 로컬 에뮬레이터 기본값 그대로이거나 미설정이면 서버 연결 시도 후 안전하게 폴백
        val serverHealthy = runCatching {
            val api = NetworkModule.createApi(prefs)
            val health = api.health()
            health.status.equals("ok", ignoreCase = true)
        }.onFailure { err ->
            CrashLogger.logWarning("EtfRepository.health", "EveryCorp 서버 연결 실패 ($baseUrl): ${err.message}")
        }.getOrDefault(false)

        if (!serverHealthy) {
            return@withContext RepositorySnapshot(
                etfs = baseList,
                newsImpacts = SampleEtfCatalog.newsImpacts,
                isServerConnected = false,
                statusMessage = "로컬 정밀 분석 모드 (EveryCorp 서버 미연결 · 설정에서 주소/토큰 입력 가능)",
            )
        }

        // EveryCorp 서버 연결 성공 시, 주요 ETF의 최신 기초자료 및 PDF 구성종목을 병합
        val api = NetworkModule.createApi(prefs)
        val merged = baseList.map { fallback ->
            runCatching {
                val basics = api.getEtfBasics(fallback.code)
                val portfolio = api.getEtfPortfolio(fallback.code)
                val stats = api.getEtfStats(fallback.code)
                val latestClose = stats.close.lastOrNull { it != null } ?: fallback.marketPrice
                val latestNav = basics.navKrx ?: stats.nav.lastOrNull { it != null } ?: fallback.iNav
                val liveHoldings = if (portfolio.holdings.isNotEmpty()) {
                    portfolio.holdings.take(10).map { dto ->
                        val prevMatch = fallback.holdings.find { it.code == dto.code }
                        PdfHolding(
                            code = dto.code.ifBlank { dto.stockCode.orEmpty() }.ifBlank { dto.name },
                            name = dto.displayName,
                            weightPct = dto.weight ?: prevMatch?.weightPct ?: 0.0,
                            prevWeightPct = prevMatch?.prevWeightPct ?: (dto.weight ?: 0.0),
                            shareCount = dto.qty?.toLong() ?: prevMatch?.shareCount ?: 0L,
                            prevShareCount = prevMatch?.prevShareCount ?: (dto.qty?.toLong() ?: 0L),
                            dailyReturnPct = prevMatch?.dailyReturnPct ?: 0.0,
                        )
                    }
                } else {
                    fallback.holdings
                }
                fallback.copy(
                    manager = basics.manager.ifBlank { fallback.manager },
                    marketPrice = latestClose,
                    iNav = latestNav,
                    terPct = basics.totalFee ?: fallback.terPct,
                    holdings = liveHoldings,
                )
            }.getOrElse { fallback }
        }

        return@withContext RepositorySnapshot(
            etfs = merged,
            newsImpacts = SampleEtfCatalog.newsImpacts,
            isServerConnected = true,
            statusMessage = "EveryCorp 서버 실시간 연동됨",
        )
    }

    suspend fun searchRemoteEtfs(query: String): List<EtfSummary> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty() || prefs.apiBaseUrl.isBlank() || prefs.appToken.isBlank()) return@withContext emptyList()
        return@withContext runCatching {
            val api = NetworkModule.createApi(prefs)
            val res = api.searchEtfs(q)
            res.items.map { item ->
                val existing = SampleEtfCatalog.etfs.firstOrNull { it.code == item.code }
                existing ?: EtfSummary(
                    code = item.code,
                    name = item.name.ifBlank { item.code },
                    manager = item.manager.ifBlank { "운용사 정보" },
                    category = "국내 상장 ETF",
                    isActive = item.name.contains("액티브"),
                    marketPrice = item.close ?: 0L,
                    iNav = item.nav ?: (item.close?.toDouble() ?: 0.0),
                    dailyChangePct = 0.0,
                    aumBillionWon = (item.netAssets ?: 0L) / 100_000_000L,
                    tradingValueBillionWon = 0.0,
                    lpAskQty = 0L,
                    lpBidQty = 0L,
                    terPct = 0.15,
                    otherCostPct = 0.03,
                    brokerageFeePct = 0.03,
                    annualDividendYieldPct = 2.0,
                    sharpeRatio = 0.9,
                    mddPct = -15.0,
                    trackingErrorPct = 0.2,
                    holdings = emptyList(),
                    rocBreakdown = io.github.hdlee73.etftracker.domain.model.RocBreakdown(90.0, 0.0, 10.0),
                    dailyPrices = listOf(100.0, 101.0, 102.0),
                    benchmarkPrices = listOf(100.0, 101.0, 102.0),
                )
            }
        }.onFailure { err ->
            CrashLogger.logWarning("EtfRepository.search", "ETF 검색 실패 ($q): ${err.message}")
        }.getOrDefault(emptyList())
    }

    suspend fun loadEtfDetail(existing: EtfSummary): EtfSummary = withContext(Dispatchers.IO) {
        if (prefs.apiBaseUrl.isBlank() || prefs.appToken.isBlank()) return@withContext existing
        return@withContext runCatching {
            val api = NetworkModule.createApi(prefs)
            val basics = api.getEtfBasics(existing.code)
            val portfolio = api.getEtfPortfolio(existing.code)
            val stats = api.getEtfStats(existing.code)
            val latestClose = stats.close.lastOrNull { it != null } ?: existing.marketPrice
            val latestNav = basics.navKrx ?: stats.nav.lastOrNull { it != null } ?: existing.iNav
            val liveHoldings = if (portfolio.holdings.isNotEmpty()) {
                portfolio.holdings.take(10).map { dto ->
                    val prevMatch = existing.holdings.find { it.code == dto.code }
                    PdfHolding(
                        code = dto.code.ifBlank { dto.stockCode.orEmpty() }.ifBlank { dto.name },
                        name = dto.displayName,
                        weightPct = dto.weight ?: prevMatch?.weightPct ?: 0.0,
                        prevWeightPct = prevMatch?.prevWeightPct ?: (dto.weight ?: 0.0),
                        shareCount = dto.qty?.toLong() ?: prevMatch?.shareCount ?: 0L,
                        prevShareCount = prevMatch?.prevShareCount ?: (dto.qty?.toLong() ?: 0L),
                        dailyReturnPct = prevMatch?.dailyReturnPct ?: 0.0,
                    )
                }
            } else {
                existing.holdings
            }
            val closes = stats.close.filterNotNull().takeLast(12).map { it.toDouble() }
            val navs = stats.nav.filterNotNull().takeLast(12)
            existing.copy(
                manager = basics.manager.ifBlank { existing.manager },
                category = basics.assetClass.ifBlank { existing.category },
                marketPrice = latestClose,
                iNav = latestNav,
                terPct = basics.totalFee ?: existing.terPct,
                aumBillionWon = basics.netAssets?.div(100_000_000L) ?: existing.aumBillionWon,
                holdings = liveHoldings,
                dailyPrices = if (closes.size >= 3) closes else existing.dailyPrices,
                benchmarkPrices = if (navs.size >= 3) navs else existing.benchmarkPrices,
            )
        }.onFailure { err ->
            CrashLogger.logWarning("EtfRepository.detail", "ETF 상세 조회 실패 (${existing.code}): ${err.message}")
        }.getOrDefault(existing)
    }
}
