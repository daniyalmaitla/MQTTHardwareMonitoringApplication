package com.app.mqtthardwareapp.ViewModels

import com.app.mqtthardwareapp.Data.Device
import com.app.mqtthardwareapp.Data.DeviceDao
import kotlinx.coroutines.flow.Flow

class DeviceRepository(private val dao: DeviceDao) {
    suspend fun addDevice(device: Device) = dao.insertDevice(device)
    suspend fun deleteDevice(device: Device) = dao.deleteDevice(device)
    suspend fun enableDevice(slot: Int) = dao.updateDeviceStatus(slot, true)
    suspend fun disableDevice(slot: Int) = dao.updateDeviceStatus(slot, false)
    fun getDevices(): Flow<List<Device>> = dao.getAllDevices()
}
