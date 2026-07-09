package uk.dubai.mall.maps.construction.data.repository

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import uk.dubai.mall.maps.construction.data.model.AppConfig
import uk.dubai.mall.maps.construction.data.model.Mall
import uk.dubai.mall.maps.construction.data.network.ApiService
import java.io.File

class MallRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    private val _localMalls = MutableStateFlow<List<Mall>>(emptyList())
    val localMalls: StateFlow<List<Mall>> = _localMalls.asStateFlow()

    private val gson = Gson()
    private val cacheFile = File(context.filesDir, "malls_cache.json")

    init {
        loadCache()
    }

    private fun loadCache() {
        if (cacheFile.exists()) {
            try {
                val json = cacheFile.readText()
                val config = gson.fromJson(json, AppConfig::class.java)
                _localMalls.value = config.malls
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchAppConfig(): Result<AppConfig> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getConfig()
                // Cache to file
                cacheFile.writeText(gson.toJson(response))
                _localMalls.value = response.malls
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
