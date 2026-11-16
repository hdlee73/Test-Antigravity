package io.github.hdlee73.etftracker.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EtfSearchItemDto(
    val code: String = "",
    val name: String = "",
    @SerialName("index_name") val indexName: String = "",
    val manager: String = "",
    val price: Long? = null,
    val close: Long? = null,
    @SerialName("change_rate") val changeRate: Double? = null,
    val nav: Double? = null,
    val volume: Long? = null,
    @SerialName("net_assets") val netAssets: Long? = null,
) {
    val resolvedPrice: Long get() = price ?: close ?: 0L
}

@Serializable
data class EtfSearchResponseDto(
    val items: List<EtfSearchItemDto> = emptyList(),
)

@Serializable
data class EtfQuoteDto(
    val price: Long? = null,
    @SerialName("change_rate") val changeRate: Double? = null,
    val volume: Long? = null,
)

@Serializable
data class EtfDailyPointDto(
    val date: String = "",
    val close: Long? = null,
    @SerialName("change_rate") val changeRate: Double? = null,
    val volume: Long? = null,
    val nav: Double? = null,
    val disparity: Double? = null,
    @SerialName("tracking_error") val trackingError: Double? = null,
)

@Serializable
data class EtfBasicsDto(
    @SerialName("base_date") val baseDate: String = "",
    @SerialName("net_assets") val netAssets: Long? = null,
    @SerialName("listed_shares") val listedShares: Long? = null,
    @SerialName("market_cap") val marketCap: Long? = null,
    @SerialName("nav_krx") val navKrx: Double? = null,
    val manager: String = "",
    @SerialName("total_fee") val totalFee: Double? = null,
    @SerialName("listed_date") val listedDate: String = "",
    val replication: String = "",
    @SerialName("asset_class") val assetClass: String = "",
    val source: String = "",
)

/**
 * EveryCorp `/api/v1/etfs/{code}` 실제 중첩 응답 구조.
 */
@Serializable
data class EtfDetailResponseDto(
    val code: String = "",
    val name: String = "",
    @SerialName("index_name") val indexName: String = "",
    @SerialName("tax_type") val taxType: String = "",
    val quote: EtfQuoteDto? = null,
    val daily: List<EtfDailyPointDto> = emptyList(),
    val basics: EtfBasicsDto? = null,
)

@Serializable
data class EtfChartCandleDto(
    val date: String = "",
    val open: Long? = null,
    val high: Long? = null,
    val low: Long? = null,
    val close: Long? = null,
    val volume: Long? = null,
)

@Serializable
data class EtfChartResponseDto(
    val code: String = "",
    val name: String = "",
    val items: List<EtfChartCandleDto> = emptyList(),
    val candles: List<EtfChartCandleDto> = emptyList(),
) {
    val allPoints: List<EtfChartCandleDto> get() = items.ifEmpty { candles }
}

@Serializable
data class EtfHoldingDto(
    val code: String = "",
    val name: String = "",
    val qty: Double? = null,
    val value: Long? = null,
    val weight: Double? = null,
    val kind: String = "STOCK",
    @SerialName("corp_name") val corpName: String? = null,
    @SerialName("stock_code") val stockCode: String? = null,
) {
    val displayName: String get() = corpName?.takeIf { it.isNotBlank() } ?: name.ifBlank { code }
}

@Serializable
data class EtfPortfolioDto(
    val code: String = "",
    val date: String = "",
    val source: String = "",
    val count: Int = 0,
    @SerialName("total_weight") val totalWeight: Double = 0.0,
    val holdings: List<EtfHoldingDto> = emptyList(),
)

@Serializable
data class EtfOverlapResponseDto(
    @SerialName("overlap_weight") val overlapWeight: Double = 0.0,
    @SerialName("common_count") val commonCount: Int = 0,
    val common: List<EtfHoldingDto> = emptyList(),
)

@Serializable
data class EtfRankItemDto(
    val rank: Int = 0,
    val code: String = "",
    val name: String = "",
    val manager: String = "",
    val close: Long? = null,
    val nav: Double? = null,
    @SerialName("net_assets") val netAssets: Long? = null,
    @SerialName("market_cap") val marketCap: Long? = null,
    @SerialName("net_assets_change_pct") val netAssetsChangePct: Double? = null,
)

@Serializable
data class EtfRankingResponseDto(
    val date: String = "",
    val total: Int = 0,
    val items: List<EtfRankItemDto> = emptyList(),
)

@Serializable
data class EtfStatsDto(
    val code: String = "",
    val name: String = "",
    val manager: String = "",
    val dates: List<String> = emptyList(),
    val close: List<Long?> = emptyList(),
    val nav: List<Double?> = emptyList(),
    @SerialName("net_assets") val netAssets: List<Long?> = emptyList(),
)

@Serializable
data class HealthResponseDto(
    val status: String = "ok",
    val version: String = "",
)
