package io.github.hdlee73.etftracker.data.api

import io.github.hdlee73.etftracker.data.api.dto.EtfBasicsDto
import io.github.hdlee73.etftracker.data.api.dto.EtfOverlapResponseDto
import io.github.hdlee73.etftracker.data.api.dto.EtfPortfolioDto
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
    suspend fun searchEtfs(@Query("q") query: String): EtfSearchResponseDto

    @GET("api/v1/etfs/{code}")
    suspend fun getEtfBasics(@Path("code") code: String): EtfBasicsDto

    @GET("api/v1/etfs/{code}/portfolio")
    suspend fun getEtfPortfolio(@Path("code") code: String): EtfPortfolioDto

    @GET("api/v1/etfs/{code}/stats")
    suspend fun getEtfStats(@Path("code") code: String): EtfStatsDto

    @GET("api/v1/etfs/overlap")
    suspend fun getEtfOverlap(
        @Query("a") codeA: String,
        @Query("b") codeB: String,
    ): EtfOverlapResponseDto
}
