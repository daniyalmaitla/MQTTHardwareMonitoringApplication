package com.app.mqtthardwareapp.Events

sealed class DeviceEvent {
    data class AddDevice(
        val id: Int,
        val slot:Int,
        val deviceId: String,
        val deviceName: String,
        val interval: Long,
        val enabled: Boolean
    ) : DeviceEvent()

    data class DeleteDevice(val id: Int) : DeviceEvent()
    data class EnableDevice(val id: Int) : DeviceEvent()
    data class DisableDevice(val id: Int) : DeviceEvent()
    object LoadDevices : DeviceEvent()
}