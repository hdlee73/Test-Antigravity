package io.github.hdlee73.etftracker.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hdlee73.etftracker.domain.analytics.EtfAnalyticsEngine
import io.github.hdlee73.etftracker.ui.common.EditorialCard
import io.github.hdlee73.etftracker.ui.common.MultiLineChart
import io.github.hdlee73.etftracker.ui.common.SectionEditorialHeader
import io.github.hdlee73.etftracker.ui.common.StatusPill
import io.github.hdlee73.etftracker.ui.common.formatWon
import io.github.hdlee73.etftracker.ui.theme.WealthColors
import io.github.hdlee73.etftracker.ui.viewmodel.MainUiState
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun DividendTaxScreen(
    state: MainUiState,
    onSimChange: (Long, Int, Double, Boolean) -> Unit,
) {
    val selected = state.selectedEtf
    val simResult = remember(state.simPrincipalWon, state.simYears, state.simGrowthPct, selected.annualDividendYieldPct) {
        EtfAnalyticsEngine.simulateDividendCompound(
            principalWon = state.simPrincipalWon,
            annualPriceGrowthPct = state.simGrowthPct,
            annualDividendYieldPct = selected.annualDividendYieldPct,
            years = state.simYears,
        )
    }
    val annualGrossDiv = remember(state.simPrincipalWon, selected.annualDividendYieldPct) {
        (state.simPrincipalWon * (selected.annualDividendYieldPct / 100.0)).roundToLong()
    }
    val taxRows = remember(annualGrossDiv, state.isComprehensiveTax) {
        EtfAnalyticsEngine.compareAccountTaxes(annualGrossDiv, state.isComprehensiveTax)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionEditorialHeader(
                eyebrow = "Dividend Cashflow & Tax Shield",
                title = "배당 캐시플로우 · ROC · 절세 시뮬레이터",
                subtitle = "${selected.name} (연 분배율 ${String.format(Locale.US, "%.2f", selected.annualDividendYieldPct)}%) 기준",
            )
        }

        // 1. TR vs Cash-out 시뮬레이터
        item {
            EditorialCard {
                Text("투자 원금: ${formatWon(state.simPrincipalWon)} · 기간: ${state.simYears}년", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = (state.simPrincipalWon / 1_000_000L).toFloat().coerceIn(5f, 300f),
                    onValueChange = { onSimChange(it.roundToLong() * 1_000_000L, state.simYears, state.simGrowthPct, state.isComprehensiveTax) },
                    valueRange = 5f..300f,
                )
                Text("투자 기간 (${state.simYears}년)", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = state.simYears.toFloat(),
                    onValueChange = { onSimChange(state.simPrincipalWon, it.roundToInt().coerceIn(1, 30), state.simGrowthPct, state.isComprehensiveTax) },
                    valueRange = 1f..30f,
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("재투자(TR) 최종", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatWon(simResult.trFinalWon), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WealthColors.ForestEmerald)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("복리 격차 (+)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatWon(simResult.compoundGapWon), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WealthColors.ChampagneGold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                MultiLineChart(
                    series = listOf(
                        "분배금 재투자(TR)" to simResult.points.map { it.trValueWon.toDouble() },
                        "분배금 현금 수령" to simResult.points.map { it.cashOutValueWon.toDouble() },
                    ),
                    colors = listOf(WealthColors.ForestEmerald, WealthColors.ClassicNavy),
                )
            }
        }

        // 2. 분배금 재원 분석 (ROC 판별)
        item {
            val roc = selected.rocBreakdown
            EditorialCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("분배금 재원 분석 (ROC 판별)", style = MaterialTheme.typography.titleMedium)
                    StatusPill(
                        text = if (roc.isPrincipalErosionRisk) "원금 잠식(ROC) 주의" else "실질 배당 양호",
                        containerColor = if (roc.isPrincipalErosionRisk) WealthColors.CrimsonContainer else WealthColors.EmeraldContainer,
                        contentColor = if (roc.isPrincipalErosionRisk) WealthColors.DeepCrimson else WealthColors.ForestEmerald,
                    )
                }
                Spacer(Modifier.height(10.dp))
                RocRatioBar("주식 배당수익", roc.dividendIncomePct, WealthColors.ForestEmerald)
                Spacer(Modifier.height(6.dp))
                RocRatioBar("옵션 프리미엄", roc.optionPremiumPct, WealthColors.ChampagneGold)
                Spacer(Modifier.height(6.dp))
                RocRatioBar("원금·자본차익(ROC)", roc.capitalGainRocPct, WealthColors.DeepCrimson)
            }
        }

        // 3. 계좌별 세후 실수령액 비교
        item {
            EditorialCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("계좌별 세후 실수령액 (연간)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "세전 예상 분배금: ${formatWon(annualGrossDiv)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("종합과세", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = state.isComprehensiveTax,
                            onCheckedChange = { onSimChange(state.simPrincipalWon, state.simYears, state.simGrowthPct, it) },
                        )
                    }
                }
            }
        }

        items(taxRows, key = { it.accountName }) { row ->
            EditorialCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(row.accountName, style = MaterialTheme.typography.titleSmall)
                        Text(row.taxRateLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "실수령 ${formatWon(row.netDividendWon)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        if (row.taxSavedWon > 0L) {
                            Text(
                                text = "절세 +${formatWon(row.taxSavedWon)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = WealthColors.ForestEmerald,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RocRatioBar(label: String, pct: Double, color: androidx.compose.ui.graphics.Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("${String.format(Locale.US, "%.1f", pct)}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(WealthColors.ChampagneContainer, RoundedCornerShape(4.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((pct / 100.0).toFloat().coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(color, RoundedCornerShape(4.dp)),
            )
        }
    }
}
