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
import io.github.hdlee73.etftracker.domain.model.NewsImpactItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppRoute(val title: String, val subtitle: String) {
    DASHBOARD("자산 브리핑", "포트폴리오 현황 및 실시간 괴리율"),
    PDF_XRAY("PDF X-Ray", "구성종목 기여도 · 중복도 · 리밸런싱"),
    COST_DISCREPANCY("비용 · 괴리율", "실부담비용 1원 비교 · iNAV · 추적오차"),
    DIVIDEND_TAX("배당 · 절세", "TR 복리 · ROC 재원 · ISA/연금 시뮬레이션"),
    FOLD_COMPARE("멀티 비교", "폴드8 Dual-Pane · AI 뉴스 임팩트"),
    BACKTEST_CORR("백테스트 · 상관", "위기 MDD 회복력 · 상관계수 매트릭스"),
    APP_INFO("앱 정보 · 설정", "EveryCorp 서버 · 자동 업데이트 · 로그"),
}

@Immutable
data class MainUiState(
    val currentRoute: AppRoute = AppRoute.DASHBOARD,
    val backStack: List<AppRoute> = listOf(AppRoute.DASHBOARD),
    val isLoading: Boolean = false,
    val isServerConnected: Boolean = false,
    val statusMessage: String = "데이터를 불러오는 중...",
    val searchQuery: String = "",
    val etfs: List<EtfSummary> = SampleEtfCatalog.etfs,
    val selectedEtfCode: String = "069500",
    val overlapCodeA: String = "091180",
    val overlapCodeB: String = "496120",
    val compareCodes: List<String> = listOf("069500", "360750", "458730"),
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
            return etfs.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.code.contains(q, ignoreCase = true) ||
                    it.category.contains(q, ignoreCase = true)
            }
        }
}

class EtfTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPrefs(application)
    private val repository = EtfRepository(prefs)

    private val _uiState = MutableStateFlow(
        MainUiState(
            selectedEtfCode = prefs.selectedEtfCode,
            compareCodes = prefs.compareCodes,
            apiBaseUrlInput = prefs.apiBaseUrl,
            appTokenInput = prefs.appToken,
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
            _uiState.update {
                it.copy(
                    isLoading = false,
                    etfs = snapshot.etfs,
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

    /**
     * BackHandler에서 호출: 직전 페이지가 있으면 true 반환 후 뒤로 이동, 루트면 false 반환.
     */
    fun navigateBack(): Boolean {
        val currentStack = _uiState.value.backStack
        if (currentStack.size <= 1) return false
        val trimmed = currentStack.dropLast(1)
        _uiState.update { it.copy(currentRoute = trimmed.last(), backStack = trimmed) }
        return true
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectEtf(code: String) {
        prefs.selectedEtfCode = code
        _uiState.update { it.copy(selectedEtfCode = code) }
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
