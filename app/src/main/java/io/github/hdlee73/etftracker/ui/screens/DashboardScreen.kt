package io.github.hdlee73.etftracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import io.github.hdlee73.etftracker.domain.analytics.EtfAnalyticsEngine
import io.github.hdlee73.etftracker.domain.model.DiscrepancyAlertLevel
import io.github.hdlee73.etftracker.domain.model.EtfSummary
import io.github.hdlee73.etftracker.ui.common.EditorialCard
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
    onRemoveEtf: (String) -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    val selected = state.selectedEtf
    val alertLevel = EtfAnalyticsEngine.evaluateDiscrepancy(selected)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionEditorialHeader(
                eyebrow = "Editorial Private Wealth",
                title = "ETF 정밀 자산 브리핑",
                subtitle = state.statusMessage,
            )
        }

        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ETF 종목명, 코드(예: 069500) 또는 테마 검색") },
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

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.filteredEtfs, key = { it.code }) { etf ->
                    val isSelected = etf.code == selected.code
                    Surface(
                        modifier = Modifier
                            .width(210.dp)
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
                                    text = etf.code,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) WealthColors.ChampagneGold else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
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
                                text = "${formatWon(etf.marketPrice)} · 실부담 ${String.format(Locale.US, "%.2f", etf.realCostPct)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            SelectedEtfHeroCard(
                etf = selected,
                alertLevel = alertLevel,
                onOpenXRay = { onNavigate(AppRoute.PDF_XRAY) },
            )
        }

        item {
            Text(
                text = "핵심 분석 모듈",
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
                    title = "5. 포트폴리오 백테스팅 & 상관계수 매트릭스",
                    description = "과거 위기 구간 MDD · 원금 회복 기간 및 환노출/환헤지 상관계수 히트맵",
                    onClick = { onNavigate(AppRoute.BACKTEST_CORR) },
                )
            }
        }
    }
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
