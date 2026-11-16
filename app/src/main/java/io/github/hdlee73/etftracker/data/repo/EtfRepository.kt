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

        RepositorySnapshot(
            etfs = merged,
            newsImpacts = SampleEtfCatalog.newsImpacts,
            isServerConnected = true,
            statusMessage = "EveryCorp 서버 실시간 연동됨 ($baseUrl)",
        )
    }
}
