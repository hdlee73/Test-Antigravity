package io.github.hdlee73.etftracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hdlee73.etftracker.domain.analytics.EtfAnalyticsEngine
import io.github.hdlee73.etftracker.domain.model.DiscrepancyAlertLevel
import io.github.hdlee73.etftracker.ui.common.EditorialCard
import io.github.hdlee73.etftracker.ui.common.MultiLineChart
import io.github.hdlee73.etftracker.ui.common.SectionEditorialHeader
import io.github.hdlee73.etftracker.ui.common.StatusPill
import io.github.hdlee73.etftracker.ui.common.formatSignedPct
import io.github.hdlee73.etftracker.ui.common.formatWon
import io.github.hdlee73.etftracker.ui.theme.WealthColors
import io.github.hdlee73.etftracker.ui.viewmodel.MainUiState
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun CostDiscrepancyScreen(
    state: MainUiState,
    onInvestmentChange: (Long) -> Unit,
) {
    val selected = state.selectedEtf
    val costRows = remember(state.etfs, state.investmentAmountWon) {
        EtfAnalyticsEngine.compareRealCosts(state.etfs, state.investmentAmountWon)
    }
    val trackingError = remember(selected) {
        EtfAnalyticsEngine.calculateTrackingErrorPct(selected.dailyPrices, selected.benchmarkPrices)
    }
    val alertLevel = EtfAnalyticsEngine.evaluateDiscrepancy(selected)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionEditorialHeader(
                eyebrow = "True Cost & iNAV Monitor",
                title = "실부담비용률 · 괴리율 정밀 진단",
                subtitle = "KOFIA 기준 [총보수 + 기타비용 + 매매·중개수수료율] 합산 및 실시간 iNAV 괴리율",
            )
        }

        // 1. 실시간 iNAV 괴리율 모니터
        item {
            EditorialCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${selected.name} 실시간 iNAV 모니터", style = MaterialTheme.typography.titleMedium)
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
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("시장 체결가", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatWon(selected.marketPrice), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("실시간 iNAV", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatWon(selected.iNav.toLong()), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("괴리율", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = formatSignedPct(selected.discrepancyPct),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (alertLevel == DiscrepancyAlertLevel.OVERHEAT_ALERT) WealthColors.DeepCrimson else WealthColors.ClassicNavy,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "LP 호가 잔량: 매도 ${selected.lpAskQty}주 / 매수 ${selected.lpBidQty}주" +
                        if (minOf(selected.lpAskQty, selected.lpBidQty) < 500L) " (⚠ LP 호가 공백 주의)" else " (유동성 양호)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 2. 실부담비용률 1원 단위 비교
        item {
            EditorialCard {
                Text("투자 원금 설정: ${formatWon(state.investmentAmountWon)}", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = (state.investmentAmountWon / 1_000_000L).toFloat().coerceIn(1f, 300f),
                    onValueChange = { onInvestmentChange((it.roundToLong()) * 1_000_000L) },
                    valueRange = 1f..300f,
                )
                Text(
                    text = "동일 금액 투자 시 연간 실부담비용(1원 단위)을 낮은 순서대로 비교합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(costRows, key = { it.code }) { row ->
            EditorialCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "총보수 ${String.format(Locale.US, "%.3f", row.terPct)}% + 기타 ${String.format(Locale.US, "%.2f", row.otherCostPct)}% + 매매 ${String.format(Locale.US, "%.2f", row.brokerageFeePct)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "연 ${formatWon(row.annualCostWon)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = WealthColors.ClassicNavy,
                        )
                        Text(
                            text = "실부담 ${String.format(Locale.US, "%.3f", row.realCostPct)}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = WealthColors.ForestEmerald,
                        )
                    }
                }
            }
        }

        // 3. 추적오차 분석
        item {
            EditorialCard {
                Text("${selected.name} 추적오차(Tracking Error) 시계열", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "연환산 추적오차: ${String.format(Locale.US, "%.2f", maxOf(trackingError, selected.trackingErrorPct))}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = WealthColors.ChampagneGold,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                MultiLineChart(
                    series = listOf(
                        selected.name to selected.dailyPrices,
                        "기초지수" to selected.benchmarkPrices,
                    ),
                    colors = listOf(WealthColors.ClassicNavy, WealthColors.ChampagneGold),
                )
            }
        }
    }
}
