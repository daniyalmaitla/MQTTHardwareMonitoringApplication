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

    fun saveDevice(id: Int, deviceName: String, deviceId: String, interval: Long, enabled: Boolean) {
        onEvent(DeviceEvent.AddDevice(id, deviceId, deviceName, interval, enabled))
    }
}

