package io.github.hdlee73.etftracker.data.api

import io.github.hdlee73.etftracker.data.api.dto.EtfChartResponseDto
import io.github.hdlee73.etftracker.data.api.dto.EtfDetailResponseDto
import io.github.hdlee73.etftracker.data.api.dto.EtfOverlapResponseDto
import io.github.hdlee73.etftracker.data.api.dto.EtfPortfolioDto
import io.github.hdlee73.etftracker.data.api.dto.EtfRankingResponseDto
import io.github.hdlee73.etftracker.data.api.dto.EtfSearchResponseDto
import io.github.hdlee73.etftracker.data.api.dto.EtfStatsDto
import io.github.hdlee73.etftracker.data.api.dto.HealthResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface EveryCorpApi {
    @GET("api/v1/health")
    suspend fun health(): HealthResponseDto

    @GET("api/v1/etfs/search")
    suspend fun searchEtfs(
        @Query("q") query: String,
        @Query("limit") limit: Int = 30,
    ): EtfSearchResponseDto

    @GET("api/v1/etfs/stats/ranking")
    suspend fun getEtfRanking(
        @Query("sort") sort: String = "net_assets",
        @Query("period") period: Int = 30,
        @Query("limit") limit: Int = 50,
    ): EtfRankingResponseDto

    @GET("api/v1/etfs/{code}")
    suspend fun getEtfDetail(@Path("code") code: String): EtfDetailResponseDto

    @GET("api/v1/etfs/{code}/chart")
    suspend fun getEtfChart(
        @Path("code") code: String,
        @Query("days") days: Int = 365,
    ): EtfChartResponseDto

    @GET("api/v1/etfs/{code}/portfolio")
    suspend fun getEtfPortfolio(@Path("code") code: String): EtfPortfolioDto

    @GET("api/v1/etfs/{code}/stats")
    suspend fun getEtfStats(
        @Path("code") code: String,
        @Query("days") days: Int = 365,
    ): EtfStatsDto

    @GET("api/v1/etfs/overlap")
    suspend fun getEtfOverlap(
        @Query("a") codeA: String,
        @Query("b") codeB: String,
    ): EtfOverlapResponseDto
}
