package uk.dubai.mall.maps.construction.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import uk.dubai.mall.maps.construction.data.model.AppConfig

interface ApiService {
    @GET("amrimarihotjati/dubai-mallmaps-antigravity/main/config.json")
    suspend fun getConfig(): AppConfig

    companion object {
        private const val BASE_URL = "https://raw.githubusercontent.com/"

        fun create(): ApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
