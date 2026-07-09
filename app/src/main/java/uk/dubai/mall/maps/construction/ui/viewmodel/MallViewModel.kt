package uk.dubai.mall.maps.construction.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.dubai.mall.maps.construction.data.model.AppConfig
import uk.dubai.mall.maps.construction.data.model.Mall
import uk.dubai.mall.maps.construction.data.repository.MallRepository
import uk.dubai.mall.maps.construction.utils.LocationHelper

class MallViewModel(
    private val repository: MallRepository,
    private val locationHelper: LocationHelper,
    context: Context
) : ViewModel() {
    private val _config = MutableStateFlow<AppConfig?>(null)
    val config: StateFlow<AppConfig?> = _config.asStateFlow()

    private val _malls = MutableStateFlow<List<Mall>>(emptyList())
    val malls: StateFlow<List<Mall>> = _malls.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    
    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_completed", false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()
    
    private val _tripMallIds = MutableStateFlow(
        prefs.getString("trip_mall_ids", "")?.split(",")?.filter { it.isNotEmpty() }?.map { it.toInt() }?.toSet() ?: emptySet()
    )
    
    val tripMalls: StateFlow<List<Mall>> = combine(_malls, _tripMallIds) { allMalls, tripIds ->
        allMalls.filter { it.id in tripIds }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Observe local DB
        viewModelScope.launch {
            repository.localMalls.collect { localList ->
                _malls.value = localList
                if (localList.isNotEmpty()) {
                    _isLoading.value = false
                    _error.value = null
                }
            }
        }
        fetchConfig()
    }

    fun fetchConfig() {
        viewModelScope.launch {
            if (_malls.value.isEmpty()) {
                _isLoading.value = true
            }
            repository.fetchAppConfig()
                .onSuccess { appConfig ->
                    _config.value = appConfig
                    _isLoading.value = false
                    _error.value = null
                }
                .onFailure { e ->
                    if (_malls.value.isEmpty()) {
                        _error.value = e.message ?: "An unknown error occurred"
                    }
                    _isLoading.value = false
                }
        }
    }

    fun toggleFavorite(mallId: Int) {
        _malls.update { currentList ->
            currentList.map { mall ->
                if (mall.id == mallId) {
                    mall.copy(isFavorite = !(mall.isFavorite ?: false))
                } else {
                    mall
                }
            }
        }
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        _isOnboardingCompleted.value = true
    }

    fun fetchUserLocation() {
        viewModelScope.launch {
            val location = locationHelper.getCurrentLocation()
            if (location != null) {
                _userLocation.value = location
            }
        }
    }
    
    fun toggleTripMall(mallId: Int) {
        _tripMallIds.update { currentSet ->
            val newSet = if (currentSet.contains(mallId)) {
                currentSet - mallId
            } else {
                currentSet + mallId
            }
            prefs.edit().putString("trip_mall_ids", newSet.joinToString(",")).apply()
            newSet
        }
    }
}
