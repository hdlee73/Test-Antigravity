package io.github.hdlee73.etftracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hdlee73.etftracker.domain.analytics.EtfAnalyticsEngine
import io.github.hdlee73.etftracker.domain.model.DiscrepancyAlertLevel
import io.github.hdlee73.etftracker.domain.model.EtfSummary
import io.github.hdlee73.etftracker.domain.model.ExitActionType
import io.github.hdlee73.etftracker.domain.model.HoldingPosition
import io.github.hdlee73.etftracker.domain.model.HoldingSellSignal
import io.github.hdlee73.etftracker.domain.model.TradingSignalGrade
import io.github.hdlee73.etftracker.domain.model.TradingSignalResult
import io.github.hdlee73.etftracker.ui.common.EditorialCard
import io.github.hdlee73.etftracker.ui.common.MultiLineChart
import io.github.hdlee73.etftracker.ui.common.SectionEditorialHeader
import io.github.hdlee73.etftracker.ui.common.StatusPill
import io.github.hdlee73.etftracker.ui.common.formatSignedPct
import io.github.hdlee73.etftracker.ui.common.formatWon
import io.github.hdlee73.etftracker.ui.theme.WealthColors
import io.github.hdlee73.etftracker.ui.viewmodel.AppRoute
import io.github.hdlee73.etftracker.ui.viewmodel.MainUiState
import java.util.Locale

@Composable
fun DashboardScreen(
    state: MainUiState,
    onSearchChange: (String) -> Unit,
    onSelectEtf: (String) -> Unit,
    onAddWatchlist: (EtfSummary) -> Unit,
    onRemoveEtf: (String) -> Unit,
    onUpsertHolding: (String, String, Long, Int, Double, Double) -> Unit,
    onRemoveHolding: (String) -> Unit,
    onClearBanner: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    val selected = state.selectedEtf
    val alertLevel = remember(selected) { EtfAnalyticsEngine.evaluateDiscrepancy(selected) }
    val selectedSignal = remember(selected) { EtfAnalyticsEngine.generateTradingSignal(selected) }

    val allSignals = remember(state.etfs) {
        state.etfs.map { EtfAnalyticsEngine.generateTradingSignal(it) }
            .sortedByDescending { it.totalScore }
    }

    val sellSignals = remember(state.holdings, state.etfs) {
        val etfMap = state.etfs.associateBy { it.code }
        state.holdings.map { pos ->
            EtfAnalyticsEngine.evaluateHoldingSellSignal(pos, etfMap[pos.code])
        }.sortedByDescending { it.urgencyScore }
    }

    // 0 = 전체 점수 순위, 1 = 내 보유 매도코치, 2 = 종목 심층 & 히스토리
    var activeTab by rememberSaveable { mutableIntStateOf(0) }
    // 0 = 전체, 1 = 매수 추천(64점↑), 2 = 매도·주의(47점↓)
    var rankFilter by rememberSaveable { mutableIntStateOf(0) }

    var showHoldingDialog by remember { mutableStateOf(false) }
    var dialogInitialCode by remember { mutableStateOf(selected.code) }
    var dialogInitialName by remember { mutableStateOf(selected.name) }
    var dialogInitialPrice by remember { mutableStateOf(selected.marketPrice) }
    var dialogInitialQty by remember { mutableStateOf(10) }
    var dialogInitialTargetPct by remember { mutableStateOf(12.0) }
    var dialogInitialStopPct by remember { mutableStateOf(-6.0) }

    fun openHoldingModalForEtf(etf: EtfSummary, existingPos: HoldingPosition? = null) {
        dialogInitialCode = existingPos?.code ?: etf.code
        dialogInitialName = existingPos?.name ?: etf.name
        dialogInitialPrice = existingPos?.avgBuyPrice ?: etf.marketPrice
        dialogInitialQty = existingPos?.quantity ?: 20
        dialogInitialTargetPct = existingPos?.targetProfitPct ?: 12.0
        dialogInitialStopPct = existingPos?.stopLossPct ?: -6.0
        showHoldingDialog = true
    }

    if (showHoldingDialog) {
        HoldingPositionDialog(
            etfs = state.etfs,
            initialCode = dialogInitialCode,
            initialName = dialogInitialName,
            initialAvgPrice = dialogInitialPrice,
            initialQty = dialogInitialQty,
            initialTargetPct = dialogInitialTargetPct,
            initialStopPct = dialogInitialStopPct,
            onDismiss = { showHoldingDialog = false },
            onSave = { code, name, price, qty, targetPct, stopPct ->
                onUpsertHolding(code, name, price, qty, targetPct, stopPct)
                showHoldingDialog = false
                activeTab = 1
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionEditorialHeader(
                eyebrow = "AI Smart Trading & Exit Coach",
                title = "ETF 매매 시그널 · 랭킹 · 매도코치",
                subtitle = state.statusMessage,
            )
        }

        // 액션 알림 배너
        if (!state.actionBannerMessage.isNullOrBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WealthColors.EmeraldContainer,
                    border = BorderStroke(1.dp, WealthColors.ForestEmerald),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = state.actionBannerMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = WealthColors.ForestEmerald,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onClearBanner, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "알림 닫기",
                                tint = WealthColors.ForestEmerald,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        // 검색창
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ETF 검색 후 [+관심저장] 또는 [+보유등록] (예: 반도체, 배당, 069500)") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "검색어 지우기")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
        }

        // 검색 결과 즉시 저장 / 보유등록 패널
        if (state.searchQuery.isNotBlank()) {
            item {
                SearchResultsSavePanel(
                    query = state.searchQuery,
                    isSearching = state.isSearching,
                    results = state.filteredEtfs,
                    savedCodes = state.etfs.map { it.code }.toSet(),
                    onSaveToWatchlist = { etf ->
                        onAddWatchlist(etf)
                        activeTab = 2
                    },
                    onRegisterHolding = { etf ->
                        onAddWatchlist(etf)
                        openHoldingModalForEtf(etf)
                    },
                )
            }
        }

        // 상단 관심 ETF 빠른 선택 바 (점수 뱃지 + 삭제 버튼 포함)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.etfs, key = { it.code }) { etf ->
                    val isSelected = etf.code == selected.code
                    val sig = allSignals.firstOrNull { it.code == etf.code }
                    Surface(
                        modifier = Modifier
                            .width(218.dp)
                            .clickable { onSelectEtf(etf.code) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) WealthColors.ClassicNavy else MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) WealthColors.ChampagneGold else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${etf.code} · ${sig?.totalScore ?: 50}점",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) WealthColors.ChampagneGold else WealthColors.ClassicNavy,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = formatSignedPct(etf.dailyChangePct),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isSelected -> WealthColors.ChampagneContainer
                                            etf.dailyChangePct >= 0 -> WealthColors.ForestEmerald
                                            else -> WealthColors.DeepCrimson
                                        },
                                    )
                                    if (state.etfs.size > 1) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "종목 삭제",
                                            tint = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { onRemoveEtf(etf.code) },
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = etf.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${formatWon(etf.marketPrice)} · ${sig?.grade?.label ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // 핵심 3대 모드 전환 탭
        item {
            val urgentSellCount = sellSignals.count { it.recommendedSellRatioPct > 0 }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DashboardModeTab(
                    title = "1. 점수 순위 (${allSignals.size})",
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    modifier = Modifier.weight(1f),
                )
                DashboardModeTab(
                    title = "2. 보유 매도코치 (${urgentSellCount}/${sellSignals.size})",
                    selected = activeTab == 1,
                    highlightAlert = urgentSellCount > 0,
                    onClick = { activeTab = 1 },
                    modifier = Modifier.weight(1f),
                )
                DashboardModeTab(
                    title = "3. 심층·히스토리",
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        when (activeTab) {
            0 -> {
                // 탭 0: 전체 ETF 매매 점수 랭킹 보드 + 현재 선택 종목 요약 시그널
                item {
                    FullScoreLeaderboardCard(
                        signals = allSignals,
                        selectedCode = selected.code,
                        rankFilter = rankFilter,
                        onFilterChange = { rankFilter = it },
                        onSelectEtf = { code ->
                            onSelectEtf(code)
                        },
                        onOpenDetail = { code ->
                            onSelectEtf(code)
                            activeTab = 2
                        },
                        onRegisterHolding = { code ->
                            val target = state.etfs.firstOrNull { it.code == code } ?: selected
                            val existing = state.holdings.firstOrNull { it.code == code }
                            openHoldingModalForEtf(target, existing)
                        },
                        onRemoveEtf = onRemoveEtf,
                        canRemove = state.etfs.size > 1,
                    )
                }
                item {
                    TradingSignalCard(
                        signal = selectedSignal,
                        onRegisterHolding = {
                            val existing = state.holdings.firstOrNull { it.code == selected.code }
                            openHoldingModalForEtf(selected, existing)
                        },
                    )
                }
            }

            1 -> {
                // 탭 1: 내 보유 종목 매도 시그널 코치
                item {
                    HoldingPortfolioSummaryCard(
                        sellSignals = sellSignals,
                        onAddHoldingClick = { openHoldingModalForEtf(selected, null) },
                    )
                }
                items(sellSignals, key = { it.position.code }) { sellSig ->
                    HoldingSellSignalCard(
                        signal = sellSig,
                        onEdit = {
                            val etf = state.etfs.firstOrNull { it.code == sellSig.position.code } ?: selected
                            openHoldingModalForEtf(etf, sellSig.position)
                        },
                        onRemove = { onRemoveHolding(sellSig.position.code) },
                        onInspectEtf = {
                            onSelectEtf(sellSig.position.code)
                            activeTab = 2
                        },
                    )
                }
            }

            2 -> {
                // 탭 2: 선택 종목 심층 매매 시그널 + 과거 추세/기술적 히스토리 분석
                item {
                    TradingSignalCard(
                        signal = selectedSignal,
                        onRegisterHolding = {
                            val existing = state.holdings.firstOrNull { it.code == selected.code }
                            openHoldingModalForEtf(selected, existing)
                        },
                    )
                }
                item {
                    HistoricalTechnicalCard(
                        etf = selected,
                        signal = selectedSignal,
                    )
                }
                item {
                    SelectedEtfHeroCard(
                        etf = selected,
                        alertLevel = alertLevel,
                        onOpenXRay = { onNavigate(AppRoute.PDF_XRAY) },
                    )
                }
            }
        }

        item {
            Text(
                text = "전문 퀀트 분석 모듈",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ModuleShortcutCard(
                    icon = Icons.Outlined.Analytics,
                    title = "1. PDF(자산구성내역) 심층 해부 & X-Ray",
                    description = "상위 종목 기여도(%p), ETF 간 중복도 벤다이어그램, 액티브 리밸런싱 추적",
                    onClick = { onNavigate(AppRoute.PDF_XRAY) },
                )
                ModuleShortcutCard(
                    icon = Icons.Outlined.VerifiedUser,
                    title = "2. 실부담비용 · iNAV 괴리율 · 추적오차",
                    description = "KOFIA 1원 단위 실질 비용 비교, LP 호가 공백 경보, 지수 추적 효율 진단",
                    onClick = { onNavigate(AppRoute.COST_DISCREPANCY) },
                )
                ModuleShortcutCard(
                    icon = Icons.Outlined.Payments,
                    title = "3. 배당(분배금) 캐시플로우 & 절세 시뮬레이터",
                    description = "TR vs 수령 복리 격차, ROC 원금 잠식 판별, ISA·연금저축·IRP 세후 비교",
                    onClick = { onNavigate(AppRoute.DIVIDEND_TAX) },
                )
                ModuleShortcutCard(
                    icon = Icons.Outlined.CompareArrows,
                    title = "4. 폴드8 Dual-Pane 멀티 비교 & AI 뉴스 임팩트",
                    description = "3~4개 ETF 지표 테이블 + 수익률 차트 분할 뷰 및 보유 종목 뉴스 매핑",
                    onClick = { onNavigate(AppRoute.FOLD_COMPARE) },
                )
                ModuleShortcutCard(
                    icon = Icons.Outlined.ShowChart,
                    title = "5. 포트폴리오 백테스팅 · 히스토리 & 상관계수",
                    description = "전 종목 1M/3M/6M 추세 비교, MDD 회복 기간 및 환율/지수 상관계수 히트맵",
                    onClick = { onNavigate(AppRoute.BACKTEST_CORR) },
                )
            }
        }
    }
}

@Composable
private fun DashboardModeTab(
    title: String,
    selected: Boolean,
    highlightAlert: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        selected -> WealthColors.ClassicNavy
        highlightAlert -> WealthColors.CrimsonContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val fg = when {
        selected -> Color.White
        highlightAlert -> WealthColors.DeepCrimson
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(
            1.dp,
            if (selected) WealthColors.ChampagneGold else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchResultsSavePanel(
    query: String,
    isSearching: Boolean,
    results: List<EtfSummary>,
    savedCodes: Set<String>,
    onSaveToWatchlist: (EtfSummary) -> Unit,
    onRegisterHolding: (EtfSummary) -> Unit,
) {
    EditorialCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "'$query' 검색 결과 (${results.size}건)",
                style = MaterialTheme.typography.titleMedium,
            )
            if (isSearching) {
                Text(
                    text = "서버 검색 중...",
                    style = MaterialTheme.typography.bodySmall,
                    color = WealthColors.ChampagneGold,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "원하는 ETF를 [+관심저장]하면 점수 순위 및 히스토리 분석에 영구 추가되며, [+보유등록]하면 매도 시그널을 알려줍니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        if (results.isEmpty() && !isSearching) {
            Text(
                text = "일치하는 ETF가 없습니다. 종목코드 6자리(예: 069500) 또는 키워드(반도체, 나스닥, 배당)로 검색해 보세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                results.take(12).forEach { etf ->
                    val sig = remember(etf) { EtfAnalyticsEngine.generateTradingSignal(etf) }
                    val alreadySaved = savedCodes.contains(etf.code)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${etf.name} (${etf.code})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "현재가 ${formatWon(etf.marketPrice)} (${formatSignedPct(etf.dailyChangePct)}) · AI ${sig.totalScore}점 (${sig.grade.label})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onSaveToWatchlist(etf) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.BookmarkAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (alreadySaved) "분석 화면 보기" else "+ 관심저장 · 분석",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                                Button(
                                    onClick = { onRegisterHolding(etf) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WealthColors.ClassicNavy,
                                        contentColor = Color.White,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "+ 내 보유등록",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScoreLeaderboardCard(
    signals: List<TradingSignalResult>,
    selectedCode: String,
    rankFilter: Int,
    onFilterChange: (Int) -> Unit,
    onSelectEtf: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    onRegisterHolding: (String) -> Unit,
    onRemoveEtf: (String) -> Unit,
    canRemove: Boolean,
) {
    val buyList = signals.filter { it.totalScore >= 64 }
    val sellList = signals.filter { it.totalScore <= 47 }
    val filtered = when (rankFilter) {
        1 -> buyList
        2 -> sellList
        else -> signals
    }

    val topScore = signals.maxOfOrNull { it.totalScore } ?: 0
    val bottomScore = signals.minOfOrNull { it.totalScore } ?: 0

    EditorialCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "전 종목 AI 매매 점수 순위표",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "최고 ${topScore}점 ~ 최저 ${bottomScore}점 (격차 ${topScore - bottomScore}점) · 추세·수급·PDF·효율 4대 팩터 정밀 채점",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = rankFilter == 0,
                onClick = { onFilterChange(0) },
                label = { Text("전체 (${signals.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WealthColors.ClassicNavy,
                    selectedLabelColor = Color.White,
                ),
            )
            FilterChip(
                selected = rankFilter == 1,
                onClick = { onFilterChange(1) },
                label = { Text("매수 추천 (${buyList.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WealthColors.ForestEmerald,
                    selectedLabelColor = Color.White,
                ),
            )
            FilterChip(
                selected = rankFilter == 2,
                onClick = { onFilterChange(2) },
                label = { Text("매도·주의 (${sellList.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WealthColors.DeepCrimson,
                    selectedLabelColor = Color.White,
                ),
            )
        }

        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filtered.forEachIndexed { idx, item ->
                val globalRank = signals.indexOfFirst { it.code == item.code } + 1
                val isCurrent = item.code == selectedCode
                val (bg, fg) = signalGradeColors(item.grade)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectEtf(item.code) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCurrent) WealthColors.ChampagneContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.background,
                    border = BorderStroke(
                        1.dp,
                        if (isCurrent) WealthColors.ChampagneGold else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (globalRank) {
                                        1 -> WealthColors.ChampagneGold
                                        2, 3 -> WealthColors.ClassicNavy
                                        else -> MaterialTheme.colorScheme.surfaceContainer
                                    },
                                ) {
                                    Text(
                                        text = "#$globalRank",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (globalRank <= 3) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${item.code} · 현재가 ${formatWon(item.marketPrice)} (${formatSignedPct(item.dailyChangePct)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${item.totalScore}점",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = fg,
                                )
                                StatusPill(text = item.grade.label, containerColor = bg, contentColor = fg)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // 점수 게이지 바
                        LinearProgressIndicator(
                            progress = { (item.totalScore / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = fg,
                            trackColor = MaterialTheme.colorScheme.surfaceContainer,
                        )

                        Spacer(Modifier.height(8.dp))

                        // 과거 수익률 & 타점 요약
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "1개월 ${formatSignedPct(item.history.return1M)} · 3개월 ${formatSignedPct(item.history.return3M)} · RSI ${String.format(Locale.US, "%.0f", item.history.rsi14)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "목표가 ${formatWon(item.targetPriceWon)} (${formatSignedPct(item.expectedUpsidePct)})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (item.expectedUpsidePct >= 0) WealthColors.ForestEmerald else WealthColors.DeepCrimson,
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { onOpenDetail(item.code) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text("히스토리·타점 상세 →", style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(
                                    onClick = { onRegisterHolding(item.code) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text("+ 보유등록(매도코치)", style = MaterialTheme.typography.labelMedium, color = WealthColors.ForestEmerald)
                                }
                            }
                            if (canRemove) {
                                IconButton(
                                    onClick = { onRemoveEtf(item.code) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "종목 삭제",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldingPortfolioSummaryCard(
    sellSignals: List<HoldingSellSignal>,
    onAddHoldingClick: () -> Unit,
) {
    val totalInvested = sellSignals.sumOf { it.position.avgBuyPrice * it.position.quantity }
    val totalCurrent = sellSignals.sumOf { it.currentPrice * it.position.quantity }
    val totalPnlWon = totalCurrent - totalInvested
    val totalPnlPct = if (totalInvested > 0) (totalPnlWon.toDouble() / totalInvested.toDouble()) * 100.0 else 0.0
    val urgentAlerts = sellSignals.count { it.recommendedSellRatioPct > 0 }

    EditorialCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MY PORTFOLIO EXIT COACH",
                    style = MaterialTheme.typography.labelMedium,
                    color = WealthColors.ChampagneGold,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "내 보유 ETF 실시간 매도 코치",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Button(
                onClick = onAddHoldingClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WealthColors.ClassicNavy,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("보유 ETF 추가", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MetricMini("총 평가금액", formatWon(totalCurrent))
            MetricMini(
                "총 평가손익",
                "${if (totalPnlWon >= 0) "+" else ""}${formatWon(totalPnlWon)} (${formatSignedPct(totalPnlPct)})",
            )
            MetricMini("매도 시그널 발동", "${urgentAlerts}종목")
        }

        if (urgentAlerts > 0) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = WealthColors.CrimsonContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = WealthColors.DeepCrimson,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "현재 ${urgentAlerts}개 보유 종목에서 익절/손절/고점꺾임 매도 시그널이 감지되었습니다. 아래 권장 매도비중을 확인하세요.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WealthColors.DeepCrimson,
                    )
                }
            }
        }
    }
}

@Composable
private fun HoldingSellSignalCard(
    signal: HoldingSellSignal,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onInspectEtf: () -> Unit,
) {
    val isUrgentSell = signal.recommendedSellRatioPct > 0
    val (pillBg, pillFg) = when (signal.action) {
        ExitActionType.STOP_LOSS_NOW,
        ExitActionType.TRAILING_STOP_SELL,
        ExitActionType.OVERHEAT_TRIM -> WealthColors.CrimsonContainer to WealthColors.DeepCrimson
        ExitActionType.TAKE_PROFIT_NOW,
        ExitActionType.PARTIAL_TAKE_PROFIT -> WealthColors.ChampagneContainer to WealthColors.ClassicNavy
        ExitActionType.ADD_ON_DIP,
        ExitActionType.HOLD_POSITION -> WealthColors.EmeraldContainer to WealthColors.ForestEmerald
    }

    EditorialCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${signal.position.name} (${signal.position.code})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "보유 ${signal.position.quantity}주 · 매수평단 ${formatWon(signal.position.avgBuyPrice)} → 현재가 ${formatWon(signal.currentPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(text = signal.action.label, containerColor = pillBg, contentColor = pillFg)
        }

        Spacer(Modifier.height(10.dp))

        // 수익률 및 매도 비중 핵심 박스
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isUrgentSell) WealthColors.CrimsonContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.background,
            border = BorderStroke(
                1.dp,
                if (isUrgentSell) WealthColors.DeepCrimson else MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = signal.headline,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUrgentSell) WealthColors.DeepCrimson else WealthColors.ClassicNavy,
                        modifier = Modifier.weight(1f),
                    )
                    if (signal.recommendedSellRatioPct > 0) {
                        val sellQty = ((signal.position.quantity * signal.recommendedSellRatioPct) / 100).coerceAtLeast(1)
                        StatusPill(
                            text = "권장 매도 ${signal.recommendedSellRatioPct}% (${sellQty}주)",
                            containerColor = WealthColors.DeepCrimson,
                            contentColor = Color.White,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = signal.reasonDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MetricMini(
                "평가손익",
                "${if (signal.pnlAmountWon >= 0) "+" else ""}${formatWon(signal.pnlAmountWon)} (${formatSignedPct(signal.pnlPct)})",
            )
            MetricMini("목표 익절가", formatWon(signal.nextTargetPrice))
            MetricMini("트레일링/손절가", formatWon(signal.nextTrailingStopPrice))
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onInspectEtf) {
                Text("종목 추세·히스토리 보기 →", style = MaterialTheme.typography.labelMedium)
            }
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "보유정보 수정",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "보유종목 삭제",
                        tint = WealthColors.DeepCrimson,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoricalTechnicalCard(
    etf: EtfSummary,
    signal: TradingSignalResult,
) {
    var periodIndex by rememberSaveable { mutableIntStateOf(1) } // 0=1개월(20일), 1=3개월(60일)
    val prices = etf.dailyPrices
    val sliceCount = if (periodIndex == 0) 20 else 60
    val displayPrices = remember(prices, sliceCount) { prices.takeLast(sliceCount) }

    val ma5Series = remember(displayPrices) {
        displayPrices.indices.map { idx ->
            val start = (idx - 4).coerceAtLeast(0)
            displayPrices.subList(start, idx + 1).average()
        }
    }
    val ma20Series = remember(displayPrices) {
        displayPrices.indices.map { idx ->
            val start = (idx - 19).coerceAtLeast(0)
            displayPrices.subList(start, idx + 1).average()
        }
    }

    val h = signal.history

    EditorialCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HISTORICAL TREND & BACKTEST",
                    style = MaterialTheme.typography.labelMedium,
                    color = WealthColors.ChampagneGold,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${etf.name} · 과거 추세 & 기술적 히스토리",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = periodIndex == 0,
                    onClick = { periodIndex = 0 },
                    label = { Text("1개월") },
                )
                FilterChip(
                    selected = periodIndex == 1,
                    onClick = { periodIndex = 1 },
                    label = { Text("3개월") },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        MultiLineChart(
            series = listOf(
                "종가" to displayPrices,
                "5일선(MA5)" to ma5Series,
                "20일선(MA20)" to ma20Series,
            ),
            colors = listOf(
                WealthColors.ClassicNavy,
                WealthColors.ForestEmerald,
                WealthColors.ChampagneGold,
            ),
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MetricMini("1개월 수익률", formatSignedPct(h.return1M))
            MetricMini("3개월 수익률", formatSignedPct(h.return3M))
            MetricMini("6개월(연환산)", formatSignedPct(h.return6M))
            MetricMini("RSI(14일)", "${String.format(Locale.US, "%.1f", h.rsi14)}pt")
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MetricMini("이평선 배열", h.trendStatus)
            MetricMini("연환산 변동성", "${String.format(Locale.US, "%.1f", h.volatilityPct)}%")
            MetricMini("과거 시그널 승률", "${String.format(Locale.US, "%.1f", h.winRatePct)}%")
            MetricMini("시그널 누적수익", formatSignedPct(h.signalBacktestReturnPct))
        }
    }
}

@Composable
private fun TradingSignalCard(
    signal: TradingSignalResult,
    onRegisterHolding: () -> Unit,
) {
    val grade = signal.grade
    val (bg, fg) = signalGradeColors(grade)

    EditorialCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI 매매 시그널 · 정밀 타점 브리핑",
                    style = MaterialTheme.typography.labelMedium,
                    color = WealthColors.ChampagneGold,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${signal.name} · ${signal.totalScore}점 / 100",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            StatusPill(text = grade.label, containerColor = bg, contentColor = fg)
        }

        Spacer(Modifier.height(10.dp))

        // 구체적 진입가 / 목표 매도가 / 손절가 박스
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricMini("권장 진입가", formatWon(signal.entryPriceWon))
                MetricMini(
                    "목표 매도가",
                    "${formatWon(signal.targetPriceWon)} (${formatSignedPct(signal.expectedUpsidePct)})",
                )
                MetricMini("손절 기준가", formatWon(signal.stopLossPriceWon))
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = signal.targetActionText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            signal.factors.forEach { factor ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = factor.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${factor.score} / ${factor.maxScore}점",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = WealthColors.ClassicNavy,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = factor.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (factor.score.toFloat() / factor.maxScore.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(50)),
                        color = WealthColors.ClassicNavy,
                        trackColor = MaterialTheme.colorScheme.surfaceContainer,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRegisterHolding,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("이미 보유 중이신가요? 내 평단가 등록하고 매도 타점 받기")
        }
    }
}

@Composable
private fun HoldingPositionDialog(
    etfs: List<EtfSummary>,
    initialCode: String,
    initialName: String,
    initialAvgPrice: Long,
    initialQty: Int,
    initialTargetPct: Double,
    initialStopPct: Double,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Int, Double, Double) -> Unit,
) {
    var code by remember { mutableStateOf(initialCode) }
    var name by remember { mutableStateOf(initialName) }
    var avgPriceText by remember { mutableStateOf(initialAvgPrice.toString()) }
    var qtyText by remember { mutableStateOf(initialQty.toString()) }
    var targetPctText by remember { mutableStateOf(String.format(Locale.US, "%.1f", initialTargetPct)) }
    var stopPctText by remember { mutableStateOf(String.format(Locale.US, "%.1f", initialStopPct)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("내 보유 ETF 등록 · 매도 시그널 설정", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "보유 중인 종목을 선택하고 매수 평단가와 수량을 입력하면 목표 익절가, 손절선, 고점 대비 트레일링 스탑 매도 시그널을 실시간 계산합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(etfs, key = { it.code }) { item ->
                        FilterChip(
                            selected = item.code == code,
                            onClick = {
                                code = item.code
                                name = item.name
                                avgPriceText = item.marketPrice.toString()
                            },
                            label = { Text(item.name.take(10)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = "$name ($code)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("선택 종목") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = avgPriceText,
                    onValueChange = { avgPriceText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("내 매수 평단가 (원)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("보유 수량 (주)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetPctText,
                        onValueChange = { targetPctText = it },
                        label = { Text("목표 익절률 (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = stopPctText,
                        onValueChange = { stopPctText = it },
                        label = { Text("손절 기준 (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = avgPriceText.toLongOrNull() ?: initialAvgPrice
                    val qty = qtyText.toIntOrNull() ?: initialQty
                    val target = targetPctText.toDoubleOrNull() ?: 12.0
                    val stop = stopPctText.toDoubleOrNull()?.let { if (it > 0) -it else it } ?: -6.0
                    onSave(code, name, price, qty, target, stop)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = WealthColors.ClassicNavy,
                    contentColor = Color.White,
                ),
            ) {
                Text("매도 코치 등록")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
    )
}

@Composable
private fun SelectedEtfHeroCard(
    etf: EtfSummary,
    alertLevel: DiscrepancyAlertLevel,
    onOpenXRay: () -> Unit,
) {
    EditorialCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${etf.manager} · ${etf.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = etf.name,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            StatusPill(
                text = alertLevel.label,
                containerColor = when (alertLevel) {
                    DiscrepancyAlertLevel.NORMAL -> WealthColors.EmeraldContainer
                    DiscrepancyAlertLevel.CAUTION -> WealthColors.ChampagneContainer
                    DiscrepancyAlertLevel.OVERHEAT_ALERT -> WealthColors.CrimsonContainer
                },
                contentColor = when (alertLevel) {
                    DiscrepancyAlertLevel.NORMAL -> WealthColors.ForestEmerald
                    DiscrepancyAlertLevel.CAUTION -> WealthColors.ChampagneGold
                    DiscrepancyAlertLevel.OVERHEAT_ALERT -> WealthColors.DeepCrimson
                },
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MetricMini("현재가", formatWon(etf.marketPrice))
            MetricMini("실시간 iNAV", formatWon(etf.iNav.toLong()))
            MetricMini("괴리율", formatSignedPct(etf.discrepancyPct))
            MetricMini("실부담비용", "${String.format(Locale.US, "%.2f", etf.realCostPct)}%")
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenXRay),
            shape = RoundedCornerShape(10.dp),
            color = WealthColors.ChampagneContainer,
        ) {
            Text(
                text = "구성종목 ${etf.holdings.size}개 기여도 · 리밸런싱 상세 분석 보기 →",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = WealthColors.ClassicNavy,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

private fun signalGradeColors(grade: TradingSignalGrade): Pair<Color, Color> = when (grade) {
    TradingSignalGrade.STRONG_BUY,
    TradingSignalGrade.BUY -> WealthColors.EmeraldContainer to WealthColors.ForestEmerald
    TradingSignalGrade.HOLD -> WealthColors.ChampagneContainer to WealthColors.ChampagneGold
    TradingSignalGrade.TRIM,
    TradingSignalGrade.SELL -> WealthColors.CrimsonContainer to WealthColors.DeepCrimson
}

@Composable
private fun MetricMini(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ModuleShortcutCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    EditorialCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WealthColors.ChampagneContainer,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = WealthColors.ClassicNavy,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
