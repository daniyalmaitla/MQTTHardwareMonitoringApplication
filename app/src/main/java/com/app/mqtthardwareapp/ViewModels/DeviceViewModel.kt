package com.app.mqtthardwareapp.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.mqtthardwareapp.Data.Device
import com.app.mqtthardwareapp.Events.DeviceEvent
import com.app.mqtthardwareapp.States.DeviceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceViewModel(private val repo: DeviceRepository) : ViewModel() {
    private val _state = MutableStateFlow(DeviceState())
    val state: StateFlow<DeviceState> = _state.asStateFlow()

    init {
        onEvent(DeviceEvent.LoadDevices)
    }

    fun onEvent(event: DeviceEvent) {
        when (event) {
            is DeviceEvent.AddDevice -> {
                viewModelScope.launch {
                    val device = Device(
                        id = event.id,
                        slot = event.slot,
                        deviceId = event.deviceId,
                        name = event.deviceName,
                        interval = event.interval,
                        enabled = event.enabled,
                        subscribedReadTopic = "${event.deviceId}READ",
                        subscribedWriteTopic = "${event.deviceId}WRITE"
                    )
                    repo.addDevice(device)
                }
            }

            is DeviceEvent.DeleteDevice -> {
                viewModelScope.launch {
                    val device = _state.value.devices.find { it.id == event.id }
                    device?.let { repo.deleteDevice(it) }
                }
            }

            is DeviceEvent.EnableDevice -> {
                viewModelScope.launch { repo.enableDevice(event.id) }
            }

            is DeviceEvent.DisableDevice -> {
                viewModelScope.launch { repo.disableDevice(event.id) }
            }

            is DeviceEvent.LoadDevices -> {
                viewModelScope.launch {
                    repo.getDevices().collect { devices ->
                        _state.value = _state.value.copy(devices = devices)
                    }
                }
            }
        }
    }

    fun saveDevice(id: Int,slot:Int ,deviceName: String, deviceId: String, interval: Long, enabled: Boolean) {
        onEvent(DeviceEvent.AddDevice(id, slot, deviceId, deviceName, interval, enabled))
    }
    fun addDeviceFromQr(scannedValue: String) {
        viewModelScope.launch {
            val currentInterval = repo.getGlobalInterval() ?: 0L
            val nextSlot = getNextAvailableSlot()  // <-- SLOT, not id

            val device = Device(
                id = 0, // let Room auto-generate
                slot = nextSlot, // use calculated slot
                deviceId = scannedValue,
                name = "Scanned-${randomName()}",
                interval = currentInterval,
                enabled = false, // user decides later
                subscribedReadTopic = "${scannedValue}READ",
                subscribedWriteTopic = "${scannedValue}WRITE"
            )

            repo.addDevice(device)
        }
    }

    private fun getNextAvailableSlot(): Int {
        val currentSlots = _state.value.devices.map { it.slot }
        for (i in 1..20) {
            if (!currentSlots.contains(i)) return i
        }
        return 20 // fallback if all are filled
    }
    fun randomName(): String {
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..4).map { chars.random() }.joinToString("")
    }
    fun registerDevice(
        device: Device,
        onDuplicateFound: () -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val existing = repo.getDeviceByDeviceId(device.deviceId)

            if (existing != null) {
                // Duplicate found → show dialog
                onDuplicateFound()
                return@launch
            }

            repo.addDevice(device)
            onSuccess()
        }
    }
    fun isDeviceExists(deviceId: String): Boolean {
        return state.value.devices.any { it.deviceId == deviceId }
    }


}

