package io.github.hdlee73.etftracker.data.prefs

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.github.hdlee73.etftracker.BuildConfig
import io.github.hdlee73.etftracker.data.log.CrashLogger
import io.github.hdlee73.etftracker.data.repo.SampleEtfCatalog
import io.github.hdlee73.etftracker.domain.model.HoldingPosition
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("etf_tracker_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    var apiBaseUrl: String
        get() = prefs.getString(KEY_API_URL, BuildConfig.API_BASE_URL).orEmpty().ifBlank { BuildConfig.API_BASE_URL }
        set(value) = prefs.edit().putString(KEY_API_URL, value.trim().trimEnd('/')).apply()

    var appToken: String
        get() {
            val encoded = prefs.getString(KEY_APP_TOKEN_ENC, null)
            val ivBase64 = prefs.getString(KEY_APP_TOKEN_IV, null)
            if (encoded != null && ivBase64 != null) {
                return decrypt(encoded, ivBase64).orEmpty().ifBlank { BuildConfig.APP_TOKEN }
            }
            return BuildConfig.APP_TOKEN
        }
        set(value) {
            val trimmed = value.trim()
            if (trimmed.isEmpty()) {
                prefs.edit().remove(KEY_APP_TOKEN_ENC).remove(KEY_APP_TOKEN_IV).apply()
                return
            }
            val pair = encrypt(trimmed)
            if (pair != null) {
                prefs.edit()
                    .putString(KEY_APP_TOKEN_ENC, pair.first)
                    .putString(KEY_APP_TOKEN_IV, pair.second)
                    .apply()
            }
        }

    var selectedEtfCode: String
        get() = prefs.getString(KEY_SELECTED_ETF, "360750").orEmpty().ifBlank { "360750" }
        set(value) = prefs.edit().putString(KEY_SELECTED_ETF, value).apply()

    var compareCodes: List<String>
        get() = prefs.getString(KEY_COMPARE_CODES, "360750,496120,091180")
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("360750", "496120", "091180") }
        set(value) = prefs.edit().putString(KEY_COMPARE_CODES, value.take(4).joinToString(",")).apply()

    var removedEtfCodes: Set<String>
        get() = prefs.getStringSet(KEY_REMOVED_ETFS, emptySet()).orEmpty()
        set(value) = prefs.edit().putStringSet(KEY_REMOVED_ETFS, value).apply()

    /**
     * 사용자가 검색해서 관심종목으로 추가 저장한 ETF 코드 목록.
     */
    var savedWatchlistCodes: Set<String>
        get() = prefs.getStringSet(KEY_SAVED_WATCHLIST, emptySet()).orEmpty()
        set(value) = prefs.edit().putStringSet(KEY_SAVED_WATCHLIST, value).apply()

    /**
     * 사용자가 보유 중인 ETF 포지션(평단가·수량·목표수익률·손절선) 목록.
     */
    var holdings: List<HoldingPosition>
        get() {
            val raw = prefs.getString(KEY_HOLDINGS_JSON, null)
            if (raw.isNullOrBlank()) return SampleEtfCatalog.defaultHoldings
            return runCatching {
                json.decodeFromString<List<HoldingPosition>>(raw)
            }.getOrDefault(SampleEtfCatalog.defaultHoldings)
        }
        set(value) {
            runCatching {
                prefs.edit().putString(KEY_HOLDINGS_JSON, json.encodeToString(value)).apply()
            }
        }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): Pair<String, String>? {
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val encB64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            val ivB64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            encB64 to ivB64
        }.onFailure {
            CrashLogger.logWarning("AppPrefs.encrypt", "토큰 암호화 실패", it)
        }.getOrNull()
    }

    private fun decrypt(encB64: String, ivB64: String): String? {
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            val plainBytes = cipher.doFinal(Base64.decode(encB64, Base64.NO_WRAP))
            String(plainBytes, Charsets.UTF_8)
        }.onFailure {
            CrashLogger.logWarning("AppPrefs.decrypt", "토큰 복호화 실패", it)
        }.getOrNull()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "etf_tracker_token_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_API_URL = "api_base_url"
        private const val KEY_APP_TOKEN_ENC = "app_token_enc"
        private const val KEY_APP_TOKEN_IV = "app_token_iv"
        private const val KEY_SELECTED_ETF = "selected_etf"
        private const val KEY_COMPARE_CODES = "compare_codes"
        private const val KEY_REMOVED_ETFS = "removed_etfs"
        private const val KEY_SAVED_WATCHLIST = "saved_watchlist"
        private const val KEY_HOLDINGS_JSON = "holdings_json"
    }
}
