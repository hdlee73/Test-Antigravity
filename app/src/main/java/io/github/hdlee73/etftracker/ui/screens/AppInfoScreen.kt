package io.github.hdlee73.etftracker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.browser.customtabs.CustomTabsIntent
import io.github.hdlee73.etftracker.BuildConfig
import io.github.hdlee73.etftracker.data.update.UpdateCheckResult
import io.github.hdlee73.etftracker.ui.common.EditorialCard
import io.github.hdlee73.etftracker.ui.common.SectionEditorialHeader
import io.github.hdlee73.etftracker.ui.theme.WealthColors
import io.github.hdlee73.etftracker.ui.viewmodel.MainUiState

@Composable
fun AppInfoScreen(
    state: MainUiState,
    onSaveServerConfig: (String, String) -> Unit,
    onCheckUpdate: () -> Unit,
    onRefreshLog: () -> Unit,
    onClearLog: () -> Unit,
) {
    val context = LocalContext.current
    var urlInput by rememberSaveable(state.apiBaseUrlInput) { mutableStateOf(state.apiBaseUrlInput) }
    var tokenInput by rememberSaveable(state.appTokenInput) { mutableStateOf(state.appTokenInput) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionEditorialHeader(
                eyebrow = "About & Operations",
                title = "ETF Tracker 정보 및 연동 설정",
                subtitle = "앱 버전, 제작자 안내, 사용 매뉴얼, 자동 업데이트 및 EveryCorp 서버 설정",
            )
        }

        // 필수 규칙 2-(4): 앱 정보 및 제작자 정보
        item {
            EditorialCard {
                Text("ETF Tracker", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "현재 버전: v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WealthColors.ChampagneGold,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text("만든이: ${BuildConfig.AUTHOR_NAME}", style = MaterialTheme.typography.bodyMedium)
                Text("이메일: ${BuildConfig.AUTHOR_EMAIL}", style = MaterialTheme.typography.bodyMedium)
                Text("홈페이지: ${BuildConfig.AUTHOR_HOMEPAGE}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        runCatching {
                            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(BuildConfig.AUTHOR_HOMEPAGE))
                        }
                    },
                ) {
                    Text("제작자 홈페이지 열기")
                }
            }
        }

        // 사용 방법 매뉴얼
        item {
            EditorialCard {
                Text("사용 방법 (매뉴얼)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val manual = """
                    1. 좌측 상단 메뉴(≡) 또는 화면 왼쪽 끝 스와이프로 6개 분석 모듈을 전환합니다.
                    2. [자산 브리핑]에서 관심 ETF를 선택하면 모든 화면(PDF X-Ray, 비용·괴리율, 배당 시뮬레이터)이 해당 ETF 기준으로 동기화됩니다.
                    3. [멀티 비교]는 갤럭시 폴드8 펼침 화면 및 윈도우 환경에서 좌우 Dual-Pane으로 자동 확장됩니다.
                    4. 실시간 키움/KRX 시세를 연동하려면 아래 EveryCorp 서버 주소와 앱 토큰을 입력하세요.
                """.trimIndent()
                Text(manual, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // GitHub Releases 자동 업데이트
        item {
            EditorialCard {
                Text("자동 업데이트 (GitHub Releases)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "저장소: ${BuildConfig.GITHUB_REPO}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onCheckUpdate,
                    enabled = !state.isCheckingUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = WealthColors.ClassicNavy),
                ) {
                    if (state.isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp).width(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("최신 버전 확인")
                }

                when (val res = state.updateResult) {
                    is UpdateCheckResult.UpdateAvailable -> {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "새 버전 v${res.latestVersion} 출시! (현재 v${res.currentVersion})",
                            style = MaterialTheme.typography.titleSmall,
                            color = WealthColors.ForestEmerald,
                        )
                        Text(res.releaseNotes, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(res.downloadUrl)))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WealthColors.ForestEmerald),
                        ) {
                            Text("업데이트 APK 다운로드")
                        }
                    }

                    is UpdateCheckResult.UpToDate -> {
                        Spacer(Modifier.height(8.dp))
                        Text("현재 최신 버전(v${res.currentVersion})을 사용 중입니다.", style = MaterialTheme.typography.bodySmall, color = WealthColors.ForestEmerald)
                    }

                    is UpdateCheckResult.Failed -> {
                        Spacer(Modifier.height(8.dp))
                        Text(res.reason, style = MaterialTheme.typography.bodySmall, color = WealthColors.DeepCrimson)
                    }

                    null -> Unit
                }
            }
        }

        // EveryCorp 서버 연동 설정
        item {
            EditorialCard {
                Text("EveryCorp 백엔드 서버 연동", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(state.statusMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "보안을 위해 현재 적용 중인 서버 URL과 토큰은 화면에 표시하지 않습니다. 변경이 필요할 때만 아래에 새로 입력하세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WealthColors.ChampagneGold,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("새 서버 URL (비워두면 기존 값 유지)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                var showToken by rememberSaveable { mutableStateOf(false) }
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("새 X-App-Token (비워두면 기존 값 유지)") },
                    singleLine = true,
                    visualTransformation = if (showToken) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        androidx.compose.ui.text.input.PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        androidx.compose.material3.TextButton(onClick = { showToken = !showToken }) {
                            Text(if (showToken) "숨김" else "보기")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        onSaveServerConfig(urlInput, tokenInput)
                        urlInput = ""
                        tokenInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WealthColors.ClassicNavy),
                ) {
                    Text("서버 설정 저장 및 재연동")
                }
            }
        }

        // 로컬 크래시·오류 로그
        item {
            EditorialCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("로컬 오류/크래시 로그", style = MaterialTheme.typography.titleMedium)
                    Row {
                        OutlinedButton(onClick = onRefreshLog) { Text("새로고침") }
                        Spacer(Modifier.width(6.dp))
                        OutlinedButton(onClick = onClearLog) { Text("비우기") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.crashLogText.takeLast(2000),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
