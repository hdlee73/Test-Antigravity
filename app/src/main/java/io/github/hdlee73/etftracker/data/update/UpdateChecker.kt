package io.github.hdlee73.etftracker.data.update

import io.github.hdlee73.etftracker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class GitHubAssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
)

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val assets: List<GitHubAssetDto> = emptyList(),
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
    ) : UpdateCheckResult

    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Failed(val reason: String) : UpdateCheckResult
}

object UpdateChecker {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun checkLatest(
        repo: String = BuildConfig.GITHUB_REPO,
        currentVersion: String = BuildConfig.VERSION_NAME.removeSuffix("-debug"),
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$repo/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return@withContext UpdateCheckResult.UpToDate(currentVersion)
                }
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Failed("GitHub API 응답 오류 (${response.code})")
                }
                val body = response.body?.string().orEmpty()
                val dto = json.decodeFromString<GitHubReleaseDto>(body)
                val latest = dto.tagName.trim().removePrefix("v")
                if (isNewerVersion(latest, currentVersion)) {
                    val apkUrl = dto.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }?.downloadUrl
                        ?: dto.htmlUrl
                    UpdateCheckResult.UpdateAvailable(
                        currentVersion = currentVersion,
                        latestVersion = latest,
                        releaseNotes = dto.body.ifBlank { "새로운 버전이 출시되었습니다." },
                        downloadUrl = apkUrl,
                    )
                } else {
                    UpdateCheckResult.UpToDate(currentVersion)
                }
            }
        }.getOrElse { e ->
            UpdateCheckResult.Failed(e.message ?: "업데이트 확인 중 네트워크 오류가 발생했습니다.")
        }
    }

    internal fun isNewerVersion(candidate: String, current: String): Boolean {
        val candParts = candidate.split(".", "-").mapNotNull { it.toIntOrNull() }
        val currParts = current.split(".", "-").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(candParts.size, currParts.size)
        for (i in 0 until maxLen) {
            val c = candParts.getOrElse(i) { 0 }
            val cur = currParts.getOrElse(i) { 0 }
            if (c > cur) return true
            if (c < cur) return false
        }
        return false
    }
}
