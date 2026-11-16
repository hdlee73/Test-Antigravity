package io.github.hdlee73.etftracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hdlee73.etftracker.ui.screens.AppInfoScreen
import io.github.hdlee73.etftracker.ui.screens.BacktestCorrelationScreen
import io.github.hdlee73.etftracker.ui.screens.CostDiscrepancyScreen
import io.github.hdlee73.etftracker.ui.screens.DashboardScreen
import io.github.hdlee73.etftracker.ui.screens.DividendTaxScreen
import io.github.hdlee73.etftracker.ui.screens.FoldCompareScreen
import io.github.hdlee73.etftracker.ui.screens.PdfXRayScreen
import io.github.hdlee73.etftracker.ui.theme.EtfTrackerTheme
import io.github.hdlee73.etftracker.ui.theme.WealthColors
import io.github.hdlee73.etftracker.ui.viewmodel.AppRoute
import io.github.hdlee73.etftracker.ui.viewmodel.EtfTrackerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EtfTrackerTheme {
                EtfTrackerRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtfTrackerRoot(viewModel: EtfTrackerViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 필수 규칙 2-(3): 드로어가 열려 있거나 서브 페이지에 있을 때 뒤로가기로 부드럽게 복귀
    BackHandler(enabled = drawerState.isOpen || state.backStack.size > 1) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            viewModel.navigateBack()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
                    Text(
                        text = "ETF TRACKER",
                        style = MaterialTheme.typography.labelMedium,
                        color = WealthColors.ChampagneGold,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Private Wealth Edition",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (state.isServerConnected) "EveryCorp 서버 연동 중" else "로컬 분석 모드",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.isServerConnected) WealthColors.ForestEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))

                AppRoute.entries.forEach { route ->
                    NavigationDrawerItem(
                        icon = { Icon(routeIcon(route), contentDescription = null) },
                        label = { Text(route.title) },
                        selected = state.currentRoute == route,
                        onClick = {
                            viewModel.navigateTo(route)
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = WealthColors.ChampagneContainer,
                            selectedIconColor = WealthColors.ClassicNavy,
                            selectedTextColor = WealthColors.ClassicNavy,
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = state.currentRoute.title,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = state.currentRoute.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Outlined.Menu, contentDescription = "메뉴 열기")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "새로고침")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Crossfade(targetState = state.currentRoute, label = "route_fade") { route ->
                    when (route) {
                        AppRoute.DASHBOARD -> DashboardScreen(
                            state = state,
                            onSearchChange = viewModel::updateSearchQuery,
                            onSelectEtf = viewModel::selectEtf,
                            onAddWatchlist = viewModel::addEtfToWatchlist,
                            onRemoveEtf = viewModel::removeEtf,
                            onUpsertHolding = viewModel::upsertHoldingPosition,
                            onRemoveHolding = viewModel::removeHoldingPosition,
                            onClearBanner = viewModel::clearBannerMessage,
                            onNavigate = viewModel::navigateTo,
                        )
                        AppRoute.PDF_XRAY -> PdfXRayScreen(
                            state = state,
                            onSelectOverlapPair = viewModel::setOverlapPair,
                        )
                        AppRoute.COST_DISCREPANCY -> CostDiscrepancyScreen(
                            state = state,
                            onInvestmentChange = viewModel::updateInvestmentAmount,
                        )
                        AppRoute.DIVIDEND_TAX -> DividendTaxScreen(
                            state = state,
                            onSimChange = viewModel::updateDividendSim,
                        )
                        AppRoute.FOLD_COMPARE -> FoldCompareScreen(
                            state = state,
                            onToggleCompareCode = viewModel::toggleCompareCode,
                        )
                        AppRoute.BACKTEST_CORR -> BacktestCorrelationScreen(state = state)
                        AppRoute.APP_INFO -> AppInfoScreen(
                            state = state,
                            onSaveServerConfig = viewModel::saveServerConfig,
                            onCheckUpdate = viewModel::checkForUpdates,
                            onRefreshLog = viewModel::refreshCrashLog,
                            onClearLog = viewModel::clearCrashLog,
                        )
                    }
                }
            }
        }
    }
}

private fun routeIcon(route: AppRoute): ImageVector = when (route) {
    AppRoute.DASHBOARD -> Icons.Outlined.Home
    AppRoute.PDF_XRAY -> Icons.Outlined.Analytics
    AppRoute.COST_DISCREPANCY -> Icons.Outlined.VerifiedUser
    AppRoute.DIVIDEND_TAX -> Icons.Outlined.Payments
    AppRoute.FOLD_COMPARE -> Icons.Outlined.CompareArrows
    AppRoute.BACKTEST_CORR -> Icons.Outlined.ShowChart
    AppRoute.APP_INFO -> Icons.Outlined.Info
}
