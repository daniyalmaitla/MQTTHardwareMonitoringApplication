package com.app.mqtthardwareapp.Data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device)

    @Update
    suspend fun updateDevice(device: Device)

    @Delete
    suspend fun deleteDevice(device: Device)

    @Query("SELECT * FROM devices ORDER BY id ASC")
    fun getAllDevices(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE enabled = 1 ORDER BY id ASC")
    fun getEnabledDevices(): Flow<List<Device>>

    @Query("UPDATE devices SET enabled = :enabled WHERE id = :id")
    suspend fun updateDeviceStatus(id: Int, enabled: Boolean)
}
