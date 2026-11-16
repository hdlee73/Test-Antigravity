package io.github.hdlee73.etftracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hdlee73.etftracker.domain.model.EtfSummary
import io.github.hdlee73.etftracker.domain.model.NewsImpactItem
import io.github.hdlee73.etftracker.domain.model.NewsSentiment
import io.github.hdlee73.etftracker.ui.common.EditorialCard
import io.github.hdlee73.etftracker.ui.common.MultiLineChart
import io.github.hdlee73.etftracker.ui.common.SectionEditorialHeader
import io.github.hdlee73.etftracker.ui.common.StatusPill
import io.github.hdlee73.etftracker.ui.theme.WealthColors
import io.github.hdlee73.etftracker.ui.viewmodel.MainUiState
import java.util.Locale

@Composable
fun FoldCompareScreen(
    state: MainUiState,
    onToggleCompareCode: (String) -> Unit,
) {
    val comparedEtfs = remember(state.etfs, state.compareCodes) {
        state.compareCodes.mapNotNull { code -> state.etfs.firstOrNull { it.code == code } }
            .ifEmpty { listOf(state.etfs.first()) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDualPane = maxWidth >= 600.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 비교 바구니 선택 바
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                SectionEditorialHeader(
                    eyebrow = if (isDualPane) "Fold8 Dual-Pane Active" else "Multi-ETF Compare",
                    title = "1:N 멀티 비교 & AI 뉴스 임팩트",
                    subtitle = "최대 4개 ETF를 선택해 핵심 지표와 수익률 겹침, 뉴스 임팩트를 비교합니다.",
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.etfs, key = { "cmp_${it.code}" }) { etf ->
                        val active = state.compareCodes.contains(etf.code)
                        Surface(
                            modifier = Modifier.clickable { onToggleCompareCode(etf.code) },
                            shape = RoundedCornerShape(50),
                            color = if (active) WealthColors.ClassicNavy else WealthColors.ChampagneContainer,
                        ) {
                            Text(
                                text = (if (active) "✓ " else "") + etf.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else WealthColors.ClassicNavy,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }

            if (isDualPane) {
                // 폴드8 펼침 / 윈도우 데스크톱: 좌우 분할 Dual-Pane
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(0.46f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item { CompareTableCard(comparedEtfs) }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(0.54f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item { CompareChartAndPdfDiffCard(comparedEtfs) }
                        item { Text("AI 뉴스-구성종목 임팩트 맵핑", style = MaterialTheme.typography.titleMedium) }
                        items(state.newsImpacts, key = { it.id }) { news -> NewsImpactCard(news) }
                    }
                }
            } else {
                // 스마트폰(갤럭시 S23) 단일 세로 스크롤
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { CompareTableCard(comparedEtfs) }
                    item { CompareChartAndPdfDiffCard(comparedEtfs) }
                    item { Text("AI 뉴스-구성종목 임팩트 맵핑", style = MaterialTheme.typography.titleMedium) }
                    items(state.newsImpacts, key = { it.id }) { news -> NewsImpactCard(news) }
                }
            }
        }
    }
}

@Composable
private fun CompareTableCard(etfs: List<EtfSummary>) {
    EditorialCard {
        Text("핵심 지표 비교 테이블", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        etfs.forEach { etf ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1.3f)) {
                    Text(etf.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "AUM ${etf.aumBillionWon}억 · 거래 ${etf.tradingValueBillionWon}억",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "비용 ${String.format(Locale.US, "%.2f", etf.realCostPct)}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "MDD ${String.format(Locale.US, "%.1f", etf.mddPct)}% · 샤프 ${String.format(Locale.US, "%.2f", etf.sharpeRatio)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WealthColors.WarmSlate,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareChartAndPdfDiffCard(etfs: List<EtfSummary>) {
    EditorialCard {
        Text("수익률 겹침 차트 & 상위 구성종목 차이", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        MultiLineChart(
            series = etfs.map { it.name to it.dailyPrices },
            colors = listOf(
                WealthColors.ClassicNavy,
                WealthColors.ForestEmerald,
                WealthColors.ChampagneGold,
                WealthColors.DeepCrimson,
            ),
        )
        Spacer(Modifier.height(12.dp))
        etfs.forEach { etf ->
            val top3 = etf.holdings.take(3).joinToString(", ") { "${it.name}(${String.format(Locale.US, "%.1f", it.weightPct)}%)" }
            Text(
                text = "• ${etf.name}: $top3",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NewsImpactCard(news: NewsImpactItem) {
    EditorialCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${news.source} · ${news.timeLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            StatusPill(
                text = news.sentiment.label,
                containerColor = when (news.sentiment) {
                    NewsSentiment.POSITIVE -> WealthColors.EmeraldContainer
                    NewsSentiment.NEGATIVE -> WealthColors.CrimsonContainer
                    NewsSentiment.NEUTRAL -> WealthColors.ChampagneContainer
                },
                contentColor = when (news.sentiment) {
                    NewsSentiment.POSITIVE -> WealthColors.ForestEmerald
                    NewsSentiment.NEGATIVE -> WealthColors.DeepCrimson
                    NewsSentiment.NEUTRAL -> WealthColors.ClassicNavy
                },
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(news.headline, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text("관련 편입종목: ${news.relatedStocks.joinToString(", ")}", style = MaterialTheme.typography.labelMedium, color = WealthColors.ChampagneGold)
        Spacer(Modifier.height(4.dp))
        Text(news.impactSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
