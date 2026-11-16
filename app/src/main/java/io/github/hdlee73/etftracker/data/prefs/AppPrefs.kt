package io.github.hdlee73.etftracker.data.prefs

import android.content.Context
import io.github.hdlee73.etftracker.BuildConfig

class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("etf_tracker_prefs", Context.MODE_PRIVATE)

    var apiBaseUrl: String
        get() = prefs.getString(KEY_API_URL, BuildConfig.API_BASE_URL).orEmpty().ifBlank { BuildConfig.API_BASE_URL }
        set(value) = prefs.edit().putString(KEY_API_URL, value.trim().trimEnd('/')).apply()

    var appToken: String
        get() = prefs.getString(KEY_APP_TOKEN, BuildConfig.APP_TOKEN).orEmpty()
        set(value) = prefs.edit().putString(KEY_APP_TOKEN, value.trim()).apply()

    var selectedEtfCode: String
        get() = prefs.getString(KEY_SELECTED_ETF, "069500").orEmpty().ifBlank { "069500" }
        set(value) = prefs.edit().putString(KEY_SELECTED_ETF, value).apply()

    var compareCodes: List<String>
        get() = prefs.getString(KEY_COMPARE_CODES, "069500,360750,458730")
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("069500", "360750", "458730") }
        set(value) = prefs.edit().putString(KEY_COMPARE_CODES, value.take(4).joinToString(",")).apply()

    companion object {
        private const val KEY_API_URL = "api_base_url"
        private const val KEY_APP_TOKEN = "app_token"
        private const val KEY_SELECTED_ETF = "selected_etf"
        private const val KEY_COMPARE_CODES = "compare_codes"
    }
}
