package com.app.mqtthardwareapp.ViewModels

import com.app.mqtthardwareapp.Data.Device
import com.app.mqtthardwareapp.Data.DeviceDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class DeviceRepository(private val dao: DeviceDao) {
    suspend fun addDevice(device: Device) = dao.insertDevice(device)
    suspend fun deleteDevice(device: Device) = dao.deleteDevice(device)
    suspend fun enableDevice(slot: Int) = dao.updateDeviceStatus(slot, true)
    suspend fun disableDevice(slot: Int) = dao.updateDeviceStatus(slot, false)
    fun getDevices(): Flow<List<Device>> = dao.getAllDevices()
    suspend fun getGlobalInterval(): Long? = dao.getGlobalInterval()
    suspend fun getDeviceByDeviceId(deviceId: String): Device? {
        return dao.getDeviceByDeviceId(deviceId)
    }
    fun getEnabledDevices(): Flow<List<Device>> = dao.getEnabledDevices()
    suspend fun setIntervalForAll(interval: Long) = dao.setIntervalForAll(interval)
    suspend fun getAllDevicesOnce(): List<Device> {
        return getDevices().first()
    }
}
