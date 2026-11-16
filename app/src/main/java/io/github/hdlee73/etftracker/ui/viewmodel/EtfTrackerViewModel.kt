package io.github.hdlee73.etftracker.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.hdlee73.etftracker.data.log.CrashLogger
import io.github.hdlee73.etftracker.data.prefs.AppPrefs
import io.github.hdlee73.etftracker.data.repo.EtfRepository
import io.github.hdlee73.etftracker.data.repo.SampleEtfCatalog
import io.github.hdlee73.etftracker.data.update.UpdateCheckResult
import io.github.hdlee73.etftracker.data.update.UpdateChecker
import io.github.hdlee73.etftracker.domain.model.EtfSummary
import io.github.hdlee73.etftracker.domain.model.HoldingPosition
import io.github.hdlee73.etftracker.domain.model.NewsImpactItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppRoute(val title: String, val subtitle: String) {
    DASHBOARD("시그널 · 랭킹 · 매도코치", "전 종목 매수/매도 점수 순위 · 보유종목 매도 타점"),
    PDF_XRAY("PDF X-Ray", "구성종목 기여도 · 중복도 · 리밸런싱"),
    COST_DISCREPANCY("비용 · 괴리율", "실부담비용 1원 비교 · iNAV · 추적오차"),
    DIVIDEND_TAX("배당 · 절세", "TR 복리 · ROC 재원 · ISA/연금 시뮬레이션"),
    FOLD_COMPARE("멀티 비교", "폴드8 Dual-Pane · AI 뉴스 임팩트"),
    BACKTEST_CORR("백테스트 · 히스토리", "기간별 추세 · MDD 회복력 · 상관계수"),
    APP_INFO("앱 정보 · 설정", "EveryCorp 서버 · 자동 업데이트 · 로그"),
}

@Immutable
data class MainUiState(
    val currentRoute: AppRoute = AppRoute.DASHBOARD,
    val backStack: List<AppRoute> = listOf(AppRoute.DASHBOARD),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val isServerConnected: Boolean = false,
    val statusMessage: String = "데이터를 불러오는 중...",
    val actionBannerMessage: String? = null,
    val searchQuery: String = "",
    val etfs: List<EtfSummary> = SampleEtfCatalog.etfs,
    val remoteSearchResults: List<EtfSummary> = emptyList(),
    val holdings: List<HoldingPosition> = SampleEtfCatalog.defaultHoldings,
    val selectedEtfCode: String = "360750",
    val overlapCodeA: String = "091180",
    val overlapCodeB: String = "496120",
    val compareCodes: List<String> = listOf("360750", "496120", "091180"),
    val investmentAmountWon: Long = 50_000_000L,
    val simPrincipalWon: Long = 30_000_000L,
    val simYears: Int = 10,
    val simGrowthPct: Double = 6.5,
    val isComprehensiveTax: Boolean = false,
    val newsImpacts: List<NewsImpactItem> = SampleEtfCatalog.newsImpacts,
    val apiBaseUrlInput: String = "",
    val appTokenInput: String = "",
    val updateResult: UpdateCheckResult? = null,
    val isCheckingUpdate: Boolean = false,
    val crashLogText: String = "",
) {
    val selectedEtf: EtfSummary
        get() = etfs.firstOrNull { it.code == selectedEtfCode } ?: etfs.first()

    val filteredEtfs: List<EtfSummary>
        get() {
            val q = searchQuery.trim()
            if (q.isEmpty()) return etfs
            val localMatches = etfs.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.code.contains(q, ignoreCase = true) ||
                    it.category.contains(q, ignoreCase = true)
            }
            return (localMatches + remoteSearchResults).distinctBy { it.code }
        }
}

class EtfTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPrefs(application)
    private val repository = EtfRepository(prefs)
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(
        MainUiState(
            selectedEtfCode = prefs.selectedEtfCode,
            compareCodes = prefs.compareCodes,
            holdings = prefs.holdings,
            apiBaseUrlInput = "",
            appTokenInput = "",
            crashLogText = CrashLogger.readLog(application),
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val snapshot = repository.loadInitialData()
            val removed = prefs.removedEtfCodes
            val visibleEtfs = snapshot.etfs.filterNot { removed.contains(it.code) }
                .ifEmpty { listOf(snapshot.etfs.first()) }
            _uiState.update {
                val nextSelected = if (visibleEtfs.any { e -> e.code == it.selectedEtfCode }) {
                    it.selectedEtfCode
                } else {
                    visibleEtfs.first().code
                }
                it.copy(
                    isLoading = false,
                    etfs = visibleEtfs,
                    selectedEtfCode = nextSelected,
                    holdings = prefs.holdings,
                    newsImpacts = snapshot.newsImpacts,
                    isServerConnected = snapshot.isServerConnected,
                    statusMessage = snapshot.statusMessage,
                )
            }
        }
    }

    fun navigateTo(route: AppRoute) {
        _uiState.update { state ->
            if (state.currentRoute == route) return@update state
            val nextStack = (state.backStack + route).takeLast(12)
            state.copy(currentRoute = route, backStack = nextStack)
        }
    }

    fun navigateBack(): Boolean {
        val currentStack = _uiState.value.backStack
        if (currentStack.size <= 1) return false
        val trimmed = currentStack.dropLast(1)
        _uiState.update { it.copy(currentRoute = trimmed.last(), backStack = trimmed) }
        return true
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(isSearching = false, remoteSearchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(250)
            val remote = repository.searchRemoteEtfs(trimmed)
            _uiState.update { state ->
                state.copy(isSearching = false, remoteSearchResults = remote)
            }
        }
    }

    /**
     * 검색 결과에서 종목을 관심종목/분석대상에 영구 저장하고 상세 시세·히스토리를 불러온다.
     */
    fun addEtfToWatchlist(etf: EtfSummary) {
        prefs.removedEtfCodes = prefs.removedEtfCodes - etf.code
        prefs.savedWatchlistCodes = prefs.savedWatchlistCodes + etf.code
        prefs.selectedEtfCode = etf.code

        _uiState.update { state ->
            val merged = (listOf(etf) + state.etfs).distinctBy { it.code }
            state.copy(
                etfs = merged,
                selectedEtfCode = etf.code,
                searchQuery = "",
                remoteSearchResults = emptyList(),
                actionBannerMessage = "'${etf.name}' 종목이 분석 목록에 저장되었습니다.",
            )
        }

        viewModelScope.launch {
            val detailed = repository.loadEtfDetail(etf)
            _uiState.update { state ->
                val updated = state.etfs.map { if (it.code == etf.code) detailed else it }
                state.copy(etfs = updated)
            }
        }
    }

    fun selectEtf(code: String) {
        prefs.selectedEtfCode = code
        _uiState.update { it.copy(selectedEtfCode = code) }
        val target = (_uiState.value.etfs + _uiState.value.remoteSearchResults).firstOrNull { it.code == code } ?: return
        viewModelScope.launch {
            val detailed = repository.loadEtfDetail(target)
            _uiState.update { state ->
                val existsInMain = state.etfs.any { it.code == code }
                val updated = if (existsInMain) {
                    state.etfs.map { if (it.code == code) detailed else it }
                } else {
                    listOf(detailed) + state.etfs
                }
                state.copy(etfs = updated)
            }
        }
    }

    fun removeEtf(code: String) {
        _uiState.update { state ->
            if (state.etfs.size <= 1) return@update state
            val remaining = state.etfs.filterNot { it.code == code }
            if (remaining.isEmpty()) return@update state
            prefs.removedEtfCodes = prefs.removedEtfCodes + code
            prefs.savedWatchlistCodes = prefs.savedWatchlistCodes - code
            val nextSelected = if (state.selectedEtfCode == code) remaining.first().code else state.selectedEtfCode
            prefs.selectedEtfCode = nextSelected
            val nextCompare = state.compareCodes.filterNot { it == code }.ifEmpty { listOf(nextSelected) }
            prefs.compareCodes = nextCompare
            state.copy(
                etfs = remaining,
                remoteSearchResults = state.remoteSearchResults.filterNot { it.code == code },
                selectedEtfCode = nextSelected,
                compareCodes = nextCompare,
                actionBannerMessage = "종목이 분석 목록에서 삭제되었습니다.",
            )
        }
    }

    /**
     * 내 보유 종목(평단가·수량·목표익절%·손절%)을 등록하거나 수정한다.
     */
    fun upsertHoldingPosition(
        code: String,
        name: String,
        avgBuyPrice: Long,
        quantity: Int,
        targetProfitPct: Double = 12.0,
        stopLossPct: Double = -6.0,
    ) {
        val safePrice = avgBuyPrice.coerceAtLeast(100L)
        val safeQty = quantity.coerceAtLeast(1)
        val existingEtf = (_uiState.value.etfs + _uiState.value.remoteSearchResults).firstOrNull { it.code == code }
        val high = maxOf(safePrice, existingEtf?.marketPrice ?: safePrice)

        val newPos = HoldingPosition(
            code = code,
            name = name,
            avgBuyPrice = safePrice,
            quantity = safeQty,
            targetProfitPct = targetProfitPct.coerceIn(2.0, 100.0),
            stopLossPct = stopLossPct.coerceIn(-35.0, -1.5),
            highestPriceSinceBuy = high,
        )

        val updatedHoldings = _uiState.value.holdings
            .filterNot { it.code == code }
            .let { listOf(newPos) + it }

        prefs.holdings = updatedHoldings

        // 보유 종목이 분석 목록에 없으면 함께 추가
        if (existingEtf != null && _uiState.value.etfs.none { it.code == code }) {
            addEtfToWatchlist(existingEtf)
        }

        _uiState.update {
            it.copy(
                holdings = updatedHoldings,
                actionBannerMessage = "'$name' 보유 포지션(평단 ${safePrice}원 · ${safeQty}주) 매도 시그널 감시가 시작되었습니다.",
            )
        }
    }

    fun removeHoldingPosition(code: String) {
        val updated = _uiState.value.holdings.filterNot { it.code == code }
        prefs.holdings = updated
        _uiState.update {
            it.copy(
                holdings = updated,
                actionBannerMessage = "보유 종목 포지션이 삭제되었습니다.",
            )
        }
    }

    fun clearBannerMessage() {
        _uiState.update { it.copy(actionBannerMessage = null) }
    }

    fun setOverlapPair(codeA: String, codeB: String) {
        _uiState.update { it.copy(overlapCodeA = codeA, overlapCodeB = codeB) }
    }

    fun toggleCompareCode(code: String) {
        _uiState.update { state ->
            val current = state.compareCodes.toMutableList()
            if (current.contains(code)) {
                if (current.size > 1) current.remove(code)
            } else if (current.size < 4) {
                current.add(code)
            }
            prefs.compareCodes = current
            state.copy(compareCodes = current)
        }
    }

    fun updateInvestmentAmount(won: Long) {
        _uiState.update { it.copy(investmentAmountWon = won.coerceIn(1_000_000L, 10_000_000_000L)) }
    }

    fun updateDividendSim(principalWon: Long, years: Int, growthPct: Double, comprehensive: Boolean) {
        _uiState.update {
            it.copy(
                simPrincipalWon = principalWon,
                simYears = years,
                simGrowthPct = growthPct,
                isComprehensiveTax = comprehensive,
            )
        }
    }

    fun saveServerConfig(url: String, token: String) {
        if (url.isNotBlank()) prefs.apiBaseUrl = url
        if (token.isNotBlank()) prefs.appToken = token
        _uiState.update { it.copy(apiBaseUrlInput = "", appTokenInput = "") }
        refreshData()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true) }
            val res = UpdateChecker.checkLatest()
            _uiState.update { it.copy(isCheckingUpdate = false, updateResult = res) }
        }
    }

    fun refreshCrashLog() {
        val text = CrashLogger.readLog(getApplication())
        _uiState.update { it.copy(crashLogText = text) }
    }

    fun clearCrashLog() {
        CrashLogger.clearLog(getApplication())
        refreshCrashLog()
    }
}
