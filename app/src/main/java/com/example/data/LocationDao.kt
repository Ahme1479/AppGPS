package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM mock_locations ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<MockLocationEntity>>

    @Query("SELECT * FROM mock_locations WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<MockLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: MockLocationEntity): Long

    @Query("DELETE FROM mock_locations WHERE id = :id")
    suspend fun deleteLocationById(id: Int)

    @Query("UPDATE mock_locations SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Query("DELETE FROM mock_locations WHERE isFavorite = 0")
    suspend fun clearNonFavoriteHistory()
}
