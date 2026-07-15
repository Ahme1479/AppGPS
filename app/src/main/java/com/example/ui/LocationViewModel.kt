package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LocationRepository
import com.example.data.MockLocationEntity
import com.example.service.MockLocationService
import com.example.utils.CountriesData
import com.example.utils.CountryInfo
import com.example.utils.parseDoubleSafe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = LocationRepository(db.locationDao())

    // UI Input State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _customLatitude = MutableStateFlow("")
    val customLatitude: StateFlow<String> = _customLatitude.asStateFlow()

    private val _customLongitude = MutableStateFlow("")
    val customLongitude: StateFlow<String> = _customLongitude.asStateFlow()

    private val _selectedCountry = MutableStateFlow<CountryInfo?>(null)
    val selectedCountry: StateFlow<CountryInfo?> = _selectedCountry.asStateFlow()

    // Filtered countries list reactive state
    val filteredCountries: StateFlow<List<CountryInfo>> = _searchQuery
        .map { query ->
            if (query.isBlank()) {
                CountriesData.list
            } else {
                CountriesData.list.filter {
                    it.nameEn.contains(query, ignoreCase = true) ||
                    it.nameAr.contains(query, ignoreCase = true) ||
                    it.code.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CountriesData.list
        )

    // Room Database states
    val allHistory: StateFlow<List<MockLocationEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favorites: StateFlow<List<MockLocationEntity>> = repository.favorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Binding to Foreground Service active state
    val isMockingRunning: StateFlow<Boolean> = MockLocationService.isRunning
    val currentSpoofedCountry: StateFlow<String?> = MockLocationService.spoofedCountry
    val currentSpoofedLat: StateFlow<Double?> = MockLocationService.spoofedLatitude
    val currentSpoofedLng: StateFlow<Double?> = MockLocationService.spoofedLongitude
    val permissionException: StateFlow<Boolean> = MockLocationService.securityExceptionOccurred

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCustomLatitudeChange(lat: String) {
        _customLatitude.value = lat
        _selectedCountry.value = null // reset selection if custom text modified
    }

    fun onCustomLongitudeChange(lng: String) {
        _customLongitude.value = lng
        _selectedCountry.value = null // reset selection if custom text modified
    }

    fun selectCountry(country: CountryInfo) {
        _selectedCountry.value = country
        _customLatitude.value = String.format(java.util.Locale.US, "%.6f", country.latitude)
        _customLongitude.value = String.format(java.util.Locale.US, "%.6f", country.longitude)
    }

    fun startSpoofing() {
        val lat = parseDoubleSafe(_customLatitude.value)
        val lng = parseDoubleSafe(_customLongitude.value)
        val countryName = _selectedCountry.value?.let {
            "${it.nameAr} | ${it.nameEn}"
        } ?: "موقع مخصص | Custom Location"

        if (lat == null || lng == null) {
            // Coordinate parsing error state could be checked in UI
            return
        }

        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_START
            putExtra(MockLocationService.EXTRA_LATITUDE, lat)
            putExtra(MockLocationService.EXTRA_LONGITUDE, lng)
            putExtra(MockLocationService.EXTRA_COUNTRY, countryName)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        // Save to database history
        viewModelScope.launch {
            repository.insertLocation(
                MockLocationEntity(
                    name = countryName,
                    latitude = lat,
                    longitude = lng,
                    isFavorite = false
                )
            )
        }
    }

    fun startSpoofingEntity(entity: MockLocationEntity) {
        _customLatitude.value = String.format(java.util.Locale.US, "%.6f", entity.latitude)
        _customLongitude.value = String.format(java.util.Locale.US, "%.6f", entity.longitude)
        
        val parsedCountry = CountriesData.list.firstOrNull {
            it.latitude == entity.latitude && it.longitude == entity.longitude
        }
        _selectedCountry.value = parsedCountry

        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_START
            putExtra(MockLocationService.EXTRA_LATITUDE, entity.latitude)
            putExtra(MockLocationService.EXTRA_LONGITUDE, entity.longitude)
            putExtra(MockLocationService.EXTRA_COUNTRY, entity.name)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        // Update timestamp/history
        viewModelScope.launch {
            repository.insertLocation(
                MockLocationEntity(
                    name = entity.name,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    isFavorite = entity.isFavorite
                )
            )
        }
    }

    fun stopSpoofing() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun toggleFavorite(entity: MockLocationEntity) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(entity.id, !entity.isFavorite)
        }
    }

    fun deleteHistoryItem(entity: MockLocationEntity) {
        viewModelScope.launch {
            repository.deleteLocationById(entity.id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
