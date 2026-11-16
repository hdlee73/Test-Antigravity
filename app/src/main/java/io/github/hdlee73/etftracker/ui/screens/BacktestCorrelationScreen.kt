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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hdlee73.etftracker.data.repo.SampleEtfCatalog
import io.github.hdlee73.etftracker.domain.analytics.EtfAnalyticsEngine
import io.github.hdlee73.etftracker.ui.common.EditorialCard
import io.github.hdlee73.etftracker.ui.common.MultiLineChart
import io.github.hdlee73.etftracker.ui.common.SectionEditorialHeader
import io.github.hdlee73.etftracker.ui.common.StatusPill
import io.github.hdlee73.etftracker.ui.theme.WealthColors
import io.github.hdlee73.etftracker.ui.viewmodel.MainUiState
import java.util.Locale

@Composable
fun BacktestCorrelationScreen(state: MainUiState) {
    val stressRows = remember(state.etfs) {
        state.etfs.map { EtfAnalyticsEngine.calculateStressTest(it) }
    }
    val corrMatrix = remember {
        EtfAnalyticsEngine.buildCorrelationMatrix(SampleEtfCatalog.correlationSeries)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionEditorialHeader(
                eyebrow = "Stress Test & Correlation",
                title = "위기 MDD 회복력 & 상관계수 매트릭스",
                subtitle = "과거 급락장 최대낙폭(MDD)·원금 회복 기간과 환율/대표지수 상관계수를 분석합니다.",
            )
        }

        item {
            EditorialCard {
                Text("위기 구간 Drawdown(수면 아래) 곡선", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                MultiLineChart(
                    series = stressRows.take(3).map { it.name to it.drawdownSeries },
                    colors = listOf(WealthColors.ClassicNavy, WealthColors.ForestEmerald, WealthColors.DeepCrimson),
                )
            }
        }

        items(stressRows, key = { it.code }) { row ->
            EditorialCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "최대낙폭 ${String.format(Locale.US, "%.1f", row.mddPct)}% · 회복 기간 약 ${row.underwaterDays}일",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusPill(
                        text = if (row.recovered) "전고점 회복" else "회복 진행중",
                        containerColor = if (row.recovered) WealthColors.EmeraldContainer else WealthColors.ChampagneContainer,
                        contentColor = if (row.recovered) WealthColors.ForestEmerald else WealthColors.ChampagneGold,
                    )
                }
            }
        }

        item {
            EditorialCard {
                Text("기초자산-ETF 상관계수 히트맵", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "환노출(UH) vs 환헤지(H), 원/달러 환율, 코스피 200 간 피어슨 상관계수 (-1.0 ~ +1.0)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.width(74.dp))
                    corrMatrix.labels.forEach { label ->
                        Text(
                            text = label.take(6),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                corrMatrix.labels.forEachIndexed { rIdx, rowLabel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = rowLabel.take(8),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(74.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        corrMatrix.values[rIdx].forEach { value ->
                            val bg = when {
                                value >= 0.7 -> WealthColors.ForestEmerald.copy(alpha = 0.85f)
                                value >= 0.3 -> WealthColors.ForestEmerald.copy(alpha = 0.40f)
                                value <= -0.3 -> WealthColors.DeepCrimson.copy(alpha = 0.55f)
                                else -> WealthColors.ChampagneContainer
                            }
                            val fg = if (value >= 0.7 || value <= -0.3) Color.White else WealthColors.ClassicNavy
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .background(bg, RoundedCornerShape(6.dp))
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.2f", value),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = fg,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
