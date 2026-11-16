package io.github.hdlee73.etftracker.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hdlee73.etftracker.domain.analytics.EtfAnalyticsEngine
import io.github.hdlee73.etftracker.domain.model.RebalanceAction
import io.github.hdlee73.etftracker.ui.common.EditorialCard
import io.github.hdlee73.etftracker.ui.common.SectionEditorialHeader
import io.github.hdlee73.etftracker.ui.common.StatusPill
import io.github.hdlee73.etftracker.ui.common.VennOverlapDiagram
import io.github.hdlee73.etftracker.ui.common.formatSignedPct
import io.github.hdlee73.etftracker.ui.theme.WealthColors
import io.github.hdlee73.etftracker.ui.viewmodel.MainUiState
import java.util.Locale

@Composable
fun PdfXRayScreen(
    state: MainUiState,
    onSelectOverlapPair: (String, String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val selected = state.selectedEtf

    val etfA = state.etfs.firstOrNull { it.code == state.overlapCodeA } ?: state.etfs.first()
    val etfB = state.etfs.firstOrNull { it.code == state.overlapCodeB } ?: state.etfs.last()
    val overlap = remember(etfA, etfB) { EtfAnalyticsEngine.analyzeOverlap(etfA, etfB) }
    val rebalanceEvents = remember(selected) { EtfAnalyticsEngine.detectRebalancing(selected.holdings) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("기여도 X-Ray") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("중복도 분석") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("리밸런싱") })
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        SectionEditorialHeader(
                            eyebrow = "Portfolio Deposit File",
                            title = "${selected.name} 구성종목 기여도",
                            subtitle = "개별 종목의 비중과 금일 등락 기여도(%p)를 분해해 보여줍니다.",
                        )
                    }
                    items(selected.holdings.sortedByDescending { it.weightPct }, key = { "${it.code}_${it.name}" }) { h ->
                        EditorialCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = h.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${h.code} · 보유 ${h.shareCount}주 (전일 대비 ${if (h.shareDelta >= 0) "+" else ""}${h.shareDelta}주)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", h.weightPct)}%",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "기여 ${formatSignedPct(h.contributionPctPoint, "%p")}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (h.contributionPctPoint >= 0) WealthColors.ForestEmerald else WealthColors.DeepCrimson,
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (h.weightPct / 30.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = WealthColors.ClassicNavy,
                                trackColor = WealthColors.ChampagneContainer,
                            )
                        }
                    }
                }

                1 -> {
                    item {
                        SectionEditorialHeader(
                            eyebrow = "Overlap Analysis",
                            title = "ETF 간 중복도 및 합산 노출",
                            subtitle = "비교할 두 ETF를 선택하면 교집합 비중과 실제 합산 노출도를 계산합니다.",
                        )
                    }
                    item {
                        EditorialCard {
                            Text("ETF A 선택", style = MaterialTheme.typography.labelMedium, color = WealthColors.ChampagneGold)
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.etfs, key = { "A_${it.code}" }) { etf ->
                                    val active = etf.code == etfA.code
                                    Surface(
                                        modifier = Modifier.clickable { onSelectOverlapPair(etf.code, etfB.code) },
                                        shape = RoundedCornerShape(50),
                                        color = if (active) WealthColors.ClassicNavy else WealthColors.ChampagneContainer,
                                    ) {
                                        Text(
                                            text = etf.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (active) MaterialTheme.colorScheme.onPrimary else WealthColors.ClassicNavy,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("ETF B 선택", style = MaterialTheme.typography.labelMedium, color = WealthColors.ForestEmerald)
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.etfs, key = { "B_${it.code}" }) { etf ->
                                    val active = etf.code == etfB.code
                                    Surface(
                                        modifier = Modifier.clickable { onSelectOverlapPair(etfA.code, etf.code) },
                                        shape = RoundedCornerShape(50),
                                        color = if (active) WealthColors.ForestEmerald else WealthColors.EmeraldContainer,
                                    ) {
                                        Text(
                                            text = etf.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (active) MaterialTheme.colorScheme.onTertiary else WealthColors.ForestEmerald,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        EditorialCard {
                            VennOverlapDiagram(
                                etfAName = overlap.etfAName,
                                etfBName = overlap.etfBName,
                                overlapPct = overlap.totalOverlapPct,
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "공통 편입 종목 ${overlap.commonCount}개 · 합산 노출 비중(50:50 투자 기준)은 아래 바 차트에서 확인하세요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(overlap.items, key = { it.code }) { item ->
                        EditorialCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(item.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "합산 노출 ${String.format(Locale.US, "%.1f", item.combinedExposurePct)}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = WealthColors.ClassicNavy,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .background(WealthColors.ChampagneContainer, RoundedCornerShape(4.dp)),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((item.combinedExposurePct / 25.0).toFloat().coerceIn(0.05f, 1f))
                                            .height(8.dp)
                                            .background(WealthColors.ForestEmerald, RoundedCornerShape(4.dp)),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "A ${String.format(Locale.US, "%.1f", item.weightA)}% / B ${String.format(Locale.US, "%.1f", item.weightB)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        SectionEditorialHeader(
                            eyebrow = "Active Rebalancing Radar",
                            title = "${selected.name} 전일 대비 리밸런싱",
                            subtitle = if (selected.isActive) "액티브 ETF 운용역의 수량 증감 및 편입·편출 내역입니다."
                            else "패시브 지수 추종 ETF의 일별 PDF 수량 미세 조정 내역입니다.",
                        )
                    }
                    if (rebalanceEvents.isEmpty()) {
                        item {
                            EditorialCard {
                                Text("전일 대비 유의미한 PDF 주식 수량 변동이 없습니다.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        items(rebalanceEvents, key = { it.stockCode }) { ev ->
                            EditorialCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(ev.stockName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "수량 변화: ${if (ev.shareDelta >= 0) "+" else ""}${ev.shareDelta}주 · 비중 ${formatSignedPct(ev.weightDeltaPct)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    StatusPill(
                                        text = ev.action.label,
                                        containerColor = when (ev.action) {
                                            RebalanceAction.ADDED, RebalanceAction.INCREASED -> WealthColors.EmeraldContainer
                                            RebalanceAction.REMOVED, RebalanceAction.DECREASED -> WealthColors.CrimsonContainer
                                        },
                                        contentColor = when (ev.action) {
                                            RebalanceAction.ADDED, RebalanceAction.INCREASED -> WealthColors.ForestEmerald
                                            RebalanceAction.REMOVED, RebalanceAction.DECREASED -> WealthColors.DeepCrimson
                                        },
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
