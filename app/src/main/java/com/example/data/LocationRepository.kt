package com.example.data

import kotlinx.coroutines.flow.Flow

class LocationRepository(private val locationDao: LocationDao) {
    val allHistory: Flow<List<MockLocationEntity>> = locationDao.getAllHistory()
    val favorites: Flow<List<MockLocationEntity>> = locationDao.getFavorites()

    suspend fun insertLocation(location: MockLocationEntity): Long {
        return locationDao.insertLocation(location)
    }

    suspend fun deleteLocationById(id: Int) {
        locationDao.deleteLocationById(id)
    }

    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean) {
        locationDao.updateFavoriteStatus(id, isFavorite)
    }

    suspend fun clearHistory() {
        locationDao.clearNonFavoriteHistory()
    }
}
